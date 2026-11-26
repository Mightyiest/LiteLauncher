package app.olauncher.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import app.olauncher.BuildConfig
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentHomeBinding
import app.olauncher.helper.FontHelper
import app.olauncher.helper.MonochromeIconHelper
import app.olauncher.helper.WeatherHelper
import app.olauncher.helper.appUsagePermissionGranted
import app.olauncher.helper.dpToPx
import app.olauncher.helper.expandNotificationDrawer
import app.olauncher.helper.getChangedAppTheme
import app.olauncher.helper.getUserHandleFromString
import app.olauncher.helper.isDarkThemeOn
import app.olauncher.helper.isPackageInstalled
import app.olauncher.helper.openAlarmApp
import app.olauncher.helper.openCalendar
import app.olauncher.helper.openCameraApp
import app.olauncher.helper.openDialerApp
import app.olauncher.helper.openSearch
import app.olauncher.helper.setPlainWallpaperByTheme
import app.olauncher.helper.showToast
import app.olauncher.listener.OnSwipeTouchListener
import app.olauncher.listener.ViewSwipeTouchListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private var verifyHomeAppsJob: Job? = null
    private var loadSideDockJob: Job? = null

    private val requestWeatherPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.suppressBackToHome = false
        if (!granted) prefs.weatherIpFallback = true
        viewModel.refreshWeather(force = true) { ok ->
            if (!ok && isAdded) requireContext().showToast(getString(R.string.weather_unavailable))
        }
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private data class HomeAppSlot(
        val location: Int,
        val packageName: String,
        val userString: String,
        val isShortcut: Boolean,
        val shortcutId: String?,
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        initObservers()
        setHomeAlignment(prefs.homeAlignment)
        initSwipeTouchListener()
        initClickListeners()
    }

    override fun onResume() {
        super.onResume()
        populateHomeScreen(false)
        applyHomeFont()
        viewModel.isOlauncherDefault()
        if (prefs.showStatusBar) showStatusBar()
        else hideStatusBar()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.lock -> {}
            // Home button for recents feature disabled
            // R.id.recents -> {}
            R.id.clock -> openClockApp()
            R.id.date -> openCalendarApp()
            R.id.tvWeather -> onWeatherClick()
            R.id.setDefaultLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.tvScreenTime -> openScreenTimeDigitalWellbeing()
            R.id.sideDockApp1 -> sideDockClicked(1)
            R.id.sideDockApp2 -> sideDockClicked(2)
            R.id.sideDockApp3 -> sideDockClicked(3)
            R.id.sideDockApp4 -> sideDockClicked(4)
            R.id.sideDockApp5 -> sideDockClicked(5)
            R.id.sideDockApp6 -> sideDockClicked(6)

            else -> {
                try { // Launch app
                    val appLocation = view.tag.toString().toInt()
                    homeAppClicked(appLocation)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openClockApp() {
        if (prefs.clockAppPackage.isBlank())
            openAlarmApp(requireContext())
        else
            launchApp(
                "Clock",
                prefs.clockAppPackage,
                prefs.clockAppClassName,
                prefs.clockAppUser
            )
    }

    private fun openCalendarApp() {
        if (prefs.calendarAppPackage.isBlank())
            openCalendar(requireContext())
        else
            launchApp(
                "Calendar",
                prefs.calendarAppPackage,
                prefs.calendarAppClassName,
                prefs.calendarAppUser
            )
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.homeApp1 -> showAppList(Constants.FLAG_SET_HOME_APP_1, prefs.appName1.isNotEmpty(), true)
            R.id.homeApp2 -> showAppList(Constants.FLAG_SET_HOME_APP_2, prefs.appName2.isNotEmpty(), true)
            R.id.homeApp3 -> showAppList(Constants.FLAG_SET_HOME_APP_3, prefs.appName3.isNotEmpty(), true)
            R.id.homeApp4 -> showAppList(Constants.FLAG_SET_HOME_APP_4, prefs.appName4.isNotEmpty(), true)
            R.id.homeApp5 -> showAppList(Constants.FLAG_SET_HOME_APP_5, prefs.appName5.isNotEmpty(), true)
            R.id.homeApp6 -> showAppList(Constants.FLAG_SET_HOME_APP_6, prefs.appName6.isNotEmpty(), true)
            R.id.homeApp7 -> showAppList(Constants.FLAG_SET_HOME_APP_7, prefs.appName7.isNotEmpty(), true)
            R.id.homeApp8 -> showAppList(Constants.FLAG_SET_HOME_APP_8, prefs.appName8.isNotEmpty(), true)
            R.id.sideDockApp1 -> showAppList(Constants.FLAG_SET_SIDE_DOCK_1, false, true)
            R.id.sideDockApp2 -> showAppList(Constants.FLAG_SET_SIDE_DOCK_2, false, true)
            R.id.sideDockApp3 -> showAppList(Constants.FLAG_SET_SIDE_DOCK_3, false, true)
            R.id.sideDockApp4 -> showAppList(Constants.FLAG_SET_SIDE_DOCK_4, false, true)
            R.id.sideDockApp5 -> showAppList(Constants.FLAG_SET_SIDE_DOCK_5, false, true)
            R.id.sideDockApp6 -> showAppList(Constants.FLAG_SET_SIDE_DOCK_6, false, true)
            R.id.clock -> {
                showAppList(Constants.FLAG_SET_CLOCK_APP)
                prefs.clockAppPackage = ""
                prefs.clockAppClassName = ""
                prefs.clockAppUser = ""
            }

            R.id.date -> {
                showAppList(Constants.FLAG_SET_CALENDAR_APP)
                prefs.calendarAppPackage = ""
                prefs.calendarAppClassName = ""
                prefs.calendarAppUser = ""
            }

            R.id.tvScreenTime -> {
                showAppList(Constants.FLAG_SET_SCREEN_TIME_APP)
                prefs.screenTimeAppPackage = ""
                prefs.screenTimeAppClassName = ""
                prefs.screenTimeAppUser = ""
            }

            R.id.tvWeather -> {
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            R.id.setDefaultLauncher -> {
                prefs.hideSetDefaultLauncher = true
                binding.setDefaultLauncher.visibility = View.GONE
                if (viewModel.isOlauncherDefault.value != true) {
                    requireContext().showToast(R.string.set_as_default_launcher)
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                }
            }
        }
        return true
    }

    private fun initObservers() {
        if (prefs.firstSettingsOpen) {
            binding.firstRunTips.visibility = View.VISIBLE
            binding.setDefaultLauncher.visibility = View.GONE
        } else binding.firstRunTips.visibility = View.GONE

        viewModel.refreshHome.observe(viewLifecycleOwner) {
            populateHomeScreen(it)
        }
        viewModel.isOlauncherDefault.observe(viewLifecycleOwner, Observer {
            if (it != true) {
                if (prefs.dailyWallpaper && prefs.appTheme == AppCompatDelegate.MODE_NIGHT_YES) {
                    prefs.dailyWallpaper = false
                    viewModel.cancelWallpaperWorker()
                }
                prefs.homeBottomAlignment = false
                setHomeAlignment()
            }
            if (binding.firstRunTips.isVisible) return@Observer
            binding.setDefaultLauncher.isVisible = it.not() && prefs.hideSetDefaultLauncher.not()
        })
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            setHomeAlignment(it)
        }
        viewModel.toggleDateTime.observe(viewLifecycleOwner) {
            populateDateTime()
        }
        viewModel.screenTimeValue.observe(viewLifecycleOwner) {
            it?.let { binding.tvScreenTime.text = it }
        }
        viewModel.weatherValue.observe(viewLifecycleOwner) {
            if (prefs.weatherMode == Constants.WeatherMode.OFF) {
                binding.tvWeather.isVisible = false
            } else {
                binding.tvWeather.isVisible = true
                if (!it.isNullOrBlank()) {
                    binding.tvWeather.text = it
                }
            }
        }
        viewModel.refreshFont.observe(viewLifecycleOwner) {
            applyHomeFont()
        }
        // Home button for recents feature disabled
        // viewModel.showRecentApps.observe(viewLifecycleOwner) {
        //     binding.recents.performClick()
        // }
    }

    private fun initSwipeTouchListener() {
        val context = requireContext()
        binding.mainLayout.setOnTouchListener(getSwipeGestureListener(context))
        binding.homeApp1.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp1))
        binding.homeApp2.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp2))
        binding.homeApp3.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp3))
        binding.homeApp4.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp4))
        binding.homeApp5.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp5))
        binding.homeApp6.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp6))
        binding.homeApp7.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp7))
        binding.homeApp8.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp8))
    }

    private fun initClickListeners() {
        binding.lock.setOnClickListener(this)
        // Home button for recents feature disabled
        // binding.recents.setOnClickListener(this)
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.tvWeather.setOnClickListener(this)
        binding.tvWeather.setOnLongClickListener(this)
        binding.clock.setOnLongClickListener(this)
        binding.date.setOnLongClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
        binding.setDefaultLauncher.setOnLongClickListener(this)
        binding.tvScreenTime.setOnClickListener(this)
        binding.tvScreenTime.setOnLongClickListener(this)
        listOf(
            binding.sideDockApp1,
            binding.sideDockApp2,
            binding.sideDockApp3,
            binding.sideDockApp4,
            binding.sideDockApp5,
            binding.sideDockApp6
        ).forEach { dockView ->
            dockView.setOnClickListener(this)
            dockView.setOnLongClickListener(this)
        }
    }

    private fun setHomeAlignment(horizontalGravity: Int = prefs.homeAlignment) {
        val verticalGravity = if (prefs.homeBottomAlignment) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        binding.homeAppsLayout.gravity = horizontalGravity or verticalGravity
        binding.dateTimeLayout.gravity = horizontalGravity
        binding.tvWeather.gravity = horizontalGravity
        binding.homeApp1.gravity = horizontalGravity
        binding.homeApp2.gravity = horizontalGravity
        binding.homeApp3.gravity = horizontalGravity
        binding.homeApp4.gravity = horizontalGravity
        binding.homeApp5.gravity = horizontalGravity
        binding.homeApp6.gravity = horizontalGravity
        binding.homeApp7.gravity = horizontalGravity
        binding.homeApp8.gravity = horizontalGravity
    }

    private fun applyHomeFont() {
        FontHelper.applyFont(
            prefs.fontFamily,
            binding.clock,
            binding.date,
            binding.tvWeather,
            binding.tvScreenTime,
            binding.homeApp1,
            binding.homeApp2,
            binding.homeApp3,
            binding.homeApp4,
            binding.homeApp5,
            binding.homeApp6,
            binding.homeApp7,
            binding.homeApp8,
            binding.firstRunTips,
            binding.setDefaultLauncher
        )
    }

    private fun populateDateTime() {
        binding.dateTimeLayout.isVisible =
            prefs.dateTimeVisibility != Constants.DateTime.OFF ||
            prefs.weatherMode != Constants.WeatherMode.OFF
        binding.clock.isVisible = Constants.DateTime.isTimeVisible(prefs.dateTimeVisibility)
        binding.date.isVisible = Constants.DateTime.isDateVisible(prefs.dateTimeVisibility)

//        var dateText = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        var dateText = dateFormat.format(Date())

        if (!prefs.showStatusBar) {
            val battery = (requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (battery > 0)
                dateText = getString(R.string.day_battery, dateText, battery)
        }
        binding.date.text = dateText.replace(".,", ",")
        populateWeather()
    }

    private fun populateWeather() {
        if (prefs.weatherMode == Constants.WeatherMode.OFF) {
            binding.tvWeather.isVisible = false
            return
        }
        binding.tvWeather.isVisible = true
        if (!WeatherHelper.canFetchWeather(requireContext(), prefs)) {
            binding.tvWeather.text = getString(R.string.tap_for_local_weather)
            return
        }
        binding.tvWeather.text = prefs.cachedWeatherText.ifBlank { getString(R.string.refreshing_weather) }
        viewModel.refreshWeather(force = false)
    }

    private fun onWeatherClick() {
        if (!WeatherHelper.canFetchWeather(requireContext(), prefs)) {
            viewModel.suppressBackToHome = true
            try {
                requestWeatherPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            } catch (e: Exception) {
                viewModel.suppressBackToHome = false
                e.printStackTrace()
            }
        } else {
            binding.tvWeather.text = getString(R.string.refreshing_weather)
            requireContext().showToast(getString(R.string.refreshing_weather))
            viewModel.refreshWeather(force = true) { ok ->
                if (!ok && isAdded) requireContext().showToast(getString(R.string.weather_unavailable))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun populateScreenTime() {
        if (requireContext().appUsagePermissionGranted().not()) return

        viewModel.getTodaysScreenTime()
        binding.tvScreenTime.visibility = View.VISIBLE

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val horizontalMargin = if (isLandscape) 64.dpToPx() else 10.dpToPx()
        val marginTop = if (isLandscape) {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 36.dpToPx() else 56.dpToPx()
        } else {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 45.dpToPx() else 72.dpToPx()
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = marginTop
            marginStart = horizontalMargin
            marginEnd = horizontalMargin
            gravity = if (prefs.homeAlignment == Gravity.END) Gravity.START else Gravity.END
        }
        binding.tvScreenTime.layoutParams = params
        binding.tvScreenTime.setPadding(10.dpToPx())
    }

    private fun populateHomeScreen(appCountUpdated: Boolean) {
        if (appCountUpdated) hideHomeApps()
        populateDateTime()
        populateSideDock()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            populateScreenTime()

        verifyHomeAppsJob?.cancel()

        val homeAppsNum = prefs.homeAppsNum
        if (homeAppsNum == 0) return

        val homeAppViews: List<TextView> = listOf(
            binding.homeApp1,
            binding.homeApp2,
            binding.homeApp3,
            binding.homeApp4,
            binding.homeApp5,
            binding.homeApp6,
            binding.homeApp7,
            binding.homeApp8
        )

        val slotsToVerify = mutableListOf<HomeAppSlot>()
        val count = homeAppsNum.coerceAtMost(homeAppViews.size)
        for (i in 1..count) {
            val textView = homeAppViews[i - 1]
            val appName = prefs.getAppName(i)
            val packageName = prefs.getAppPackage(i)
            textView.visibility = View.VISIBLE
            textView.text = appName
            if (packageName.isNotEmpty() || appName.isNotEmpty()) {
                slotsToVerify.add(
                    HomeAppSlot(
                        location = i,
                        packageName = packageName,
                        userString = prefs.getAppUser(i),
                        isShortcut = prefs.getIsShortcut(i),
                        shortcutId = prefs.getShortcutId(i)
                    )
                )
            }
        }

        if (slotsToVerify.isEmpty()) return

        val appContext = requireContext().applicationContext
        verifyHomeAppsJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val invalidLocations = slotsToVerify.mapNotNull { slot ->
                val valid = isHomeAppInstalled(
                    appContext,
                    slot.packageName,
                    slot.userString,
                    slot.isShortcut,
                    slot.shortcutId
                )
                if (valid) null else slot.location
            }

            if (invalidLocations.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    if (_binding == null || !isAdded) return@withContext
                    for (location in invalidLocations) {
                        homeAppViews[location - 1].text = ""
                        prefs.clearHomeAppSlot(location)
                    }
                }
            }
        }
    }

    private fun isHomeAppInstalled(
        context: Context,
        packageName: String,
        userString: String,
        isShortcut: Boolean,
        shortcutId: String?,
    ): Boolean {
        if (packageName.isBlank()) return false
        val userHandle = getUserHandleFromString(context, userString)

        if (isShortcut) {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }
            return try {
                val shortcuts = launcherApps.getShortcuts(query, userHandle)
                shortcuts?.any { it.id == shortcutId } == true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        return isPackageInstalled(context, packageName, userString)
    }

    private fun hideHomeApps() {
        binding.homeApp1.visibility = View.GONE
        binding.homeApp2.visibility = View.GONE
        binding.homeApp3.visibility = View.GONE
        binding.homeApp4.visibility = View.GONE
        binding.homeApp5.visibility = View.GONE
        binding.homeApp6.visibility = View.GONE
        binding.homeApp7.visibility = View.GONE
        binding.homeApp8.visibility = View.GONE
    }

    private fun launchAppOrShortcut(
        appName: String,
        packageName: String,
        activityClassName: String?,
        shortcutId: String?,
        isShortcut: Boolean,
        userString: String,
        fallback: (() -> Unit)? = null,
    ) {
        if (appName.isEmpty()) {
            showLongPressToast()
            return
        }
        if (isShortcut && !shortcutId.isNullOrEmpty()) {
            launchShortcut(
                packageName = packageName,
                shortcutId = shortcutId,
                shortcutLabel = appName,
                userString = userString
            )
        } else if (packageName.isNotEmpty()) {
            launchApp(
                appName = appName,
                packageName = packageName,
                activityClassName = activityClassName,
                userString = userString
            )
        } else {
            fallback?.invoke()
        }
    }

    private fun launchShortcut(shortcutId: String, packageName: String, shortcutLabel: String, userString: String) {
        viewModel.selectedApp(
            AppModel.PinnedShortcut(
                shortcutId = shortcutId,
                appLabel = shortcutLabel,
                user = getUserHandleFromString(requireContext(), userString),
                key = null,
                appPackage = packageName,
                isNew = false,
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun launchApp(appName: String, packageName: String, activityClassName: String?, userString: String) {
        viewModel.selectedApp(
            AppModel.App(
                appLabel = appName,
                key = null,
                appPackage = packageName,
                activityClassName = activityClassName,
                isNew = false,
                user = getUserHandleFromString(requireContext(), userString)
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun homeAppClicked(location: Int) {
        launchAppOrShortcut(
            appName = prefs.getAppName(location),
            packageName = prefs.getAppPackage(location),
            activityClassName = prefs.getAppActivityClassName(location),
            shortcutId = prefs.getShortcutId(location),
            isShortcut = prefs.getIsShortcut(location),
            userString = prefs.getAppUser(location)
        )
    }

    private fun sideDockClicked(position: Int) {
        val pkg = prefs.getSideDockPackage(position)
        if (pkg.isBlank()) {
            showLongPressToast()
            return
        }
        launchAppOrShortcut(
            appName = prefs.getSideDockAppName(position).ifBlank { pkg },
            packageName = pkg,
            activityClassName = prefs.getSideDockActivityClassName(position),
            shortcutId = prefs.getSideDockShortcutId(position),
            isShortcut = prefs.getSideDockIsShortcut(position),
            userString = prefs.getSideDockUser(position)
        )
    }

    private fun populateSideDock() {
        loadSideDockJob?.cancel()

        val pos = prefs.sideDockPosition
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (!isLandscape) {
            val startPad = if (pos == Constants.SideDockPosition.LEFT) 72.dpToPx() else 24.dpToPx()
            val endPad = if (pos == Constants.SideDockPosition.RIGHT) 72.dpToPx() else 24.dpToPx()
            binding.homeAppsLayout.setPaddingRelative(
                startPad,
                binding.homeAppsLayout.paddingTop,
                endPad,
                binding.homeAppsLayout.paddingBottom
            )
        }

        if (pos == Constants.SideDockPosition.OFF) {
            binding.sideDockLayout.isVisible = false
            return
        }

        val dockParams = binding.sideDockLayout.layoutParams as FrameLayout.LayoutParams
        val targetGravity =
            (if (pos == Constants.SideDockPosition.LEFT) Gravity.START else Gravity.END) or Gravity.CENTER_VERTICAL
        if (dockParams.gravity != targetGravity) {
            dockParams.gravity = targetGravity
            binding.sideDockLayout.layoutParams = dockParams
        }
        binding.sideDockLayout.isVisible = true

        val dockViews = listOf(
            binding.sideDockApp1,
            binding.sideDockApp2,
            binding.sideDockApp3,
            binding.sideDockApp4,
            binding.sideDockApp5,
            binding.sideDockApp6
        )
        val count = prefs.sideDockCount
        val slotsToLoad = mutableListOf<Triple<Int, ImageView, HomeAppSlot>>()

        for (i in 1..dockViews.size) {
            val iconView = dockViews[i - 1]
            if (i > count) {
                iconView.visibility = View.GONE
                continue
            }
            iconView.visibility = View.VISIBLE
            val pkg = prefs.getSideDockPackage(i)
            val appName = prefs.getSideDockAppName(i)
            if (pkg.isBlank()) {
                iconView.setImageResource(R.drawable.ic_add_minimal)
                iconView.contentDescription = getString(R.string.side_icon_dock)
            } else {
                iconView.contentDescription = appName.ifBlank { pkg }
                slotsToLoad.add(
                    Triple(
                        i,
                        iconView,
                        HomeAppSlot(
                            location = i,
                            packageName = pkg,
                            userString = prefs.getSideDockUser(i),
                            isShortcut = prefs.getSideDockIsShortcut(i),
                            shortcutId = prefs.getSideDockShortcutId(i)
                        )
                    )
                )
            }
        }

        if (slotsToLoad.isEmpty()) return

        val appContext = requireContext().applicationContext
        val isDark = requireContext().isDarkThemeOn()
        val iconSizePx = 24.dpToPx()
        loadSideDockJob = viewLifecycleOwner.lifecycleScope.launch {
            for ((index, iconView, slot) in slotsToLoad) {
                val valid = withContext(Dispatchers.IO) {
                    isHomeAppInstalled(
                        appContext,
                        slot.packageName,
                        slot.userString,
                        slot.isShortcut,
                        slot.shortcutId
                    )
                }
                if (!isAdded || _binding == null) return@launch
                if (!valid) {
                    prefs.clearSideDockSlot(index)
                    iconView.setImageResource(R.drawable.ic_add_minimal)
                    iconView.contentDescription = getString(R.string.side_icon_dock)
                    continue
                }
                val userHandle = getUserHandleFromString(appContext, slot.userString)
                val bitmap = MonochromeIconHelper.loadMonochromeIcon(
                    appContext,
                    slot.packageName,
                    userHandle,
                    isDark,
                    iconSizePx
                )
                if (!isAdded || _binding == null) return@launch
                if (bitmap != null) {
                    iconView.setImageBitmap(bitmap)
                } else {
                    iconView.setImageResource(R.drawable.ic_add_minimal)
                }
            }
        }
    }

    private fun openSwipeRightApp() {
        if (!prefs.swipeRightEnabled) return
        launchAppOrShortcut(
            appName = prefs.appNameSwipeRight,
            packageName = prefs.appPackageSwipeRight,
            activityClassName = prefs.appActivityClassNameRight,
            shortcutId = prefs.shortcutIdSwipeRight,
            isShortcut = prefs.isShortcutSwipeRight,
            userString = prefs.appUserSwipeRight,
            fallback = { openDialerApp(requireContext()) }
        )
    }

    private fun openSwipeLeftApp() {
        if (!prefs.swipeLeftEnabled) return
        launchAppOrShortcut(
            appName = prefs.appNameSwipeLeft,
            packageName = prefs.appPackageSwipeLeft,
            activityClassName = prefs.appActivityClassNameSwipeLeft,
            shortcutId = prefs.shortcutIdSwipeLeft,
            isShortcut = prefs.isShortcutSwipeLeft,
            userString = prefs.appUserSwipeLeft,
            fallback = { openCameraApp(requireContext()) }
        )
    }

    private fun showAppList(flag: Int, rename: Boolean = false, includeHiddenApps: Boolean = false) {
        viewModel.getAppList(includeHiddenApps)
        try {
            findNavController().navigate(
                R.id.action_mainFragment_to_appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
        } catch (e: Exception) {
            findNavController().navigate(
                R.id.appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
            e.printStackTrace()
        }
    }

    private fun swipeDownAction() {
        when (prefs.swipeDownAction) {
            Constants.SwipeDownAction.SEARCH -> openSearch(requireContext())
            else -> expandNotificationDrawer(requireContext())
        }
    }

    private fun lockPhone() {
        requireActivity().runOnUiThread {
            try {
                deviceManager.lockNow()
            } catch (e: SecurityException) {
                requireContext().showToast(getString(R.string.please_turn_on_double_tap_to_unlock), Toast.LENGTH_LONG)
                findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
            } catch (e: Exception) {
                requireContext().showToast(getString(R.string.launcher_failed_to_lock_device), Toast.LENGTH_LONG)
                prefs.lockModeOn = false
            }
        }
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun changeAppTheme() {
        if (prefs.dailyWallpaper.not()) return
        val changedAppTheme = getChangedAppTheme(requireContext(), prefs.appTheme)
        prefs.appTheme = changedAppTheme
        if (prefs.dailyWallpaper) {
            setPlainWallpaperByTheme(requireContext(), changedAppTheme)
            viewModel.setWallpaperWorker()
        }
        requireActivity().recreate()
    }

    private fun openScreenTimeDigitalWellbeing() {
        if (prefs.screenTimeAppPackage.isNotBlank()) {
            launchApp(
                "Screen Time",
                prefs.screenTimeAppPackage,
                prefs.screenTimeAppClassName,
                prefs.screenTimeAppUser
            )
            return
        }
        val intent = Intent()
        try {
            intent.setClassName(
                Constants.DIGITAL_WELLBEING_PACKAGE_NAME,
                Constants.DIGITAL_WELLBEING_ACTIVITY
            )
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                intent.setClassName(
                    Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME,
                    Constants.DIGITAL_WELLBEING_SAMSUNG_ACTIVITY
                )
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLongPressToast() = requireContext().showToast(getString(R.string.long_press_to_select_app))

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getSwipeGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    viewModel.firstOpen(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                if (!prefs.lockModeOn) return
                if (BuildConfig.USE_ACCESSIBILITY_LOCK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    binding.lock.performClick()
                else
                    lockPhone()
            }

            override fun onClick() {
                super.onClick()
                viewModel.checkForMessages.call()
            }
        }
    }

    private fun getViewSwipeTouchListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }
        }
    }

    override fun onDestroyView() {
        verifyHomeAppsJob?.cancel()
        loadSideDockJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}