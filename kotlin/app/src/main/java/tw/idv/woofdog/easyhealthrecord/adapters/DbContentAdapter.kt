package tw.idv.woofdog.easyhealthrecord.adapters

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

import tw.idv.woofdog.easyhealthrecord.activities.RecBloodGlucoseFragment
import tw.idv.woofdog.easyhealthrecord.activities.RecBloodPressureFragment
import tw.idv.woofdog.easyhealthrecord.activities.RecBodyWeightFragment

class DbContentAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    parentActivity: Activity,
    bodyWeightAdapter: RecBodyWeightAdapter,
    bloodPressureAdapter: RecBloodPressureAdapter,
    bloodGlucoseAdapter: RecBloodGlucoseAdapter,
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> bloodPressureFragment
            2 -> bloodGlucoseFragment
            else -> bodyWeightFragment
        }
    }

    val bodyWeightFragment = RecBodyWeightFragment(parentActivity, bodyWeightAdapter)
    val bloodPressureFragment = RecBloodPressureFragment(parentActivity, bloodPressureAdapter)
    val bloodGlucoseFragment = RecBloodGlucoseFragment(parentActivity, bloodGlucoseAdapter)
}
