package tw.idv.woofdog.easyhealthrecord.dialogs

import java.lang.Double.parseDouble
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
import tw.idv.woofdog.easyhealthrecord.adapters.RecBodyWeightAdapter
import tw.idv.woofdog.easyhealthrecord.db.DbBodyWeight
import tw.idv.woofdog.easyhealthrecord.db.DbTableBase
import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * Show the body weight editing dialog.
 */
class RecBodyWeightDialog(
    private val activity: Activity,
    private val type: Type,
    private val bodyWeightAdapter: RecBodyWeightAdapter,
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
        view = inflater.inflate(R.layout.dialog_body_weight, null)
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
        weightEditText = view.findViewById(R.id.weightEditText)
        fatEditText = view.findViewById(R.id.fatEditText)
        intFatEditText = view.findViewById(R.id.intFatEditText)
        bmiEditText = view.findViewById(R.id.bmiEditText)
        wcEditText = view.findViewById(R.id.wcEditText)
        boneEditText = view.findViewById(R.id.boneEditText)
        muscleEditText = view.findViewById(R.id.muscleEditText)
        waterEditText = view.findViewById(R.id.waterEditText)
        metabolicEditText = view.findViewById(R.id.metabolicEditText)
        ageEditText = view.findViewById(R.id.ageEditText)
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
        weightEditText.addTextChangedListener(editTextListener)
        fatEditText.addTextChangedListener(editTextListener)
        intFatEditText.addTextChangedListener(editTextListener)
        bmiEditText.addTextChangedListener(editTextListener)
        wcEditText.addTextChangedListener(editTextListener)
        boneEditText.addTextChangedListener(editTextListener)
        muscleEditText.addTextChangedListener(editTextListener)
        waterEditText.addTextChangedListener(editTextListener)
        metabolicEditText.addTextChangedListener(editTextListener)
        ageEditText.addTextChangedListener(editTextListener)

        okButton.setOnClickListener {
            if (type == Type.DELETE) {
                bodyWeightAdapter.getDbTable().deleteBodyWeight(recId)
                bodyWeightAdapter.update()
                dialog.dismiss()
                return@setOnClickListener
            }

            val record = when (type) {
                Type.CREATE, Type.CREATE_AS -> DbBodyWeight()
                else -> DbBodyWeight(recId)
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
            record.mWeight = try {
                parseDouble(weightEditText.text.toString())
            } catch (_: Exception) {
                0.0
            }
            record.mFat = try {
                parseDouble(fatEditText.text.toString())
            } catch (_: Exception) {
                0.0
            }
            record.mIntFat = try {
                parseDouble(intFatEditText.text.toString())
            } catch (_: Exception) {
                0.0
            }
            record.mBmi = try {
                parseDouble(bmiEditText.text.toString())
            } catch (_: Exception) {
                0.0
            }
            record.mWc = try {
                parseDouble(wcEditText.text.toString())
            } catch (_: Exception) {
                0.0
            }
            record.mBone = try {
                parseDouble(boneEditText.text.toString())
            } catch (_: Exception) {
                0.0
            }
            record.mMuscle = try {
                parseDouble(muscleEditText.text.toString())
            } catch (_: Exception) {
                0.0
            }
            record.mWater = try {
                parseDouble(waterEditText.text.toString())
            } catch (_: Exception) {
                0.0
            }
            record.mMetabolic = try {
                parseInt(metabolicEditText.text.toString())
            } catch (_: Exception) {
                0
            }
            record.mAge = try {
                parseInt(ageEditText.text.toString())
            } catch (_: Exception) {
                0
            }

            when (type) {
                Type.MODIFY -> {
                    bodyWeightAdapter.getDbTable().modifyBodyWeight(recId, record)
                    bodyWeightAdapter.update()
                    dialog.dismiss()
                }

                else -> {
                    bodyWeightAdapter.getDbTable().addBodyWeight(record)
                    bodyWeightAdapter.update()
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
            weightEditText.setText("0.0")
            fatEditText.setText("0.0")
            intFatEditText.setText("0.0")
            bmiEditText.setText("0.0")
            wcEditText.setText("0.0")
            boneEditText.setText("0.0")
            muscleEditText.setText("0.0")
            waterEditText.setText("0.0")
            metabolicEditText.setText("0")
            ageEditText.setText("0")
        } else {
            val record = bodyWeightAdapter.getDbTable().getBodyWeight(recId) ?: return

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
            weightEditText.setText(Utils.format(record.mWeight).ifEmpty { "0.0" })
            fatEditText.setText(Utils.format(record.mFat).ifEmpty { "0.0" })
            intFatEditText.setText(Utils.format(record.mIntFat).ifEmpty { "0.0" })
            bmiEditText.setText(Utils.format(record.mBmi).ifEmpty { "0.0" })
            wcEditText.setText(Utils.format(record.mWc).ifEmpty { "0.0" })
            boneEditText.setText(Utils.format(record.mBone).ifEmpty { "0.0" })
            muscleEditText.setText(Utils.format(record.mMuscle).ifEmpty { "0.0" })
            waterEditText.setText(Utils.format(record.mWater).ifEmpty { "0.0" })
            metabolicEditText.setText(Utils.format(record.mMetabolic).ifEmpty { "0" })
            ageEditText.setText(Utils.format(record.mAge).ifEmpty { "0" })

            if (type == Type.DELETE) {
                datePicker.isEnabled = false
                timePicker.isEnabled = false
                commentEditText.isEnabled = false
                weightEditText.isEnabled = false
                fatEditText.isEnabled = false
                intFatEditText.isEnabled = false
                bmiEditText.isEnabled = false
                wcEditText.isEnabled = false
                boneEditText.isEnabled = false
                muscleEditText.isEnabled = false
                waterEditText.isEnabled = false
                metabolicEditText.isEnabled = false
                ageEditText.isEnabled = false
            }
        }
    }

    private fun validateInput() {
        var valid = true

        val doubleList = listOf(
            weightEditText,
            fatEditText,
            intFatEditText,
            bmiEditText,
            wcEditText,
            boneEditText,
            muscleEditText,
            waterEditText
        )
        for (item in doubleList) {
            item.error = null
            try {
                val v = parseDouble(item.text.toString())
                if (v < 0.0) {
                    valid = false
                    item.error = activity.getString(R.string.dRecErrValue)
                }
            } catch (e: NumberFormatException) {
                valid = false
                item.error = activity.getString(R.string.dRecErrValue)
            }
        }

        val intList = listOf(metabolicEditText, ageEditText)
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
    private lateinit var weightEditText: EditText
    private lateinit var fatEditText: EditText
    private lateinit var intFatEditText: EditText
    private lateinit var bmiEditText: EditText
    private lateinit var wcEditText: EditText
    private lateinit var boneEditText: EditText
    private lateinit var muscleEditText: EditText
    private lateinit var waterEditText: EditText
    private lateinit var metabolicEditText: EditText
    private lateinit var ageEditText: EditText
}
