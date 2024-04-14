package tw.idv.woofdog.easyhealthrecord.adapters

import java.util.Locale
import java.util.Vector

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.db.DbBloodPressure
import tw.idv.woofdog.easyhealthrecord.db.DbTableBase
import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * The adapter that contains display blood pressure information.
 */
class RecBloodPressureAdapter(private val parentContext: Activity, private val dbTable: DbTableBase) :
    BaseAdapter() {

    private data class RecItemHolder(
        val dateView: TextView,
        val commentView: TextView,
        val systolicView: TextView,
        val diastolicView: TextView,
        val pulseView: TextView
    )

    override fun getCount(): Int {
        return recListItems.size
    }

    override fun getItem(position: Int): Any {
        return recListItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: RecItemHolder

        val view: View
        if (convertView == null) {
            view =
                parentContext.layoutInflater.inflate(R.layout.item_blood_pressure, parent, false)
            holder = RecItemHolder(
                view.findViewById(R.id.dateTextView),
                view.findViewById(R.id.commentTextView),
                view.findViewById(R.id.systolicTextView),
                view.findViewById(R.id.diastolicTextView),
                view.findViewById(R.id.pulseTextView)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as RecItemHolder
        }

        val item = recListItems[position]
        val date = item.mDate
        val dateStr = String.format(
            Locale.getDefault(), "%d/%d/%d %d:%02d",
            DbTableBase.getYearFromDate(date),
            DbTableBase.getMonthFromDate(date),
            DbTableBase.getDayFromDate(date),
            DbTableBase.getHourFromDate(date),
            DbTableBase.getMinuteFromDate(date)
        )
        holder.dateView.text = dateStr
        holder.commentView.text = if (item.mComment.isEmpty()) "" else "[${item.mComment}]"
        holder.systolicView.text = Utils.format(item.mSystolic)
        holder.diastolicView.text = Utils.format(item.mDiastolic)
        holder.pulseView.text = Utils.format(item.mPulse)

        return view
    }

    fun update() {
        recListItems = dbTable.getBloodPressures() ?: return
        notifyDataSetChanged()
    }

    fun getDbTable(): DbTableBase {
        return dbTable
    }

    fun getRecListItems(): Vector<DbBloodPressure> {
        return recListItems
    }

    private var recListItems: Vector<DbBloodPressure> = Vector<DbBloodPressure>()
}
