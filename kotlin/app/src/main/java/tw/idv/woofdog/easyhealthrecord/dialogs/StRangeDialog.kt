package tw.idv.woofdog.easyhealthrecord.dialogs

import java.util.Calendar

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.RadioButton

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.activities.StBloodGlucoseActivity
import tw.idv.woofdog.easyhealthrecord.activities.StBloodPressureActivity
import tw.idv.woofdog.easyhealthrecord.activities.StBodyWeightActivity
import tw.idv.woofdog.easyhealthrecord.db.DbBloodGlucose
import tw.idv.woofdog.easyhealthrecord.db.DbBloodPressure
import tw.idv.woofdog.easyhealthrecord.db.DbBodyWeight
import tw.idv.woofdog.easyhealthrecord.db.DbTableBase
import java.util.Vector

/**
 * Show the range dialog for displaying range.
 */
class StRangeDialog(
    private val activity: Activity,
    private val type: Type,
    private val dbTable: DbTableBase
) {
    enum class Type {
        BODY_WEIGHT, BLOOD_PRESSURE, BLOOD_GLUCOSE
    }

    private val dialog: AlertDialog
    private val view: View

    init {
        val title = when (type) {
            Type.BODY_WEIGHT -> R.string.recBodyWeight
            Type.BLOOD_PRESSURE -> R.string.recBloodPressure
            Type.BLOOD_GLUCOSE -> R.string.recBloodGlucose
        }
        val inflater =
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.dialog_st_range, null)
        dialog = AlertDialog.Builder(activity).setTitle(title).setView(view)
            .setPositiveButton(R.string.bOk, null)
            .setNeutralButton(R.string.bCancel) { dialog, _ -> dialog.dismiss() }.show()

        setupViewComponent()
    }

    private fun setupViewComponent() {
        val useButton = view.findViewById<RadioButton>(R.id.allButton)
        useButton.isChecked = true

        val date = Calendar.getInstance()
        fromDatePicker = view.findViewById(R.id.fromDatePicker)
        fromDatePicker.init(date.get(Calendar.YEAR), date.get(Calendar.MONTH), 1, null)
        fromDatePicker.setOnDateChangedListener { _, y, m, d ->
            val from = DbTableBase.getDateFromYMDhms(y, m + 1, d)
            val to = DbTableBase.getDateFromYMDhms(
                toDatePicker.year,
                toDatePicker.month + 1,
                toDatePicker.dayOfMonth
            )
            if (from > to) {
                toDatePicker.updateDate(y, m, d)
            }
        }
        toDatePicker = view.findViewById(R.id.toDatePicker)
        toDatePicker.init(
            date.get(Calendar.YEAR), date.get(Calendar.MONTH),
            date.getActualMaximum(Calendar.DAY_OF_MONTH), null
        )
        toDatePicker.setOnDateChangedListener { _, y, m, d ->
            val from = DbTableBase.getDateFromYMDhms(
                fromDatePicker.year,
                fromDatePicker.month + 1,
                fromDatePicker.dayOfMonth
            )
            val to = DbTableBase.getDateFromYMDhms(y, m + 1, d)
            if (to < from) {
                fromDatePicker.updateDate(y, m, d)
            }
        }
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

        okButton.setOnClickListener {
            dialog.dismiss()
            doStatistics()
        }
    }

    private fun doStatistics() {
        var minDate = -1L
        var maxDate = -1L

        if (!view.findViewById<RadioButton>(R.id.allButton).isChecked) {
            minDate = DbTableBase.getDateFromYMDhms(
                fromDatePicker.year,
                fromDatePicker.month + 1,
                fromDatePicker.dayOfMonth,
            )
            maxDate = DbTableBase.getDateFromYMDhms(
                toDatePicker.year,
                toDatePicker.month + 1,
                toDatePicker.dayOfMonth,
                23, 59, 59
            )
        }

        val intent = when (type) {
            Type.BODY_WEIGHT -> {
                val intent = Intent(activity, StBodyWeightActivity::class.java)
                val recList = if (view.findViewById<RadioButton>(R.id.allButton).isChecked) {
                    dbTable.getBodyWeights()
                } else {
                    dbTable.getBodyWeights(minDate, maxDate)
                }?: Vector<DbBodyWeight>()
                intent.putExtra("recList", recList)
                intent
            }
            Type.BLOOD_PRESSURE -> {
                val intent = Intent(activity, StBloodPressureActivity::class.java)
                val recList = if (view.findViewById<RadioButton>(R.id.allButton).isChecked) {
                    dbTable.getBloodPressures()
                } else {
                    dbTable.getBloodPressures(minDate, maxDate)
                }?: Vector<DbBloodPressure>()
                intent.putExtra("recList", recList)
                intent
            }
            Type.BLOOD_GLUCOSE -> {
                val intent = Intent(activity, StBloodGlucoseActivity::class.java)
                val recList = if (view.findViewById<RadioButton>(R.id.allButton).isChecked) {
                    dbTable.getBloodGlucoses()
                } else {
                    dbTable.getBloodGlucoses(minDate, maxDate)
                }?: Vector<DbBloodGlucose>()
                intent.putExtra("recList", recList)
                intent
            }
        }
        activity.startActivity(intent)
    }

    private lateinit var fromDatePicker: DatePicker
    private lateinit var toDatePicker: DatePicker
    private lateinit var okButton: Button
}
