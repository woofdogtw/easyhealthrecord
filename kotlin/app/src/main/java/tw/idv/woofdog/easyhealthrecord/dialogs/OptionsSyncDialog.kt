package tw.idv.woofdog.easyhealthrecord.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.utils.Utils

enum class SyncType(val v: Int) {
    NONE(0), FTP(1), GOOGLE(2), MS(3), FTPS(4), SFTP(5)
}

data class SyncOptions(
    var syncType: SyncType = SyncType.NONE,
    var ftpHostName: String = "",
    var ftpPort: Int = DEFAULT_FTP_PORT,
    var ftpUserName: String = "",
    var ftpPassword: String = "",
    var ftpFileDir: String = "",
    var googleRem: Boolean = false,
    var googleFileDir: String = "",
    var msRem: Boolean = false,
    var msFileDir: String = "",
)

private const val DEFAULT_FTP_PORT = 21

/**
 * Provides options for configuring the program.
 */
class OptionsSyncDialog(private val activity: Activity) {
    private val dialog: AlertDialog
    private val view: View

    init {
        val inflater =
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.dialog_options_sync, null)
        dialog = AlertDialog.Builder(activity).setTitle(R.string.mOptSync).setView(view)
            .setPositiveButton(R.string.bOk, null)
            .setNeutralButton(R.string.bCancel) { dialog, _ -> dialog.dismiss() }.show()

        setupViewComponent()
    }

    private fun setupViewComponent() {
        val sp = Utils.getSharedPreferences(activity)
        val options = readOptions(sp)

        val editTextListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        }

        val noneButton = view.findViewById<RadioButton>(R.id.syncUseNoneButton)
        val ftpButton = view.findViewById<RadioButton>(R.id.syncUseFtpButton)
        //val ftpsButton = view.findViewById<RadioButton>(R.id.syncUseFtpsButton)
        //val sftpButton = view.findViewById<RadioButton>(R.id.syncUseSftpButton)
        //val googleButton = view.findViewById<RadioButton>(R.id.syncUseGoogleButton)
        val msButton = view.findViewById<RadioButton>(R.id.syncUseMsButton)
        val radioButton: RadioButton = when (options.syncType) {
            SyncType.NONE -> noneButton
            SyncType.FTP -> ftpButton
            SyncType.FTPS -> noneButton
            SyncType.SFTP -> noneButton
            SyncType.FTPS -> noneButton
            SyncType.SFTP -> noneButton
            SyncType.GOOGLE -> noneButton
            SyncType.MS -> msButton
        }
        radioButton.isChecked = true
        val radioList = listOf<RadioButton>(
            noneButton,
            ftpButton,
            //ftpsButton,
            //sftpButton,
            //googleButton,
            msButton
        )
        for (btn in radioList) {
            btn.setOnCheckedChangeListener { _, _ -> validateInput() }
        }
        ftpHostEditText = view.findViewById(R.id.syncFtpHostEditText)
        ftpHostEditText.setText(options.ftpHostName)
        ftpHostEditText.addTextChangedListener(editTextListener)
        ftpPortEditText = view.findViewById(R.id.syncFtpPortEditText)
        ftpPortEditText.setText(options.ftpPort.toString())
        ftpPortEditText.addTextChangedListener(editTextListener)
        ftpUserEditText = view.findViewById(R.id.syncFtpUserEditText)
        ftpUserEditText.setText(options.ftpUserName)
        ftpUserEditText.addTextChangedListener(editTextListener)
        ftpPassEditText = view.findViewById(R.id.syncFtpPassEditText)
        ftpPassEditText.setText(options.ftpPassword)
        ftpPassEditText.addTextChangedListener(editTextListener)
        var text: EditText = view.findViewById(R.id.syncFtpDirEditText)
        text.setText(options.ftpFileDir)
        var check: CheckBox
        //check = view.findViewById(R.id.syncGoogleLoginRemCheckBox)
        //check.isChecked = options.googleRem
        //text = view.findViewById(R.id.syncGoogleDirEditText)
        //text.setText(options.googleFileDir)
        check = view.findViewById(R.id.syncMsLoginRemCheckBox)
        check.isChecked = options.msRem
        text = view.findViewById(R.id.syncMsDirEditText)
        text.setText(options.msFileDir)
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

        okButton.setOnClickListener {
            writeOptions(Utils.getSharedPreferences(activity), componentsToOptions())
            dialog.dismiss()
        }

        validateInput()
    }

    private fun validateInput() {
        val options = componentsToOptions()
        var valid = true

        ftpHostEditText.error = null
        ftpPortEditText.error = null
        ftpUserEditText.error = null
        ftpPassEditText.error = null
        if (options.syncType == SyncType.FTP ||
            options.syncType == SyncType.FTPS ||
            options.syncType == SyncType.SFTP
        ) {
            if (options.ftpHostName == "") {
                ftpHostEditText.error = activity.getString(R.string.dErrEmpty)
                valid = false
            }
            if (options.syncType == SyncType.SFTP) {
                if (options.ftpUserName == "") {
                    ftpUserEditText.error = activity.getString(R.string.dErrEmpty)
                    valid = false
                }
                if (options.ftpPassword == "") {
                    ftpPassEditText.error = activity.getString(R.string.dErrEmpty)
                    valid = false
                }
            }
        }
        if (options.ftpPort < 1 || options.ftpPort > 65535) {
            ftpPortEditText.error = activity.getString(R.string.dOptSyncErrPortRange)
            valid = false
        }

        okButton.isEnabled = valid
    }

    private fun componentsToOptions(): SyncOptions {
        val options = SyncOptions()
        if (view.findViewById<RadioButton>(R.id.syncUseFtpButton).isChecked) {
            options.syncType = SyncType.FTP
            //} else if (view.findViewById<RadioButton>(R.id.syncUseFtpsButton).isChecked) {
            //    options.syncType = SyncType.FTPS
            //} else if (view.findViewById<RadioButton>(R.id.syncUseSftpButton).isChecked) {
            //    options.syncType = SyncType.SFTP
            //} else if (view.findViewById<RadioButton>(R.id.syncUseGoogleButton).isChecked) {
            //    options.syncType = SyncType.GOOGLE
        } else if (view.findViewById<RadioButton>(R.id.syncUseMsButton).isChecked) {
            options.syncType = SyncType.MS
        } else {
            options.syncType = SyncType.NONE
        }
        options.ftpHostName =
            view.findViewById<EditText>(R.id.syncFtpHostEditText).text.toString()
        val text = view.findViewById<EditText>(R.id.syncFtpPortEditText).text.toString()
        options.ftpPort = if (text != "") {
            try {
                Integer.parseInt(view.findViewById<EditText>(R.id.syncFtpPortEditText).text.toString())
            } catch (e: Exception) {
                0
            }
        } else 0
        options.ftpUserName =
            view.findViewById<EditText>(R.id.syncFtpUserEditText).text.toString()
        options.ftpPassword =
            view.findViewById<EditText>(R.id.syncFtpPassEditText).text.toString()
        options.ftpFileDir =
            view.findViewById<EditText>(R.id.syncFtpDirEditText).text.toString()
        //options.googleRem =
        //    view.findViewById<CheckBox>(R.id.syncGoogleLoginRemCheckBox).isChecked
        //options.googleFileDir =
        //    view.findViewById<EditText>(R.id.syncGoogleDirEditText).text.toString()
        options.msRem = view.findViewById<CheckBox>(R.id.syncMsLoginRemCheckBox).isChecked
        options.msFileDir = view.findViewById<EditText>(R.id.syncMsDirEditText).text.toString()
        return options
    }

    private lateinit var ftpHostEditText: EditText
    private lateinit var ftpPortEditText: EditText
    private lateinit var ftpUserEditText: EditText
    private lateinit var ftpPassEditText: EditText
    private lateinit var okButton: Button
}

