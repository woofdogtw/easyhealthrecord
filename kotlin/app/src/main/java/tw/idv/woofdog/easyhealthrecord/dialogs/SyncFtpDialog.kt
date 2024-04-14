package tw.idv.woofdog.easyhealthrecord.dialogs

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.activities.DbContentActivity
import tw.idv.woofdog.easyhealthrecord.db.DbTableBase
import tw.idv.woofdog.easyhealthrecord.db.DbTableSQLite
import tw.idv.woofdog.easyhealthrecord.network.NetFtp
import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * Do synchronization operations and display progress for FTP.
 */
class SyncFtpDialog(
    private val activity: DbContentActivity,
    private val dbTable: DbTableBase,
    private val dbDir: String
) {
    private enum class CmpResult {
        EQUAL, REMOTE_IS_OLDER, REMOTE_IS_NEWER
    }

    private enum class Progress(val v: Int) {
        LOGIN(0), CD(1), GET(2), RM(3), RMTMP(4), PUT(5), CHECK(6), RENAME(7), DONE(8)
    }

    private val dialog: AlertDialog
    private val view: View
    private var dbFileName: String
    private val localFilePath: String
    private val remoteFilePath: String

    init {
        dbFileName = dbTable.getFileName()
        dbFileName = dbFileName.substring(dbFileName.lastIndexOf("/") + 1)
        localFilePath = "$dbDir/$dbFileName"
        remoteFilePath = "$dbDir/.$dbFileName"

        val inflater =
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.dialog_sync, null)
        dialog = AlertDialog.Builder(activity).setTitle(R.string.dSyncTitle).setView(view)
            .setCancelable(false).setPositiveButton(R.string.bOk, null)
            .setNeutralButton(R.string.bCancel, null).show()

        setupViewComponent()
        syncTask()
    }

    private fun setupViewComponent() {
        syncText = view.findViewById(R.id.dSyncText)
        progressBar = view.findViewById(R.id.progressBar)
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        cancelButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL)

        syncText.text = activity.getString(R.string.dSyncStart)
        progressBar.progress = Progress.LOGIN.v
        progressBar.max = Progress.DONE.v

        okButton.isEnabled = false
        okButton.setOnClickListener {
            activity.syncUpdate()
            dialog.dismiss()
        }
        cancelButton.setOnClickListener {
            syncText.text = activity.getString(R.string.dSyncCancel)
            isCancelling = true
        }
    }

    private fun syncTask() {
        val sp = Utils.getSharedPreferences(activity)
        val options = readOptions(sp)

        GlobalScope.launch(Dispatchers.IO) {
            var dbFileName = dbTable.getFileName()
            dbFileName = dbFileName.substring(dbFileName.lastIndexOf("/") + 1)

            val client = NetFtp(options.syncType == SyncType.FTPS)

            // Login to the FTP site.
            withContext(Dispatchers.Main) { progressBar.progress = Progress.LOGIN.v }
            var result = client.login(
                options.ftpHostName,
                options.ftpPort,
                options.ftpUserName,
                options.ftpPassword
            )
            if (isCancelling) {
                syncDone(client, NetFtp.Result.OK)
                return@launch
            } else if (result != NetFtp.Result.OK) {
                syncDone(client, result)
                return@launch
            }

            // Change directory.
            withContext(Dispatchers.Main) { progressBar.progress = Progress.CD.v }
            result = client.changeDir(options.ftpFileDir)
            if (isCancelling) {
                syncDone(client, NetFtp.Result.OK)
                return@launch
            } else if (result != NetFtp.Result.OK) {
                syncDone(client, result)
                return@launch
            }

            // Download the remote database file.
            withContext(Dispatchers.Main) { progressBar.progress = Progress.GET.v }
            try {
                remoteFileStream = FileOutputStream(remoteFilePath)
            } catch (_: Exception) {
                syncDone(client, NetFtp.Result.ERR_REJECT)
                return@launch
            }
            result = client.downloadFile(remoteFileStream!!, dbFileName)
            if (isCancelling) {
                syncDone(client, NetFtp.Result.OK)
                return@launch
            } else if (result == NetFtp.Result.OK) {
                // Compare after downloaded.
                when (compare()) {
                    CmpResult.REMOTE_IS_NEWER -> {
                        // Replace the local file directly.
                        val toFile = File(localFilePath)
                        dbTable.setFileName("")
                        toFile.delete()
                        val fromFile = File(remoteFilePath)
                        fromFile.renameTo(toFile)
                        dbTable.setFileName("$dbDir/$dbFileName")
                        withContext(Dispatchers.Main) { progressBar.progress = Progress.DONE.v }
                        syncDone(client, NetFtp.Result.OK)
                        return@launch
                    }

                    CmpResult.REMOTE_IS_OLDER -> {}
                    CmpResult.EQUAL -> {
                        withContext(Dispatchers.Main) { progressBar.progress = Progress.DONE.v }
                        syncDone(client, NetFtp.Result.OK)
                        return@launch
                    }
                }
            } else if (result == NetFtp.Result.ERR_NETWORK) {
                syncDone(client, result)
                return@launch
            }

            var retryCount = 0
            while (true) {
                // Remove the remote file.
                withContext(Dispatchers.Main) { progressBar.progress = Progress.RM.v }
                result = client.removeFile(dbFileName)
                if (isCancelling) {
                    syncDone(client, NetFtp.Result.OK)
                    return@launch
                } else if (result == NetFtp.Result.ERR_NETWORK) {
                    syncDone(client, result)
                    return@launch
                }

                // Remove the remote temporary file.
                withContext(Dispatchers.Main) { progressBar.progress = Progress.RMTMP.v }
                result = client.removeFile(".$dbFileName")
                if (isCancelling) {
                    syncDone(client, NetFtp.Result.OK)
                    return@launch
                } else if (result == NetFtp.Result.ERR_NETWORK) {
                    syncDone(client, result)
                    return@launch
                }

                // Upload the database file to the remote temporary file.
                withContext(Dispatchers.Main) { progressBar.progress = Progress.PUT.v }
                try {
                    localFileStream = FileInputStream(localFilePath)
                } catch (_: Exception) {
                    syncDone(client, NetFtp.Result.ERR_REJECT)
                    return@launch
                }
                result = client.uploadFile(localFileStream!!, ".$dbFileName")
                if (isCancelling) {
                    syncDone(client, NetFtp.Result.OK)
                    return@launch
                } else if (result != NetFtp.Result.OK) {
                    syncDone(client, result)
                    return@launch
                }

                // Check remote temporary file is correctly uploaded.
                withContext(Dispatchers.Main) { progressBar.progress = Progress.CHECK.v }
                try {
                    remoteFileStream = FileOutputStream(remoteFilePath)
                } catch (_: Exception) {
                    syncDone(client, NetFtp.Result.ERR_REJECT)
                    return@launch
                }
                result = client.downloadFile(remoteFileStream!!, ".$dbFileName")
                if (isCancelling) {
                    syncDone(client, NetFtp.Result.OK)
                    return@launch
                } else if (result != NetFtp.Result.OK) {
                    syncDone(client, result)
                    return@launch
                }

                var ok = true
                var retry = false
                var local: FileInputStream? = null
                var remote: FileInputStream? = null
                try {
                    local = FileInputStream(localFilePath)
                    remote = FileInputStream(remoteFilePath)

                    var count: Int
                    val buffer = ByteArray(8192)

                    val localMD5 = MessageDigest.getInstance("MD5")
                    do {
                        count = local.read(buffer)
                        if (count > 0) {
                            localMD5.update(buffer, 0, count)
                        }
                    } while (count > 0)
                    val remoteMD5 = MessageDigest.getInstance("MD5")
                    do {
                        count = remote.read(buffer)
                        if (count > 0) {
                            remoteMD5.update(buffer, 0, count)
                        }
                    } while (count > 0)

                    if (!localMD5.digest().contentEquals(remoteMD5.digest())) {
                        // Upload again.
                        retry = true
                    }
                } catch (_: Exception) {
                    ok = false
                }

                if (local != null) {
                    try {
                        local.close()
                    } catch (_: Exception) {
                    }
                }
                if (remote != null) {
                    try {
                        remote.close()
                    } catch (_: Exception) {
                    }
                }
                if (retry) {
                    retryCount++
                    if (retryCount >= 5) {
                        syncDone(client, NetFtp.Result.ERR_REJECT)
                        return@launch
                    }
                    continue
                } else if (!ok) {
                    syncDone(client, NetFtp.Result.ERR_REJECT)
                    return@launch
                }
                break
            }

            // Rename the remote temporary file to the real file name.
            withContext(Dispatchers.Main) { progressBar.progress = Progress.RENAME.v }
            result = client.renameFile(".$dbFileName", dbFileName)
            if (result != NetFtp.Result.OK) {
                syncDone(client, result)
                return@launch
            }
            withContext(Dispatchers.Main) { progressBar.progress = Progress.DONE.v }
            syncDone(client, NetFtp.Result.OK)
        }
    }

    private fun compare(): CmpResult {
        val db = DbTableSQLite()
        val localTime = dbTable.getLastModified() ?: 0

        db.setFileName(remoteFilePath)
        val remoteTime: Long = db.getLastModified() ?: 0
        db.setFileName("")

        return if (localTime == remoteTime) {
            CmpResult.EQUAL
        } else if (localTime < remoteTime) {
            CmpResult.REMOTE_IS_NEWER
        } else {
            CmpResult.REMOTE_IS_OLDER
        }
    }

    /**
     * This must be called in the IO dispatcher scope.
     */
    private suspend fun syncDone(client: NetFtp, result: NetFtp.Result) {
        try {
            File(remoteFilePath).delete()
        } catch (_: Exception) {
        }
        client.disconnect()

        if (localFileStream != null) {
            try {
                localFileStream!!.close()
                localFileStream = null
            } catch (_: Exception) {
            }
        }
        if (remoteFileStream != null) {
            try {
                remoteFileStream!!.close()
                remoteFileStream = null
            } catch (_: Exception) {
            }
        }
        try {
            var dbFileName = dbTable.getFileName()
            dbFileName = dbFileName.substring(dbFileName.lastIndexOf("/") + 1)
            File("$dbDir/.$dbFileName").deleteOnExit()
        } catch (_: Exception) {
        }

        withContext(Dispatchers.Main) {
            val progress = progressBar.progress
            if (result == NetFtp.Result.ERR_NETWORK) {
                syncText.text = activity.getString(R.string.dSyncErrConn)
            } else if (result != NetFtp.Result.OK) {
                syncText.text = when (progress) {
                    Progress.LOGIN.v -> activity.getString(R.string.dSyncErrLogin)
                    Progress.CD.v -> activity.getString(R.string.dSyncErrDir)
                    Progress.GET.v -> activity.getString(R.string.dSyncErrGet)
                    Progress.PUT.v -> activity.getString(R.string.dSyncErrPut)
                    Progress.CHECK.v -> activity.getString(R.string.dSyncErrGet)
                    Progress.RENAME.v -> activity.getString(R.string.dSyncErrRen)
                    else -> activity.getString(R.string.dSyncErrError)
                }
            } else if (progress == Progress.DONE.v) {
                syncText.text = activity.getString(R.string.dSyncDone)
            }

            okButton.isEnabled = true
            cancelButton.isEnabled = false
        }
    }

    private var isCancelling = false

    private var localFileStream: FileInputStream? = null
    private var remoteFileStream: FileOutputStream? = null

    private lateinit var syncText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var okButton: Button
    private lateinit var cancelButton: Button
}
