package tw.idv.woofdog.easyhealthrecord.activities

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.core.view.GravityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.adapters.DbListAdapter
import tw.idv.woofdog.easyhealthrecord.dialogs.AboutDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.DbFileDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.OptionsSyncDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.WarningDialog
import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * The main activity.
 */
class MainActivity : AppCompatActivity() {
    private enum class MenuItemState {
        MAIN, // Normal state
        SELECT_NONE, // Select nothing
        SELECT_MULTI // Select one or more files
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        dbDir = "${applicationInfo.dataDir}/databases"
        val dir = File(dbDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dbListAdapter = DbListAdapter(this, dbDir)
        drawerLayout = findViewById(R.id.drawerLayout)
        progressBar = findViewById(R.id.progressBar)

        initActivityResults()

        setupViewComponent()

        checkUpgrade(this)?.let {
            progressBar.visibility = View.INVISIBLE
            WarningDialog(
                this,
                R.string.dAppUpgradeTitle,
                getString(R.string.dAppUpgradeImportSelf)
            ) { dialog, _ ->
                dialog.dismiss()
            }.setNeutralButton(R.string.bNoMoreHint) { dialog, _ ->
                writeCurrentVersion(this)
                dialog.dismiss()
            }.show()
        }

        progressBar.visibility = View.VISIBLE
        dbListAdapter.update()
        progressBar.visibility = View.INVISIBLE
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val dbItem =
            dbListAdapter.getDbListItems()[menuInfo.position] ?: return super.onContextItemSelected(
                item
            )
        when (item.itemId) {
            R.id.dbFileDeleteButton -> DbFileDialog(
                this,
                dbListAdapter,
                progressBar,
                dbDir,
                DbFileDialog.Type.DELETE,
                dbItem
            )

            R.id.dbFileRenameButton -> DbFileDialog(
                this,
                dbListAdapter,
                progressBar,
                dbDir,
                DbFileDialog.Type.RENAME,
                dbItem
            )

            R.id.dbFileDescButton -> DbFileDialog(
                this,
                dbListAdapter,
                progressBar,
                dbDir,
                DbFileDialog.Type.DESCRIPT,
                dbItem
            )

            R.id.dbFileSaveButton -> {
                saveFiles = listOf(dbItem.fileName)
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
                }
                saveLauncher.launch(intent)
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun setupViewComponent() {
        initListView()
        initTopAppBar()
        initBottomAppBar()
        initLeftDrawer()
        onCloseClick()
    }

    private fun initActivityResults() {
        openLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val data = result.data
                val selectedUris = data?.clipData?.let { clipData ->
                    (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
                } ?: listOfNotNull(data?.data)
                restoreFiles(this, selectedUris, false)
            }

        saveLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val uri = result.data?.data ?: return@registerForActivityResult
                backupFiles(this, saveFiles, listDirFiles(this, uri), false)
            }

        dbContentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val dbFileSync = result.data?.extras?.getBoolean("dbFileSync")
                if (dbFileSync == true) {
                    progressBar.visibility = View.VISIBLE
                    dbListAdapter.update()
                    progressBar.visibility = View.INVISIBLE
                }
            }
    }

    private fun initListView() {
        val listView = findViewById<ListView>(R.id.dbFileListView)
        listView.adapter = dbListAdapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = dbListAdapter.getDbListItems()[position] ?: return@setOnItemClickListener
            if (dbListAdapter.showCheckBox) {
                item.checked = !item.checked
                dbListAdapter.refresh()

                val deleteButton = findViewById<ActionMenuItemView>(R.id.dbFileDeleteButton)
                val saveButton = findViewById<ActionMenuItemView>(R.id.dbFileSaveButton)
                menuItemState = MenuItemState.SELECT_NONE
                for (dbItem in dbListAdapter.getDbListItems()) {
                    if (dbItem.checked) {
                        menuItemState = MenuItemState.SELECT_MULTI
                        deleteButton.visibility = View.VISIBLE
                        saveButton.visibility = View.VISIBLE
                        return@setOnItemClickListener
                    }
                }
                deleteButton.visibility = View.GONE
                saveButton.visibility = View.GONE
            } else {
                val intent = Intent(this, DbContentActivity::class.java)
                intent.putExtra("dbName", item.fileName)
                dbContentLauncher.launch(intent)
            }
        }
    }

