package tw.idv.woofdog.easyhealthrecord.activities

import java.util.Vector

import android.os.Build
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.adapters.StBloodPressureAdapter
import tw.idv.woofdog.easyhealthrecord.db.DbBloodPressure

/**
 * The activity to display blood pressure statistics information.
 */
class StBloodPressureActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.titleStBloodPressure)
        setContentView(R.layout.activity_statistics)

        val recList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(
                "recList",
                ArrayList::class.java
            ) as ArrayList<DbBloodPressure>
        } else {
            intent.getSerializableExtra("recList") as ArrayList<DbBloodPressure>
        }

        setupViewComponent(recList)
    }

    private fun setupViewComponent(recList: ArrayList<DbBloodPressure>) {
        val ITEM_COUNT = 3
        val count = Array(ITEM_COUNT) { _ -> 0 }
        val total = Array(ITEM_COUNT) { _ -> 0 }
        val min = Array(ITEM_COUNT) { _ -> 0 }
        val max = Array(ITEM_COUNT) { _ -> 0 }
        val newest = Array(ITEM_COUNT) { _ -> 0 }
        val newDate = Array(ITEM_COUNT) { _ -> 0L }

        // Calculate data.
        for (rec in recList) {
            for (i in 0..<ITEM_COUNT) {
                val value = when (i) {
                    0 -> rec.mSystolic
                    1 -> rec.mDiastolic
                    2 -> rec.mPulse
                    else -> 0
                }
                if (value == 0) {
                    continue
                }

                total[i] += value
                if ((min[i] == 0) || (min[i] > value)) {
                    min[i] = value
                }
                if ((max[i] == 0) || (max[i] < value)) {
                    max[i] = value
                }
                if (newDate[i] < rec.mDate) {
                    newDate[i] = rec.mDate
                    newest[i] = value
                }
                count[i]++
            }

            // Fill data for the adapter.
            val results = Vector<StBloodPressureAdapter.StItem>()
            val names = arrayOf(
                getString(R.string.recSystolic),
                getString(R.string.recDiastolic),
                getString(R.string.recPulse)
            )
            for (i in 0..<ITEM_COUNT) {
                if (count[i] == 0) {
                    continue
                }
                val item = StBloodPressureAdapter.StItem(
                    names[i],
                    min[i],
                    newest[i],
                    max[i],
                    (total[i].toDouble() / count[i])
                )
                results.add(item)
            }

            findViewById<ListView>(R.id.listView).adapter = StBloodPressureAdapter(this, results)
        }
    }
}