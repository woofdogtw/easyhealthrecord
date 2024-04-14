package tw.idv.woofdog.easyhealthrecord.dialogs

import java.lang.Exception
import java.lang.Integer.parseInt
import java.util.Calendar

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TimePicker

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.adapters.RecBloodPressureAdapter
import tw.idv.woofdog.easyhealthrecord.db.DbBloodPressure
import tw.idv.woofdog.easyhealthrecord.db.DbTableBase
import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * Show the blood pressure editing dialog.
 */
class RecBloodPressureDialog(
    private val activity: Activity,
    private val type: Type,
    private val bloodPressureAdapter: RecBloodPressureAdapter,
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
        view = inflater.inflate(R.layout.dialog_blood_pressure, null)
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
        systolicEditText = view.findViewById(R.id.systolicEditText)
        diastolicEditText = view.findViewById(R.id.diastolicEditText)
        pulseEditText = view.findViewById(R.id.pulseEditText)
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

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
        systolicEditText.addTextChangedListener(editTextListener)
        diastolicEditText.addTextChangedListener(editTextListener)
        pulseEditText.addTextChangedListener(editTextListener)

        okButton.setOnClickListener {
            if (type == Type.DELETE) {
                bloodPressureAdapter.getDbTable().deleteBloodPressure(recId)
                bloodPressureAdapter.update()
                dialog.dismiss()
                return@setOnClickListener
            }

            val record = when (type) {
                Type.CREATE, Type.CREATE_AS -> DbBloodPressure()
                else -> DbBloodPressure(recId)
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
            record.mSystolic = try {
                parseInt(systolicEditText.text.toString())
            } catch (_: Exception) {
                0
            }
            record.mDiastolic = try {
                parseInt(diastolicEditText.text.toString())
            } catch (_: Exception) {
                0
            }
            record.mPulse = try {
                parseInt(pulseEditText.text.toString())
            } catch (_: Exception) {
                0
            }

            when (type) {
                Type.MODIFY -> {
                    bloodPressureAdapter.getDbTable().modifyBloodPressure(recId, record)
                    bloodPressureAdapter.update()
                    dialog.dismiss()
                }

                else -> {
                    bloodPressureAdapter.getDbTable().addBloodPressure(record)
                    bloodPressureAdapter.update()
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
            systolicEditText.setText("0")
            diastolicEditText.setText("0")
            pulseEditText.setText("0")
        } else {
            val record = bloodPressureAdapter.getDbTable().getBloodPressure(recId) ?: return

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
            }
            commentEditText.setText(record.mComment)
            systolicEditText.setText(Utils.format(record.mSystolic).ifEmpty { "0" })
            diastolicEditText.setText(Utils.format(record.mDiastolic).ifEmpty { "0" })
            pulseEditText.setText(Utils.format(record.mPulse).ifEmpty { "0" })

            if (type == Type.DELETE) {
                datePicker.isEnabled = false
                timePicker.isEnabled = false
                commentEditText.isEnabled = false
                systolicEditText.isEnabled = false
                diastolicEditText.isEnabled = false
                pulseEditText.isEnabled = false
            }
        }
    }

    private fun validateInput() {
        var valid = true

        val intList = listOf(systolicEditText, diastolicEditText, pulseEditText)
        for (item in intList) {
            item.error = null
            try {
                val v = parseInt(item.text.toString())
                if (v < 0) {
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
    private lateinit var systolicEditText: EditText
    private lateinit var diastolicEditText: EditText
    private lateinit var pulseEditText: EditText
}
