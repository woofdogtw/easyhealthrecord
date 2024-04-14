package tw.idv.woofdog.easyhealthrecord.activities

import java.util.Vector

import android.os.Build
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.adapters.StBloodGlucoseAdapter
import tw.idv.woofdog.easyhealthrecord.db.DbBloodGlucose

/**
 * The activity to display blood glucose statistics information.
 */
class StBloodGlucoseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.titleStBloodGlucose)
        setContentView(R.layout.activity_statistics)

        val recList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(
                "recList",
                ArrayList::class.java
            ) as ArrayList<DbBloodGlucose>
        } else {
            intent.getSerializableExtra("recList") as ArrayList<DbBloodGlucose>
        }

        setupViewComponent(recList)
    }

    private fun setupViewComponent(recList: ArrayList<DbBloodGlucose>) {
        val ITEM_COUNT = 3
        val count = Array(ITEM_COUNT) { _ -> 0 }
        val total = Array(ITEM_COUNT) { _ -> 0.0 }
        val min = Array(ITEM_COUNT) { _ -> 0.0 }
        val max = Array(ITEM_COUNT) { _ -> 0.0 }
        val newest = Array(ITEM_COUNT) { _ -> 0.0 }
        val newDate = Array(ITEM_COUNT) { _ -> 0L }

        // Calculate data.
        for (rec in recList) {
            for (i in 0..<ITEM_COUNT) {
                val value = rec.mGlucose
                if (value == 0.0) {
                    continue
                }
                val meal = rec.mMeal.v

                total[meal] += value
                if ((min[meal] == 0.0) || (min[meal] > value)) {
                    min[meal] = value
                }
                if ((max[meal] == 0.0) || (max[meal] < value)) {
                    max[meal] = value
                }
                if (newDate[meal] < rec.mDate) {
                    newDate[meal] = rec.mDate
                    newest[meal] = value
                }
                count[meal]++
            }

            // Fill data for the adapter.
            val results = Vector<StBloodGlucoseAdapter.StItem>()
            val names = arrayOf(
                getString(R.string.recMealNormal),
                getString(R.string.recMealBefore),
                getString(R.string.recMealAfter)
            )
            for (i in 0..<ITEM_COUNT) {
                if (count[i] == 0) {
                    continue
                }
                val item = StBloodGlucoseAdapter.StItem(
                    names[i],
                    min[i],
                    newest[i],
                    max[i],
                    (total[i] / count[i])
                )
                results.add(item)
            }

            findViewById<ListView>(R.id.listView).adapter = StBloodGlucoseAdapter(this, results)
        }
    }
}