    private fun initTopAppBar() {
        val appBar: MaterialToolbar = findViewById(R.id.topAppBar)
        appBar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun initBottomAppBar() {
        val appBar: MaterialToolbar = findViewById(R.id.bottomAppBar)
        appBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.dbFileSelectButton -> {
                    refreshActionMenuItem()
                    if (menuItemState != MenuItemState.MAIN) {
                        return@setOnMenuItemClickListener true
                    }
                    onSelectClick()
                    true
                }

                R.id.dbFileAddButton -> {
                    refreshActionMenuItem()
                    if (menuItemState != MenuItemState.MAIN) {
                        return@setOnMenuItemClickListener true
                    }
                    DbFileDialog(
                        this,
                        dbListAdapter,
                        progressBar,
                        dbDir,
                        DbFileDialog.Type.CREATE
                    )
                    true
                }

                R.id.dbFileRestoreButton -> {
                    refreshActionMenuItem()
                    if (menuItemState != MenuItemState.MAIN) {
                        return@setOnMenuItemClickListener true
                    }
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                        Utils.getUriForOpenDocumentTree(this@MainActivity, Utils.LEGACY_APP_DIR)
                            ?.let {
                                putExtra(DocumentsContract.EXTRA_INITIAL_URI, "$it")
                            }
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    openLauncher.launch(intent)
                    true
                }

                R.id.dbFileDeleteButton -> {
                    refreshActionMenuItem()
                    if (menuItemState != MenuItemState.SELECT_MULTI) {
                        return@setOnMenuItemClickListener true
                    }
                    if (!dbListAdapter.showCheckBox) {
                        onCloseClick()
                        return@setOnMenuItemClickListener true
                    }
                    val fileList = mutableListOf<DbListAdapter.DbListItem>()
                    for (dbItem in dbListAdapter.getDbListItems()) {
                        if (dbItem.checked) {
                            fileList.add(dbItem)
                        }
                    }
                    if (fileList.isEmpty()) {
                        return@setOnMenuItemClickListener true
                    } else if (fileList.size == 1) {
                        DbFileDialog(
                            this,
                            dbListAdapter,
                            progressBar,
                            dbDir,
                            DbFileDialog.Type.DELETE,
                            fileList[0]
                        )
                        onCloseClick()
                        return@setOnMenuItemClickListener true
                    }

                    var msg = "${getString(R.string.dDbDelete)}\n"
                    for (file in fileList) {
                        msg += "\n${file.fileName} - ${file.description}"
                    }
                    val context = this@MainActivity
                    WarningDialog(context, 0, msg) { dialog, _ ->
                        for (file in fileList) {
                            File("$dbDir/${file.fileName}").delete()
                            File("$dbDir/${file.fileName}-journal").delete()
                        }
                        progressBar.visibility = View.VISIBLE
                        dbListAdapter.update()
                        progressBar.visibility = View.INVISIBLE
                        onCloseClick()
                        dialog.dismiss()
                    }.show()
                    true
                }

                R.id.dbFileSaveButton -> {
                    refreshActionMenuItem()
                    if (menuItemState != MenuItemState.SELECT_MULTI) {
                        return@setOnMenuItemClickListener true
                    }
                    if (!dbListAdapter.showCheckBox) {
                        onCloseClick()
                        return@setOnMenuItemClickListener true
                    }
                    val fileList = mutableListOf<String>()
                    for (dbItem in dbListAdapter.getDbListItems()) {
                        if (dbItem.checked) {
                            fileList.add(dbItem.fileName)
                        }
                    }
                    if (fileList.isEmpty()) {
                        return@setOnMenuItemClickListener true
                    }
                    saveFiles = fileList.toList()
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
                    }
                    saveLauncher.launch(intent)
                    true
                }

                R.id.dbFileCloseButton -> {
                    refreshActionMenuItem()
                    if (menuItemState == MenuItemState.MAIN) {
                        return@setOnMenuItemClickListener true
                    }
                    onCloseClick()
                    true
                }

                else -> false
            }
        }
        refreshActionMenuItem()
    }

    private fun initLeftDrawer() {
        val navigationView: NavigationView = findViewById(R.id.leftDrawer)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.mOptSync -> OptionsSyncDialog(this)
                R.id.mAbout -> AboutDialog(this)
            }
            true
        }
    }

    private fun onSelectClick() {
        dbListAdapter.showCheckBox = true
        menuItemState = MenuItemState.SELECT_NONE

        val listView = findViewById<ListView>(R.id.dbFileListView)
        listView.setOnCreateContextMenuListener(null)

        refreshActionMenuItem()
    }

    private fun onCloseClick() {
        dbListAdapter.showCheckBox = false
        menuItemState = MenuItemState.MAIN

        val listView = findViewById<ListView>(R.id.dbFileListView)
        listView.setOnCreateContextMenuListener { menu, _, _ ->
            menuInflater.inflate(R.menu.db_file_context_menu, menu)
        }

        refreshActionMenuItem()
    }

    @SuppressLint("RestrictedApi")
    private fun refreshActionMenuItem() {
        var item: ActionMenuItemView
        when (menuItemState) {
            MenuItemState.MAIN -> {
                item = findViewById(R.id.dbFileSelectButton)
                item.visibility = View.VISIBLE
                item = findViewById(R.id.dbFileAddButton)
                item.visibility = View.VISIBLE
                item = findViewById(R.id.dbFileRestoreButton)
                item.visibility = View.VISIBLE
                item = findViewById(R.id.dbFileSaveButton)
                item.visibility = View.GONE
                item = findViewById(R.id.dbFileDeleteButton)
                item.visibility = View.GONE
                item = findViewById(R.id.dbFileCloseButton)
                item.visibility = View.GONE
            }

            MenuItemState.SELECT_NONE -> {
                item = findViewById(R.id.dbFileSelectButton)
                item.visibility = View.GONE
                item = findViewById(R.id.dbFileAddButton)
                item.visibility = View.GONE
                item = findViewById(R.id.dbFileRestoreButton)
                item.visibility = View.GONE
                item = findViewById(R.id.dbFileSaveButton)
                item.visibility = View.GONE
                item = findViewById(R.id.dbFileDeleteButton)
                item.visibility = View.GONE
                item = findViewById(R.id.dbFileCloseButton)
                item.visibility = View.VISIBLE
            }

            MenuItemState.SELECT_MULTI -> {
                item = findViewById(R.id.dbFileSelectButton)
                item.visibility = View.GONE
                item = findViewById(R.id.dbFileAddButton)
                item.visibility = View.GONE
                item = findViewById(R.id.dbFileRestoreButton)
                item.visibility = View.GONE
                item = findViewById(R.id.dbFileSaveButton)
                item.visibility = View.VISIBLE
                item = findViewById(R.id.dbFileDeleteButton)
                item.visibility = View.VISIBLE
                item = findViewById(R.id.dbFileCloseButton)
                item.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Backup the selected files in the target directory that is selected by the OS file explorer.
     *
     * This function will prompt a dialog when there are exist files.
     *
     * @param selectedFiles The (relative) file names to be saved in the target directory.
     * @param targetDirFiles The exist files Uris in the target directory.
     */
    private fun backupFiles(
        context: Context,
        selectedFiles: List<String>,
        targetDirFiles: List<DirFileParams>,
        refresh: Boolean
    ) {
        if (selectedFiles.isEmpty()) {
            if (refresh) {
                progressBar.visibility = View.VISIBLE
                dbListAdapter.update()
                progressBar.visibility = View.INVISIBLE
            }
            return
        }
        val name = selectedFiles[0]
        var existFile: DirFileParams? = null
        for (f in targetDirFiles) {
            if (f.name == name) {
                existFile = f
                break
            }
        }
        if (existFile == null) {
            val dirUri = targetDirFiles[0].uri
            val uri = DocumentFile.fromTreeUri(context, dirUri)
                ?.createFile("application/octet-stream", name)?.uri
            if (uri != null) {
                copySaveFile(context, "$dbDir/$name", uri)
            }
            backupFiles(
                context,
                selectedFiles.subList(1, selectedFiles.size),
                targetDirFiles,
                true
            )
            return
        }

        val msg = "$name ${getString(R.string.dDbBackupExists)}"
        WarningDialog(context, 0, msg, { dialog, _ ->
            dialog.dismiss()
            copySaveFile(context, "$dbDir/$name", existFile.uri)
            backupFiles(
                context,
                selectedFiles.subList(1, selectedFiles.size),
                targetDirFiles,
                true
            )
        }, { dialog, _ ->
            dialog.dismiss()
            backupFiles(
                context,
                selectedFiles.subList(1, selectedFiles.size),
                targetDirFiles,
                refresh
            )
        }, { dialog, _ ->
            dialog.dismiss()
            backupFiles(context, emptyList(), targetDirFiles, refresh)
        }).show()
    }

    /**
     * To restore files selected from the OS file explorer in the App's storage.
     *
     * This function will prompt a dialog when there are exist files.
     *
     * @param fileUris The Uri list from the OS file explorer.
     * @param refresh To refresh the list adapter after restore files complete.
     */
    private fun restoreFiles(context: Context, fileUris: List<Uri>, refresh: Boolean) {
        if (fileUris.isEmpty()) {
            if (refresh) {
                progressBar.visibility = View.VISIBLE
                dbListAdapter.update()
                progressBar.visibility = View.INVISIBLE
            }
            return
        }
        val uri = fileUris[0]
        if (uri.path == null) {
            restoreFiles(context, fileUris.subList(1, fileUris.size), refresh)
            return
        }
        val path = uri.path!!
        val name = path.substring(path.lastIndexOf('/') + 1)
        val outFile = File("$dbDir/$name")
        if (!outFile.exists()) {
            copyOpenFile(context, uri, outFile)
            restoreFiles(context, fileUris.subList(1, fileUris.size), true)
            return
        }

        val msg = "$name ${getString(R.string.dDbBackupExists)}"
        WarningDialog(context, 0, msg, { dialog, _ ->
            dialog.dismiss()
            copyOpenFile(context, uri, outFile)
            restoreFiles(context, fileUris.subList(1, fileUris.size), true)
        }, { dialog, _ ->
            dialog.dismiss()
            restoreFiles(context, fileUris.subList(1, fileUris.size), refresh)
        }, { dialog, _ ->
            dialog.dismiss()
            restoreFiles(context, emptyList(), refresh)
        }).show()
    }

    private lateinit var dbListAdapter: DbListAdapter
    private lateinit var dbDir: String
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var openLauncher: ActivityResultLauncher<Intent>
    private lateinit var saveLauncher: ActivityResultLauncher<Intent>
    private lateinit var dbContentLauncher: ActivityResultLauncher<Intent>
    private var saveFiles = emptyList<String>()
    private var menuItemState = MenuItemState.MAIN
}

private data class DirFileParams(
    val uri: Uri,
    val name: String
)

/**
 * Copy the file that was selected in the OS file explorer to the App's storage.
 *
 * @param uri The file Uri from the OS file explorer.
 * @param targetFile The `File` object that will be copied to.
 */
private fun copyOpenFile(context: Context, uri: Uri, targetFile: File) {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: return
    targetFile.createNewFile()
    val outputStream = FileOutputStream(targetFile, false)
    inputStream.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
    outputStream.close()
    inputStream.close()
}

/**
 * Copy a file to the target directory selected in the OS file explorer.
 *
 * @param sourcePath The absolute path of the source file.
 * @param targetUri The file Uri in the target directory.
 */
private fun copySaveFile(
    context: Context,
    sourcePath: String,
    targetUri: Uri
) {
    val inputStream = FileInputStream(File(sourcePath))
    val outputStream =
        context.contentResolver.openOutputStream(targetUri) ?: return
    inputStream.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
    outputStream.close()
    inputStream.close()
}

/**
 * List nodes in the directory URI. The first item will be `dirUri` ifself with empty name.
 *
 * @param dirUri The directory URI to be list files.
 */
private fun listDirFiles(context: Context, dirUri: Uri): List<DirFileParams> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        dirUri,
        DocumentsContract.getTreeDocumentId(dirUri)
    )
    val cursor = context.contentResolver.query(
        childrenUri,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ),
        null,
        null,
        null
    ) ?: return emptyList()

    val dirUris = mutableListOf<DirFileParams>()
    dirUris.add(DirFileParams(dirUri, ""))
    while (cursor.moveToNext()) {
        val docId = cursor.getString(0)
        val name = cursor.getString(1)
        val nodeUri = DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, docId)
        dirUris.add(DirFileParams(nodeUri, name))
    }
    cursor.close()
    return dirUris.toList()
}

/**
 * To check if the App is upgraded from the older version. Currently we check:
 * - Upgrade from 1.x
 *     - To import database files from the `Environment.getExternalStorageDirectory()` directory.
 */
private fun checkUpgrade(context: Context): String? {
    val sp = Utils.getSharedPreferences(context)
    val curVersion = sp.getString(Utils.SP_CUR_VERSION, "")
    if (curVersion != "") {
        return null
    }
    return Utils.checkOldDbExist()
}

/**
 * Write the current version to SharedPreferences.
 */
private fun writeCurrentVersion(context: Context) {
    val sp = Utils.getSharedPreferences(context)
    sp.edit().putString(Utils.SP_CUR_VERSION, Utils.getPackageVersion(context)).apply()
}
