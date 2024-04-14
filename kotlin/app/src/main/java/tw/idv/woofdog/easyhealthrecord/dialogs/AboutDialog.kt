package tw.idv.woofdog.easyhealthrecord.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * Show the About dialog.
 */
class AboutDialog(private val activity: Activity) {
    private val dialog: AlertDialog
    private val view: View

    init {
        val inflater =
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.dialog_about, null)
        dialog = AlertDialog.Builder(activity).setTitle(R.string.mAbout).setView(view)
            .setPositiveButton(R.string.bOk) { dialog, _ -> dialog.dismiss() }.show()

        setupViewComponent()
    }

    private fun setupViewComponent() {
        var text: TextView = view.findViewById(R.id.aboutProgVerTextView)
        text.text =
            "${activity.getString(R.string.progVersion)} ${Utils.getPackageVersion(activity)}"

        text = view.findViewById(R.id.aboutProgDescTextView)
        text.text =
            "${activity.getString(R.string.progName)} ${activity.getString(R.string.progAbout)}"

        text = view.findViewById(R.id.aboutProgMailTextView)
        text.text = Html.fromHtml(activity.getString(R.string.progMail), 0)
        text.movementMethod = LinkMovementMethod.getInstance()

        text = view.findViewById(R.id.aboutProgWebsiteTextView)
        text.text = Html.fromHtml(activity.getString(R.string.progWebsite), 0)
        text.movementMethod = LinkMovementMethod.getInstance()
    }
}
