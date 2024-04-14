package tw.idv.woofdog.easyhealthrecord.dialogs

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface

import tw.idv.woofdog.easyhealthrecord.R

/**
 * The utility dialog for error message displaying.
 */
class WarningDialog(context: Context) : AlertDialog.Builder(context) {
    constructor(context: Context, titleRes: Int, msg: String) : this(context) {
        setCancelable(false)
        if (titleRes > 0) {
            setTitle(titleRes)
        }
        setMessage(msg)
        setPositiveButton(R.string.bOk) { dialog, _ -> dialog.dismiss() }
    }

    constructor(
        context: Context,
        titleRes: Int,
        msg: String,
        okListener: DialogInterface.OnClickListener
    ) : this(context) {
        setCancelable(false)
        if (titleRes > 0) {
            setTitle(titleRes)
        }
        setMessage(msg)
        setPositiveButton(R.string.bOk, okListener)
        setNeutralButton(R.string.bCancel) { dialog, _ -> dialog.dismiss() }
    }

    constructor(
        context: Context,
        titleRes: Int,
        msg: String,
        okListener: DialogInterface.OnClickListener,
        rejectListener: DialogInterface.OnClickListener
    ) : this(context) {
        setCancelable(false)
        if (titleRes > 0) {
            setTitle(titleRes)
        }
        setMessage(msg)
        setPositiveButton(R.string.bOk, okListener)
        setNegativeButton(R.string.bNo, rejectListener)
        setNeutralButton(R.string.bCancel) { dialog, _ -> dialog.dismiss() }
    }

    constructor(
        context: Context,
        titleRes: Int,
        msg: String,
        okListener: DialogInterface.OnClickListener,
        rejectListener: DialogInterface.OnClickListener,
        cancelListener: DialogInterface.OnClickListener
    ) : this(context) {
        setCancelable(false)
        if (titleRes > 0) {
            setTitle(titleRes)
        }
        setMessage(msg)
        setPositiveButton(R.string.bOk, okListener)
        setNegativeButton(R.string.bNo, rejectListener)
        setNeutralButton(R.string.bCancel, cancelListener)
    }
}
