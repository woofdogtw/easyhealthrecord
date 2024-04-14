package tw.idv.woofdog.easyhealthrecord.dialogs

import java.lang.Double
import java.lang.Double.parseDouble
import java.lang.Exception
import java.util.Calendar

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Spinner
import android.widget.TimePicker

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.adapters.RecBloodGlucoseAdapter
import tw.idv.woofdog.easyhealthrecord.db.DbBloodGlucose
import tw.idv.woofdog.easyhealthrecord.db.DbTableBase
import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * Show the blood glucose editing dialog.
 */
class RecBloodGlucoseDialog(
    private val activity: Activity,
    private val type: Type,
    private val bloodGlucoseAdapter: RecBloodGlucoseAdapter,
    private val recId: Long
) {
    enum class Type {
        CREATE, CREATE_AS, DELETE, MODIFY
    }

    private val dialog: AlertDialog
    private val view: View

    init {
        val title = when (type) {
            Type.CREATE, Type.CREATE_AS -> R.string.dRecTitleCreate
            Type.DELETE -> R.string.dRecTitleDelete
            Type.MODIFY -> R.string.dRecTitleModify
        }

        val inflater =
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.dialog_blood_glucose, null)
        dialog = AlertDialog.Builder(activity).setTitle(title).setView(view)
            .setPositiveButton(R.string.bOk, null)
            .setNeutralButton(R.string.bCancel) { dialog, _ -> dialog.dismiss() }.show()

        setupViewComponent()
    }

    private fun setupViewComponent() {
        datePicker = view.findViewById(R.id.datePicker)
        timePicker = view.findViewById(R.id.timePicker)
        timePicker.setIs24HourView(true)
        commentEditText = view.findViewById(R.id.commentEditText)
        glucoseEditText = view.findViewById(R.id.glucoseEditText)
        mealSpinner = view.findViewById(R.id.mealSpinner)
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

        val str = arrayOf(
            activity.getString(R.string.recMealNormal),
            activity.getString(R.string.recMealBefore),
            activity.getString(R.string.recMealAfter)
        )
        val spinnerAdapter =
            ArrayAdapter(activity, android.R.layout.simple_spinner_item, str)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        mealSpinner.adapter = spinnerAdapter

        setupRecComponent()

        val editTextListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        }
        glucoseEditText.addTextChangedListener(editTextListener)

        okButton.setOnClickListener {
            if (type == Type.DELETE) {
                bloodGlucoseAdapter.getDbTable().deleteBloodGlucose(recId)
                bloodGlucoseAdapter.update()
                dialog.dismiss()
                return@setOnClickListener
            }

            val record = when (type) {
                Type.CREATE, Type.CREATE_AS -> DbBloodGlucose()
                else -> DbBloodGlucose(recId)
            }
            record.mDate = DbTableBase.getDateFromYMDhms(
                datePicker.year,
                datePicker.month + 1,
                datePicker.dayOfMonth,
                timePicker.hour,
                timePicker.minute,
                0
            )
            record.mComment = commentEditText.text.toString()
            record.mGlucose = try {
                parseDouble(glucoseEditText.text.toString())
            } catch (_: Exception) {
                0.0
            }
            record.mMeal = when (mealSpinner.selectedItemPosition) {
                DbBloodGlucose.Meal.BEFORE.v -> DbBloodGlucose.Meal.BEFORE
                DbBloodGlucose.Meal.AFTER.v -> DbBloodGlucose.Meal.AFTER
                else -> DbBloodGlucose.Meal.NORMAL
            }

            when (type) {
                Type.MODIFY -> {
                    bloodGlucoseAdapter.getDbTable().modifyBloodGlucose(recId, record)
                    bloodGlucoseAdapter.update()
                    dialog.dismiss()
                }

                else -> {
                    bloodGlucoseAdapter.getDbTable().addBloodGlucose(record)
                    bloodGlucoseAdapter.update()
                    dialog.dismiss()
                }
            }
        }

        validateInput()
    }

    private fun setupRecComponent() {
        if (type == Type.CREATE) {
            val date = Calendar.getInstance()
            datePicker.init(
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DATE),
                null
            )
            timePicker.hour = date.get(Calendar.HOUR_OF_DAY)
            timePicker.minute = date.get(Calendar.MINUTE)
            glucoseEditText.setText("0.0")
        } else {
            val record = bloodGlucoseAdapter.getDbTable().getBloodGlucose(recId) ?: return

            if (type == Type.CREATE_AS) {
                val date = Calendar.getInstance()
                datePicker.init(
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH),
                    date.get(Calendar.DATE),
                    null
                )
                timePicker.hour = date.get(Calendar.HOUR_OF_DAY)
                timePicker.minute = date.get(Calendar.MINUTE)
            } else {
                val date = record.mDate
                datePicker.init(
                    DbTableBase.getYearFromDate(date),
                    DbTableBase.getMonthFromDate(date) - 1,
                    DbTableBase.getDayFromDate(date),
                    null
                )
                timePicker.hour = DbTableBase.getHourFromDate(date)
                timePicker.minute = DbTableBase.getMinuteFromDate(date)
                mealSpinner.setSelection(record.mMeal.v)
            }
            commentEditText.setText(record.mComment)
            glucoseEditText.setText(Utils.format(record.mGlucose).ifEmpty { "0.0" })

            if (type == Type.DELETE) {
                datePicker.isEnabled = false
                timePicker.isEnabled = false
                commentEditText.isEnabled = false
                glucoseEditText.isEnabled = false
                mealSpinner.isEnabled = false
            }
        }
    }

    private fun validateInput() {
        var valid = true

        val doubleList = listOf(glucoseEditText)
        for (item in doubleList) {
            item.error = null
            try {
                val v = Double.parseDouble(item.text.toString())
                if (v < 0.0) {
                    valid = false
                    item.error = activity.getString(R.string.dRecErrValue)
                }
            } catch (e: NumberFormatException) {
                valid = false
                item.error = activity.getString(R.string.dRecErrValue)
            }
        }

        okButton.isEnabled = valid
    }

    private lateinit var okButton: Button
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var commentEditText: EditText
    private lateinit var glucoseEditText: EditText
    private lateinit var mealSpinner: Spinner
}
