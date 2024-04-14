package tw.idv.woofdog.easyhealthrecord.activities

import java.util.Vector

import android.os.Build
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.adapters.StBodyWeightAdapter
import tw.idv.woofdog.easyhealthrecord.db.DbBodyWeight

/**
 * The activity to display body weight statistics information.
 */
class StBodyWeightActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.titleStBodyWeight)
        setContentView(R.layout.activity_statistics)

        val recList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("recList", ArrayList::class.java) as ArrayList<DbBodyWeight>
        } else {
            intent.getSerializableExtra("recList") as ArrayList<DbBodyWeight>
        }

        setupViewComponent(recList)
    }

    private fun setupViewComponent(recList: ArrayList<DbBodyWeight>) {
        val ITEM_COUNT = 10
        val count = Array(ITEM_COUNT) { _ -> 0 }
        val total = Array(ITEM_COUNT) { _ -> 0.0 }
        val min = Array(ITEM_COUNT) { _ -> 0.0 }
        val max = Array(ITEM_COUNT) { _ -> 0.0 }
        val newest = Array(ITEM_COUNT) { _ -> 0.0 }
        val newDate = Array(ITEM_COUNT) { _ -> 0L }

        // Calculate data.
        for (rec in recList) {
            for (i in 0..<ITEM_COUNT) {
                val value = when (i) {
                    0 -> rec.mWeight
                    1 -> rec.mFat
                    2 -> rec.mIntFat
                    3 -> rec.mBmi
                    4 -> rec.mWc
                    5 -> rec.mBone
                    6 -> rec.mMuscle
                    7 -> rec.mWater
                    8 -> rec.mMetabolic.toDouble()
                    9 -> rec.mAge.toDouble()
                    else -> 0.0
                }
                if (value == 0.0) {
                    continue
                }

                total[i] += value
                if ((min[i] == 0.0) || (min[i] > value)) {
                    min[i] = value
                }
                if ((max[i] == 0.0) || (max[i] < value)) {
                    max[i] = value
                }
                if (newDate[i] < rec.mDate) {
                    newDate[i] = rec.mDate
                    newest[i] = value
                }
                count[i]++
            }

            // Fill data for the adapter.
            val results = Vector<StBodyWeightAdapter.StItem>()
            val names = arrayOf(
                getString(R.string.recWeight),
                getString(R.string.recFat),
                getString(R.string.recIntFat),
                getString(R.string.recBmi),
                getString(R.string.recWc),
                getString(R.string.recBone),
                getString(R.string.recMuscle),
                getString(R.string.recWater),
                getString(R.string.recMetabolic),
                getString(R.string.recAge)
            )
            for (i in 0..<ITEM_COUNT) {
                if (count[i] == 0) {
                    continue
                }
                val item = StBodyWeightAdapter.StItem(
                    names[i],
                    min[i],
                    newest[i],
                    max[i],
                    (total[i] / count[i])
                )
                results.add(item)
            }

            findViewById<ListView>(R.id.listView).adapter = StBodyWeightAdapter(this, results)
        }
    }
}