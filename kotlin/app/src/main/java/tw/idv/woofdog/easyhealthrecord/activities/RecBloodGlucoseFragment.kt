package tw.idv.woofdog.easyhealthrecord.activities

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import androidx.fragment.app.Fragment

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.adapters.RecBloodGlucoseAdapter
import tw.idv.woofdog.easyhealthrecord.dialogs.RecBloodGlucoseDialog

class RecBloodGlucoseFragment(
    private val parentActivity: Activity,
    private val bloodGlucoseListAdapter: RecBloodGlucoseAdapter
) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_blood_glucose, container, false)

        setupViewComponent(view)
        init = true

        return view
    }

    fun scrollToLatest() {
        if (init && bloodGlucoseListAdapter.count > 0) {
            listView.setSelection(bloodGlucoseListAdapter.count - 1)
        }
    }

    fun doContextItemSelected(item: MenuItem) {
        val menuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val recItem = bloodGlucoseListAdapter.getRecListItems()[menuInfo.position] ?: return
        when (item.itemId) {
            R.id.recCreateAsButton -> RecBloodGlucoseDialog(
                parentActivity,
                RecBloodGlucoseDialog.Type.CREATE_AS,
                bloodGlucoseListAdapter,
                recItem.mId
            )

            R.id.recDeleteButton -> RecBloodGlucoseDialog(
                parentActivity,
                RecBloodGlucoseDialog.Type.DELETE,
                bloodGlucoseListAdapter,
                recItem.mId
            )

            R.id.recModifyButton -> RecBloodGlucoseDialog(
                parentActivity,
                RecBloodGlucoseDialog.Type.MODIFY,
                bloodGlucoseListAdapter,
                recItem.mId
            )
        }
    }

    private fun setupViewComponent(view: View) {
        listView = view.findViewById(R.id.recListView)
        listView.adapter = bloodGlucoseListAdapter
        listView.setOnCreateContextMenuListener { menu, _, _ ->
            parentActivity.menuInflater.inflate(R.menu.db_rec_context_menu, menu)
        }
        bloodGlucoseListAdapter.update()
        listView.setSelection(bloodGlucoseListAdapter.count - 1)
    }

    private lateinit var listView: ListView
    private var init = false
}