/**
 * Read options view model from SharedPreference.
 */
fun readOptions(sharedPrefs: SharedPreferences): SyncOptions {
    val options = SyncOptions()
    val syncType = syncTypeFromInt(sharedPrefs.getInt(Utils.SP_SYNC_TYPE, SyncType.NONE.v))
    options.syncType = syncType
    options.ftpHostName = sharedPrefs.getString(Utils.SP_SYNC_FTP_HOSTNAME, "") ?: ""
    options.ftpPort = sharedPrefs.getInt(Utils.SP_SYNC_FTP_PORT, DEFAULT_FTP_PORT)
    options.ftpUserName = sharedPrefs.getString(Utils.SP_SYNC_FTP_USERNAME, "") ?: ""
    options.ftpPassword = sharedPrefs.getString(Utils.SP_SYNC_FTP_PASSWORD, "") ?: ""
    if (options.ftpPassword != "") {
        options.ftpPassword = Base64.decode(options.ftpPassword, Base64.DEFAULT).decodeToString()
    }
    options.ftpFileDir = sharedPrefs.getString(Utils.SP_SYNC_FTP_FILE_DIR, "") ?: ""
    options.googleRem = sharedPrefs.getBoolean(Utils.SP_SYNC_GOOGLE_REM, true)
    options.googleFileDir = sharedPrefs.getString(Utils.SP_SYNC_GOOGLE_FILE_DIR, "") ?: ""
    options.msRem = sharedPrefs.getBoolean(Utils.SP_SYNC_MS_REM, true)
    options.msFileDir = sharedPrefs.getString(Utils.SP_SYNC_MS_FILE_DIR, "") ?: ""
    return options
}

/**
 * Write options view model into SharedPreference.
 */
private fun writeOptions(sharedPrefs: SharedPreferences, options: SyncOptions) {
    var password = options.ftpPassword
    if (password != "") {
        password = Base64.encodeToString(password.toByteArray(), Base64.DEFAULT)
    }
    val editor = sharedPrefs.edit()
        .putInt(Utils.SP_SYNC_TYPE, options.syncType.v)
        .putString(Utils.SP_SYNC_FTP_HOSTNAME, options.ftpHostName)
        .putInt(Utils.SP_SYNC_FTP_PORT, options.ftpPort)
        .putString(Utils.SP_SYNC_FTP_USERNAME, options.ftpUserName)
        .putString(Utils.SP_SYNC_FTP_PASSWORD, password)
        .putString(Utils.SP_SYNC_FTP_FILE_DIR, options.ftpFileDir)
        .putBoolean(Utils.SP_SYNC_GOOGLE_REM, options.googleRem)
        .putString(Utils.SP_SYNC_GOOGLE_FILE_DIR, options.googleFileDir)
        .putBoolean(Utils.SP_SYNC_MS_REM, options.msRem)
        .putString(Utils.SP_SYNC_MS_FILE_DIR, options.msFileDir)
    if (!options.googleRem) {
        editor.putString(Utils.SP_SYNC_GOOGLE_REFRESH_TOKEN, "")
    }
    if (!options.msRem) {
        editor.putString(Utils.SP_SYNC_MS_REFRESH_TOKEN, "")
    }
    editor.apply()
}

private fun syncTypeFromInt(v: Int): SyncType {
    return when (v) {
        SyncType.FTP.v -> SyncType.FTP
        SyncType.GOOGLE.v -> SyncType.GOOGLE
        SyncType.MS.v -> SyncType.MS
        SyncType.FTPS.v -> SyncType.FTPS
        SyncType.SFTP.v -> SyncType.SFTP
        else -> SyncType.NONE
    }
}
