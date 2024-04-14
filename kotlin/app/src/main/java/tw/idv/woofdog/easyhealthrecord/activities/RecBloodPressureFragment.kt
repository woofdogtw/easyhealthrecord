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
import tw.idv.woofdog.easyhealthrecord.adapters.RecBloodPressureAdapter
import tw.idv.woofdog.easyhealthrecord.dialogs.RecBloodPressureDialog

class RecBloodPressureFragment(
    private val parentActivity: Activity,
    private val bloodPressureListAdapter: RecBloodPressureAdapter
) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_blood_pressure, container, false)

        setupViewComponent(view)
        init = true

        return view
    }

    fun scrollToLatest() {
        if (init && bloodPressureListAdapter.count > 0) {
            listView.setSelection(bloodPressureListAdapter.count - 1)
        }
    }

    fun doContextItemSelected(item: MenuItem) {
        val menuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val recItem = bloodPressureListAdapter.getRecListItems()[menuInfo.position] ?: return
        when (item.itemId) {
            R.id.recCreateAsButton -> RecBloodPressureDialog(
                parentActivity,
                RecBloodPressureDialog.Type.CREATE_AS,
                bloodPressureListAdapter,
                recItem.mId
            )

            R.id.recDeleteButton -> RecBloodPressureDialog(
                parentActivity,
                RecBloodPressureDialog.Type.DELETE,
                bloodPressureListAdapter,
                recItem.mId
            )

            R.id.recModifyButton -> RecBloodPressureDialog(
                parentActivity,
                RecBloodPressureDialog.Type.MODIFY,
                bloodPressureListAdapter,
                recItem.mId
            )
        }
    }

    private fun setupViewComponent(view: View) {
        listView = view.findViewById(R.id.recListView)
        listView.adapter = bloodPressureListAdapter
        listView.setOnCreateContextMenuListener { menu, _, _ ->
            parentActivity.menuInflater.inflate(R.menu.db_rec_context_menu, menu)
        }
        bloodPressureListAdapter.update()
        listView.setSelection(bloodPressureListAdapter.count - 1)
    }

    private lateinit var listView: ListView
    private var init = false
}
