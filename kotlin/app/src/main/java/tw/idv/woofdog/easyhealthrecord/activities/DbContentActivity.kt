package tw.idv.woofdog.easyhealthrecord.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

import tw.idv.woofdog.easyhealthrecord.R
import tw.idv.woofdog.easyhealthrecord.adapters.DbContentAdapter
import tw.idv.woofdog.easyhealthrecord.adapters.RecBloodGlucoseAdapter
import tw.idv.woofdog.easyhealthrecord.adapters.RecBloodPressureAdapter
import tw.idv.woofdog.easyhealthrecord.adapters.RecBodyWeightAdapter
import tw.idv.woofdog.easyhealthrecord.db.DbTableBase
import tw.idv.woofdog.easyhealthrecord.db.DbTableMemory
import tw.idv.woofdog.easyhealthrecord.db.DbTableSQLite
import tw.idv.woofdog.easyhealthrecord.dialogs.AboutDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.OptionsSyncDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.RecBloodGlucoseDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.RecBloodPressureDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.RecBodyWeightDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.StRangeDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.SyncFtpDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.SyncMsDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.SyncType
import tw.idv.woofdog.easyhealthrecord.dialogs.WarningDialog
import tw.idv.woofdog.easyhealthrecord.dialogs.readOptions
import tw.idv.woofdog.easyhealthrecord.utils.Utils

class DbContentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_db_content)
        supportActionBar?.hide()

        dbDir = "${applicationInfo.dataDir}/databases"
        dbName = intent.getStringExtra("dbName") ?: ""
        if (dbName == "") {
            dbTable = DbTableMemory()
            dbTable.isReadOnly = true
        } else {
            dbTable = DbTableSQLite()
        }
        dbTable.setFileName("$dbDir/$dbName")
        bodyWeightListAdapter = RecBodyWeightAdapter(this, dbTable)
        bloodPressureListAdapter = RecBloodPressureAdapter(this, dbTable)
        bloodGlucoseListAdapter = RecBloodGlucoseAdapter(this, dbTable)
        drawerLayout = findViewById(R.id.drawerLayout)
        progressBar = findViewById(R.id.progressBar)

        initActivityNavigation()

        setupViewComponent()

        progressBar.visibility = View.INVISIBLE
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val adapter = viewPager.adapter as DbContentAdapter
        when (tabLayout.selectedTabPosition) {
            1 -> {
                adapter.bloodPressureFragment.doContextItemSelected(item)
            }

            2 -> {
                adapter.bloodGlucoseFragment.doContextItemSelected(item)
            }

            else -> {
                adapter.bodyWeightFragment.doContextItemSelected(item)
            }
        }
        return super.onContextItemSelected(item)
    }

    /**
     * To update adapters because database file is synchronized from network.
     */
    fun syncUpdate() {
        progressBar.visibility = View.VISIBLE
        bodyWeightListAdapter.update()
        bloodPressureListAdapter.update()
        bloodGlucoseListAdapter.update()
        updateTitle()
        (viewPager.adapter as DbContentAdapter).bodyWeightFragment.scrollToLatest()
        (viewPager.adapter as DbContentAdapter).bloodPressureFragment.scrollToLatest()
        (viewPager.adapter as DbContentAdapter).bloodGlucoseFragment.scrollToLatest()
        progressBar.visibility = View.INVISIBLE
        dbFileSync = true
    }

    private fun setupViewComponent() {
        initTab()
        initTopAppBar()
        initBottomAppBar()
        initLeftDrawer()
    }

    private fun initActivityNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                dbTable.setFileName("")
                val bundle = Bundle()
                bundle.putBoolean("dbFileSync", dbFileSync)
                val data = Intent()
                data.putExtras(bundle)
                setResult(0, data)
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(this) {
                dbTable.setFileName("")
                val bundle = Bundle()
                bundle.putBoolean("dbFileSync", dbFileSync)
                val data = Intent()
                data.putExtras(bundle)
                setResult(0, data)
                finish()
            }
        }

        syncLoginLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val type = result.data?.extras?.getString("type")
                if (type == "ms") {
                    val code = result.data?.extras?.getString("code")
                    SyncMsDialog(this, dbTable, code!!, syncLoginLauncher, dbDir)
                } else {
                    dbTable.setFileName(dbName)
                }
            }
    }

    private fun initTab() {
        tabLayout = findViewById(R.id.tabLayout)

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = DbContentAdapter(
            supportFragmentManager,
            lifecycle,
            this,
            bodyWeightListAdapter,
            bloodPressureListAdapter,
            bloodGlucoseListAdapter
        )
        TabLayoutMediator(tabLayout, viewPager, true) { tab, position ->
            tab.text = when (position) {
                1 -> getString(R.string.recBloodPressure)
                2 -> getString(R.string.recBloodGlucose)
                else -> getString(R.string.recBodyWeight)
            }
            viewPager.currentItem = tab.position
        }.attach()
    }

    private fun initTopAppBar() {
        topAppBar = findViewById(R.id.topAppBar)
        updateTitle()
        topAppBar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun initBottomAppBar() {
        val appBar: MaterialToolbar = findViewById(R.id.recAppBar)

        appBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.recAddButton -> {
                    when (tabLayout.selectedTabPosition) {
                        1 -> {
                            RecBloodPressureDialog(
                                this,
                                RecBloodPressureDialog.Type.CREATE,
                                bloodPressureListAdapter,
                                0
                            )
                        }

                        2 -> {
                            RecBloodGlucoseDialog(
                                this,
                                RecBloodGlucoseDialog.Type.CREATE,
                                bloodGlucoseListAdapter,
                                0
                            )
                        }

                        else -> {
                            RecBodyWeightDialog(
                                this,
                                RecBodyWeightDialog.Type.CREATE,
                                bodyWeightListAdapter,
                                0
                            )
                        }
                    }
                    true
                }

                R.id.recSyncButton -> {
                    doSync()
                    true
                }

                R.id.recStButton -> {
                    when (tabLayout.selectedTabPosition) {
                        1 -> StRangeDialog(this, StRangeDialog.Type.BLOOD_PRESSURE, dbTable)
                        2 -> StRangeDialog(this, StRangeDialog.Type.BLOOD_GLUCOSE, dbTable)
                        else -> StRangeDialog(this, StRangeDialog.Type.BODY_WEIGHT, dbTable)
                    }
                    true
                }

                else -> true
            }
        }
    }

    private fun initLeftDrawer() {
        val navigationView: NavigationView = findViewById(R.id.leftDrawer)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.mOptSync -> OptionsSyncDialog(this)
                R.id.mAbout -> AboutDialog(this)
            }
            true
        }
    }

    private fun updateTitle() {
        topAppBar.title =
            (dbTable.getDescription() ?: "").ifEmpty { getString(R.string.titleUntitled) }
    }

    private fun doSync() {
        if (dbName.isEmpty()) {
            return
        }

        // Set file name to empty to force commit DB operations.
        val dbTableFileName = dbTable.getFileName()
        dbTable.setFileName("")
        dbTable.setFileName(dbTableFileName)

        val sp = Utils.getSharedPreferences(this)
        val options = readOptions(sp)
        when (options.syncType) {
            SyncType.FTP, SyncType.FTPS -> {
                SyncFtpDialog(this, dbTable, dbDir)
                return
            }

            SyncType.MS -> {
                SyncMsDialog(this, dbTable, "", syncLoginLauncher, dbDir)
                return
            }

            else -> {
                WarningDialog(this, 0, getString(R.string.dSyncWarnOpt)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
            }
        }
    }

    private lateinit var dbDir: String
    private lateinit var bodyWeightListAdapter: RecBodyWeightAdapter
    private lateinit var bloodPressureListAdapter: RecBloodPressureAdapter
    private lateinit var bloodGlucoseListAdapter: RecBloodGlucoseAdapter
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var syncLoginLauncher: ActivityResultLauncher<Intent>
    private var dbName = ""
    private lateinit var dbTable: DbTableBase
    private var dbFileSync = false
}
