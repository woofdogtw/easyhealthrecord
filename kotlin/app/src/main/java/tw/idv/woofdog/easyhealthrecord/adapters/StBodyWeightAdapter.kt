package tw.idv.woofdog.easyhealthrecord.adapters

import java.util.Vector

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * The adapter that contains body weight statistics information.
 */
class StBodyWeightAdapter(
    private val parentContext: Activity,
    private val results: Vector<StItem>
) :
    BaseAdapter() {

    data class StItem(
        val name: String,
        val min: Double,
        val newest: Double,
        val max: Double,
        val avg: Double
    )

    private data class StItemHolder(
        val nameView: TextView,
        val minView: TextView,
        val newestView: TextView,
        val maxView: TextView,
        val avgView: TextView
    )

    override fun getCount(): Int {
        return results.size
    }

    override fun getItem(position: Int): Any {
        return results[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: StItemHolder

        val view: View
        if (convertView == null) {
            view =
                parentContext.layoutInflater.inflate(R.layout.item_st_list, parent, false)
            holder = StItemHolder(
                view.findViewById(R.id.itemName),
                view.findViewById(R.id.itemMin),
                view.findViewById(R.id.itemNew),
                view.findViewById(R.id.itemMax),
                view.findViewById(R.id.itemAvg)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as StItemHolder
        }

        val item = results[position]
        holder.nameView.text = item.name
        holder.minView.text = Utils.format(item.min)
        holder.newestView.text = Utils.format(item.newest)
        holder.maxView.text = Utils.format(item.max)
        holder.avgView.text = Utils.format(item.avg)

        return view
    }
}
