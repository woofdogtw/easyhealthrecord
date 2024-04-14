package tw.idv.woofdog.easyhealthrecord.adapters

import java.io.File
import java.util.Vector

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.db.DbTableSQLite

/**
 * The adapter that contains database file names and descriptions.
 */
class DbListAdapter(private val parentContext: Activity, private val dbDir: String) :
    BaseAdapter() {
    data class DbListItem(var checked: Boolean, val fileName: String, val description: String)

    private data class DbItemHolder(
        val checkBox: CheckBox,
        val descView: TextView,
        val fileView: TextView
    )

    override fun getCount(): Int {
        return dbListItems.size
    }

    override fun getItem(position: Int): Any {
        return dbListItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: DbItemHolder

        val view: View
        if (convertView == null) {
            view = parentContext.layoutInflater.inflate(R.layout.item_db_list, parent, false)
            holder =
                DbItemHolder(
                    view.findViewById(R.id.itemCheckBox),
                    view.findViewById(R.id.itemDesc),
                    view.findViewById(R.id.itemFilename)
                )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as DbItemHolder
        }

        val item = dbListItems[position]
        var desc = item.description
        if (desc == "") {
            desc = parentContext.getString(R.string.titleUntitled)
        }
        holder.checkBox.visibility = if (showCheckBox) View.VISIBLE else View.GONE
        holder.checkBox.isChecked = item.checked
        holder.descView.text = desc
        holder.fileView.text = item.fileName

        return view
    }

    fun getDbListItems(): Vector<DbListItem> {
        return dbListItems
    }

    fun refresh() {
        notifyDataSetChanged()
    }

    fun update() {
        val dir = File(dbDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        var fileList =
            dir.listFiles { file -> file.isFile && file.extension == "db" && !file.name.startsWith(".") }
                ?: emptyArray()
        fileList = fileList.sortedBy { it.name }.toTypedArray()

        dbListItems.clear()
        val db = DbTableSQLite()
        for (file in fileList) {
            if (db.setFileName(file.absolutePath)) {
                dbListItems.add(DbListItem(false, file.name, db.getDescription() ?: ""))
                db.setFileName("")
            }
        }
        notifyDataSetChanged()
    }

    var showCheckBox: Boolean = false
        set(visible) {
            field = visible
            if (!visible) {
                for (item in dbListItems) {
                    item.checked = false
                }
            }
            notifyDataSetChanged()
        }

    private val dbListItems: Vector<DbListItem> = Vector<DbListItem>()
}
