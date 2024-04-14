package tw.idv.woofdog.easyhealthrecord.dialogs

import java.io.File

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.adapters.DbListAdapter
import tw.idv.woofdog.easyhealthrecord.adapters.DbListAdapter.DbListItem
import tw.idv.woofdog.easyhealthrecord.db.DbTableSQLite

/**
 * Show the database file name and description editing dialog.
 */
class DbFileDialog(
    private val activity: Activity,
    private val adapter: DbListAdapter,
    private val progressBar: ProgressBar,
    private val dbDir: String,
    private val type: Type,
    private val item: DbListItem? = null
) {
    enum class Type {
        CREATE, DELETE, RENAME, DESCRIPT
    }

    private val dialog: AlertDialog
    private val view: View

    init {
        val title = when (type) {
            Type.CREATE -> R.string.dDbTitleCreate
            Type.DELETE -> R.string.dDbTitleDelete
            Type.DESCRIPT -> R.string.dDbTitleDescript
            Type.RENAME -> R.string.dDbTitleRename
        }

        val inflater =
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.dialog_db_file, null)
        dialog = AlertDialog.Builder(activity).setTitle(title).setView(view)
            .setPositiveButton(R.string.bOk, null)
            .setNeutralButton(R.string.bCancel) { dialog, _ -> dialog.dismiss() }.show()

        setupViewComponent()
    }

    private fun setupViewComponent() {
        fileEditText = view.findViewById(R.id.filenameEditText)
        descEditText = view.findViewById(R.id.descriptEditText)
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

        val editTextListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        }

        when (type) {
            Type.CREATE -> {
                view.findViewById<TextView>(R.id.descriptTextView).visibility = View.GONE
                descEditText.visibility = View.GONE
            }

            Type.DELETE -> {
                fileEditText.isEnabled = false
                descEditText.isEnabled = false
            }

            Type.DESCRIPT -> {
                fileEditText.isEnabled = false
            }

            Type.RENAME -> {
                descEditText.isEnabled = false
            }
        }
        fileEditText.setText(item?.fileName)
        fileEditText.addTextChangedListener(editTextListener)
        descEditText.setText(item?.description)
        descEditText.addTextChangedListener(editTextListener)

        okButton.setOnClickListener {
            val dbPath = "$dbDir/${fileEditText.text.removeSuffix(".db").trim()}.db"
            val result = when (type) {
                Type.CREATE -> doCreate(dbPath)
                Type.DELETE -> doDelete(dbPath)
                Type.DESCRIPT -> doDescript(dbPath, descEditText.text.toString())
                Type.RENAME -> {
                    if (item != null) doRename("$dbDir/${item.fileName}", dbPath) else false
                }
            }
            if (result) {
                progressBar.visibility = View.VISIBLE
                adapter.update()
                progressBar.visibility = View.GONE
                dialog.dismiss()
            }
        }

        validateInput()
    }

    private fun validateInput() {
        var valid = true
        fileEditText.error = null

        if (type == Type.CREATE || type == Type.RENAME) {
            val text = fileEditText.text.toString().removeSuffix(".db").trim()
            if (text.isEmpty()) {
                fileEditText.error = activity.getString(R.string.dErrEmpty)
                valid = false
            } else if (!validateFileName(text)) {
                fileEditText.error = activity.getString(R.string.dDbErrFileName)
                valid = false
            } else {
                if (File("$dbDir/$text.db").exists()) {
                    fileEditText.error = activity.getString(R.string.dDbErrFileExists)
                    valid = false
                }
            }
        }

        okButton.isEnabled = valid
    }

    private fun validateFileName(fileName: String): Boolean {
        return !(fileName.contains("\\") || fileName.contains("/") || fileName.contains(":") ||
                fileName.contains("*") || fileName.contains("?") || fileName.contains("\"") ||
                fileName.contains("<") || fileName.contains(">") || fileName.contains("|"))
    }

    private fun doCreate(dbFileFullPath: String): Boolean {
        val dbTable = DbTableSQLite()
        dbTable.setFileName(dbFileFullPath)
        dbTable.setFileName("")
        return true
    }

    private fun doDelete(dbFileFullPath: String): Boolean {
        File(dbFileFullPath).delete()
        File("${dbFileFullPath}-journal").delete()
        return true
    }

    private fun doDescript(dbFileFullPath: String, desc: String): Boolean {
        val db = DbTableSQLite()
        db.setFileName(dbFileFullPath)
        db.setDescription(desc)
        return true
    }

    private fun doRename(oldDbFileFullPath: String, newDbFileFullPath: String): Boolean {
        val fromFile = File(oldDbFileFullPath)
        val toFile = File(newDbFileFullPath)
        fromFile.renameTo(toFile)
        File("${oldDbFileFullPath}-journal").delete()
        return true
    }

    private fun toFileNameNoExt(name: String): String {
        return name.removeSuffix(".db").trim()
    }

    private lateinit var fileEditText: EditText
    private lateinit var descEditText: EditText
    private lateinit var okButton: Button
}
