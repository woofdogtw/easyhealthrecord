package tw.idv.woofdog.easyhealthrecord.adapters

import java.util.Locale
import java.util.Vector

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.db.DbBodyWeight
import tw.idv.woofdog.easyhealthrecord.db.DbTableBase
import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * The adapter that contains display body weight information.
 */
class RecBodyWeightAdapter(private val parentContext: Activity, private val dbTable: DbTableBase) :
    BaseAdapter() {

    private data class RecItemHolder(
        val dateView: TextView,
        val commentView: TextView,
        val weightView: TextView,
        val fatView: TextView,
        val intFatView: TextView,
        val bmiView: TextView,
        val wcView: TextView,
        val boneView: TextView,
        val muscleView: TextView,
        val waterView: TextView,
        val metabolicView: TextView,
        val ageView: TextView
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
                parentContext.layoutInflater.inflate(R.layout.item_body_weight_list, parent, false)
            holder = RecItemHolder(
                view.findViewById(R.id.dateTextView),
                view.findViewById(R.id.commentTextView),
                view.findViewById(R.id.weightTextView),
                view.findViewById(R.id.fatTextView),
                view.findViewById(R.id.intFatTextView),
                view.findViewById(R.id.bmiTextView),
                view.findViewById(R.id.wcTextView),
                view.findViewById(R.id.boneTextView),
                view.findViewById(R.id.muscleTextView),
                view.findViewById(R.id.waterTextView),
                view.findViewById(R.id.metabolicTextView),
                view.findViewById(R.id.ageTextView)
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
        holder.weightView.text = Utils.format(item.mWeight)
        holder.fatView.text = Utils.format(item.mFat)
        holder.intFatView.text = Utils.format(item.mIntFat)
        holder.bmiView.text = Utils.format(item.mBmi)
        holder.wcView.text = Utils.format(item.mWc)
        holder.boneView.text = Utils.format(item.mBone)
        holder.muscleView.text = Utils.format(item.mMuscle)
        holder.waterView.text = Utils.format(item.mWater)
        holder.metabolicView.text = Utils.format(item.mMetabolic)
        holder.ageView.text = Utils.format(item.mAge)

        return view
    }

    fun update() {
        recListItems = dbTable.getBodyWeights() ?: return
        notifyDataSetChanged()
    }

    fun getDbTable(): DbTableBase {
        return dbTable
    }

    fun getRecListItems(): Vector<DbBodyWeight> {
        return recListItems
    }

    private var recListItems: Vector<DbBodyWeight> = Vector<DbBodyWeight>()
}
