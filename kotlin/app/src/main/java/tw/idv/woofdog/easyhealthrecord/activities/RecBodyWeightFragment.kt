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
import tw.idv.woofdog.easyhealthrecord.adapters.RecBodyWeightAdapter
import tw.idv.woofdog.easyhealthrecord.dialogs.RecBodyWeightDialog

class RecBodyWeightFragment(
    private val parentActivity: Activity,
    private val bodyWeightListAdapter: RecBodyWeightAdapter
) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_body_weight, container, false)

        setupViewComponent(view)
        init = true

        return view
    }

    fun scrollToLatest() {
        if (init && bodyWeightListAdapter.count > 0) {
            listView.setSelection(bodyWeightListAdapter.count - 1)
        }
    }

    fun doContextItemSelected(item: MenuItem) {
        val menuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val recItem = bodyWeightListAdapter.getRecListItems()[menuInfo.position] ?: return
        when (item.itemId) {
            R.id.recCreateAsButton -> RecBodyWeightDialog(
                parentActivity,
                RecBodyWeightDialog.Type.CREATE_AS,
                bodyWeightListAdapter,
                recItem.mId
            )

            R.id.recDeleteButton -> RecBodyWeightDialog(
                parentActivity,
                RecBodyWeightDialog.Type.DELETE,
                bodyWeightListAdapter,
                recItem.mId
            )

            R.id.recModifyButton -> RecBodyWeightDialog(
                parentActivity,
                RecBodyWeightDialog.Type.MODIFY,
                bodyWeightListAdapter,
                recItem.mId
            )
        }
    }

    private fun setupViewComponent(view: View) {
        listView = view.findViewById(R.id.recListView)
        listView.adapter = bodyWeightListAdapter
        listView.setOnCreateContextMenuListener { menu, _, _ ->
            parentActivity.menuInflater.inflate(R.menu.db_rec_context_menu, menu)
        }
        bodyWeightListAdapter.update()
        listView.setSelection(bodyWeightListAdapter.count - 1)
    }

    private lateinit var listView: ListView
    private var init = false
}
