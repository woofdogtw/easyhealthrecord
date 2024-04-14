package tw.idv.woofdog.easyhealthrecord.adapters

import java.util.Locale
import java.util.Vector

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.db.DbBloodGlucose
import tw.idv.woofdog.easyhealthrecord.db.DbTableBase
import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * The adapter that contains display blood glucose information.
 */
class RecBloodGlucoseAdapter(private val parentContext: Activity, private val dbTable: DbTableBase) :
    BaseAdapter() {

    private data class RecItemHolder(
        val dateView: TextView,
        val commentView: TextView,
        val glucoseView: TextView,
        val mealView: TextView
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
                parentContext.layoutInflater.inflate(R.layout.item_blood_glucose, parent, false)
            holder = RecItemHolder(
                view.findViewById(R.id.dateTextView),
                view.findViewById(R.id.commentTextView),
                view.findViewById(R.id.glucoseTextView),
                view.findViewById(R.id.mealTextView)
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
        holder.glucoseView.text = Utils.format(item.mGlucose)
        holder.mealView.text = convertMeal(item.mMeal)

        return view
    }

    fun update() {
        recListItems = dbTable.getBloodGlucoses() ?: return
        notifyDataSetChanged()
    }

    fun getDbTable(): DbTableBase {
        return dbTable
    }

    fun getRecListItems(): Vector<DbBloodGlucose> {
        return recListItems
    }

    private fun convertMeal(meal: DbBloodGlucose.Meal): String {
        return when (meal) {
            DbBloodGlucose.Meal.NORMAL -> parentContext.getString(R.string.recMealNormal)
            DbBloodGlucose.Meal.BEFORE -> parentContext.getString(R.string.recMealBefore)
            DbBloodGlucose.Meal.AFTER -> parentContext.getString(R.string.recMealAfter)
        }
    }

    private var recListItems: Vector<DbBloodGlucose> = Vector<DbBloodGlucose>()
}
