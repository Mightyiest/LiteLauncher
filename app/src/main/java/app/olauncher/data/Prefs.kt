package app.olauncher.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {
    private val PREFS_FILENAME = "app.olauncher"

    private val FIRST_OPEN = "FIRST_OPEN"
    private val FIRST_OPEN_TIME = "FIRST_OPEN_TIME"
    private val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
    private val FIRST_HIDE = "FIRST_HIDE"
    private val USER_STATE = "USER_STATE"
    private val LOCK_MODE = "LOCK_MODE"
    private val HOME_APPS_NUM = "HOME_APPS_NUM"
    private val AUTO_SHOW_KEYBOARD = "AUTO_SHOW_KEYBOARD"
    private val KEYBOARD_MESSAGE = "KEYBOARD_MESSAGE"
    private val DAILY_WALLPAPER = "DAILY_WALLPAPER"
    private val DAILY_WALLPAPER_URL = "DAILY_WALLPAPER_URL"
    private val HOME_ALIGNMENT = "HOME_ALIGNMENT"
    private val HOME_BOTTOM_ALIGNMENT = "HOME_BOTTOM_ALIGNMENT"
    private val APP_LABEL_ALIGNMENT = "APP_LABEL_ALIGNMENT"
    private val STATUS_BAR = "STATUS_BAR"
    private val DATE_TIME_VISIBILITY = "DATE_TIME_VISIBILITY"
    private val SWIPE_LEFT_ENABLED = "SWIPE_LEFT_ENABLED"
    private val SWIPE_RIGHT_ENABLED = "SWIPE_RIGHT_ENABLED"
    private val HIDDEN_APPS = "HIDDEN_APPS"
    private val HIDDEN_APPS_UPDATED = "HIDDEN_APPS_UPDATED"
    private val SHOW_HINT_COUNTER = "SHOW_HINT_COUNTER"
    private val APP_THEME = "APP_THEME"
    private val ABOUT_CLICKED = "ABOUT_CLICKED"
    private val RATE_CLICKED = "RATE_CLICKED"
    private val WALLPAPER_MSG_SHOWN = "WALLPAPER_MSG_SHOWN"
    private val SHARE_SHOWN_TIME = "SHARE_SHOWN_TIME"
    private val SWIPE_DOWN_ACTION = "SWIPE_DOWN_ACTION"
    private val TEXT_SIZE_SCALE = "TEXT_SIZE_SCALE"
    private val PRO_MESSAGE_SHOWN = "PRO_MESSAGE_SHOWN"
    private val HIDE_SET_DEFAULT_LAUNCHER = "HIDE_SET_DEFAULT_LAUNCHER"
    private val SCREEN_TIME_LAST_UPDATED = "SCREEN_TIME_LAST_UPDATED"
    private val LAUNCHER_RESTART_TIMESTAMP = "LAUNCHER_RECREATE_TIMESTAMP"
    private val SHOWN_ON_DAY_OF_YEAR = "SHOWN_ON_DAY_OF_YEAR"
    private val WEATHER_MODE = "WEATHER_MODE"
    private val CACHED_WEATHER_TEXT = "CACHED_WEATHER_TEXT"
    private val WEATHER_LAST_UPDATED = "WEATHER_LAST_UPDATED"
    private val LAST_WEATHER_LAT = "LAST_WEATHER_LAT"
    private val LAST_WEATHER_LON = "LAST_WEATHER_LON"
    private val FONT_FAMILY = "FONT_FAMILY"
    private val LOCK_HIDDEN_APPS = "LOCK_HIDDEN_APPS"
    private val MINDFUL_APPS = "MINDFUL_APPS"
    // Home button for recents feature disabled
    // private val HOME_BUTTON_SHOW_RECENTS = "HOME_BUTTON_SHOW_RECENTS"

    private val APP_NAME_1 = "APP_NAME_1"
    private val APP_NAME_2 = "APP_NAME_2"
    private val APP_NAME_3 = "APP_NAME_3"
    private val APP_NAME_4 = "APP_NAME_4"
    private val APP_NAME_5 = "APP_NAME_5"
    private val APP_NAME_6 = "APP_NAME_6"
    private val APP_NAME_7 = "APP_NAME_7"
    private val APP_NAME_8 = "APP_NAME_8"
    private val APP_PACKAGE_1 = "APP_PACKAGE_1"
    private val APP_PACKAGE_2 = "APP_PACKAGE_2"
    private val APP_PACKAGE_3 = "APP_PACKAGE_3"
    private val APP_PACKAGE_4 = "APP_PACKAGE_4"
    private val APP_PACKAGE_5 = "APP_PACKAGE_5"
    private val APP_PACKAGE_6 = "APP_PACKAGE_6"
    private val APP_PACKAGE_7 = "APP_PACKAGE_7"
    private val APP_PACKAGE_8 = "APP_PACKAGE_8"
    private val APP_ACTIVITY_CLASS_NAME_1 = "APP_ACTIVITY_CLASS_NAME_1"
    private val APP_ACTIVITY_CLASS_NAME_2 = "APP_ACTIVITY_CLASS_NAME_2"
    private val APP_ACTIVITY_CLASS_NAME_3 = "APP_ACTIVITY_CLASS_NAME_3"
    private val APP_ACTIVITY_CLASS_NAME_4 = "APP_ACTIVITY_CLASS_NAME_4"
    private val APP_ACTIVITY_CLASS_NAME_5 = "APP_ACTIVITY_CLASS_NAME_5"
    private val APP_ACTIVITY_CLASS_NAME_6 = "APP_ACTIVITY_CLASS_NAME_6"
    private val APP_ACTIVITY_CLASS_NAME_7 = "APP_ACTIVITY_CLASS_NAME_7"
    private val APP_ACTIVITY_CLASS_NAME_8 = "APP_ACTIVITY_CLASS_NAME_8"
    private val APP_USER_1 = "APP_USER_1"
    private val APP_USER_2 = "APP_USER_2"
    private val APP_USER_3 = "APP_USER_3"
    private val APP_USER_4 = "APP_USER_4"
    private val APP_USER_5 = "APP_USER_5"
    private val APP_USER_6 = "APP_USER_6"
    private val APP_USER_7 = "APP_USER_7"
    private val APP_USER_8 = "APP_USER_8"

    private val APP_NAME_SWIPE_LEFT = "APP_NAME_SWIPE_LEFT"
    private val APP_NAME_SWIPE_RIGHT = "APP_NAME_SWIPE_RIGHT"
    private val APP_PACKAGE_SWIPE_LEFT = "APP_PACKAGE_SWIPE_LEFT"
    private val APP_PACKAGE_SWIPE_RIGHT = "APP_PACKAGE_SWIPE_RIGHT"
    private val APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT = "APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT"
    private val APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT = "APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT"
    private val APP_USER_SWIPE_LEFT = "APP_USER_SWIPE_LEFT"
    private val APP_USER_SWIPE_RIGHT = "APP_USER_SWIPE_RIGHT"
    private val CLOCK_APP_PACKAGE = "CLOCK_APP_PACKAGE"
    private val CLOCK_APP_USER = "CLOCK_APP_USER"
    private val CLOCK_APP_CLASS_NAME = "CLOCK_APP_CLASS_NAME"
    private val CALENDAR_APP_PACKAGE = "CALENDAR_APP_PACKAGE"
    private val CALENDAR_APP_USER = "CALENDAR_APP_USER"
    private val CALENDAR_APP_CLASS_NAME = "CALENDAR_APP_CLASS_NAME"
    private val SCREEN_TIME_APP_PACKAGE = "SCREEN_TIME_APP_PACKAGE"
    private val SCREEN_TIME_APP_USER = "SCREEN_TIME_APP_USER"
    private val SCREEN_TIME_APP_CLASS_NAME = "SCREEN_TIME_APP_CLASS_NAME"

    private val IS_SHORTCUT_1 = "IS_SHORTCUT_1"
    private val SHORTCUT_ID_1 = "SHORTCUT_ID_1"
    private val IS_SHORTCUT_2 = "IS_SHORTCUT_2"
    private val SHORTCUT_ID_2 = "SHORTCUT_ID_2"
    private val IS_SHORTCUT_3 = "IS_SHORTCUT_3"
    private val SHORTCUT_ID_3 = "SHORTCUT_ID_3"
    private val IS_SHORTCUT_4 = "IS_SHORTCUT_4"
    private val SHORTCUT_ID_4 = "SHORTCUT_ID_4"
    private val IS_SHORTCUT_5 = "IS_SHORTCUT_5"
    private val SHORTCUT_ID_5 = "SHORTCUT_ID_5"
    private val IS_SHORTCUT_6 = "IS_SHORTCUT_6"
    private val SHORTCUT_ID_6 = "SHORTCUT_ID_6"
    private val IS_SHORTCUT_7 = "IS_SHORTCUT_7"
    private val SHORTCUT_ID_7 = "SHORTCUT_ID_7"
    private val IS_SHORTCUT_8 = "IS_SHORTCUT_8"
    private val SHORTCUT_ID_8 = "SHORTCUT_ID_8"

    private val SHORTCUT_ID_SWIPE_LEFT = "SHORTCUT_ID_SWIPE_LEFT"
    private val IS_SHORTCUT_SWIPE_LEFT = "IS_SHORTCUT_SWIPE_LEFT"
    private val SHORTCUT_ID_SWIPE_RIGHT = "SHORTCUT_ID_SWIPE_RIGHT"
    private val IS_SHORTCUT_SWIPE_RIGHT = "IS_SHORTCUT_SWIPE_RIGHT"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var firstOpen: Boolean
        get() = prefs.getBoolean(FIRST_OPEN, true)
        set(value) = prefs.edit { putBoolean(FIRST_OPEN, value) }

    var firstOpenTime: Long
        get() = prefs.getLong(FIRST_OPEN_TIME, 0L)
        set(value) = prefs.edit { putLong(FIRST_OPEN_TIME, value) }

    var firstSettingsOpen: Boolean
        get() = prefs.getBoolean(FIRST_SETTINGS_OPEN, true)
        set(value) = prefs.edit { putBoolean(FIRST_SETTINGS_OPEN, value) }

    var firstHide: Boolean
        get() = prefs.getBoolean(FIRST_HIDE, true)
        set(value) = prefs.edit { putBoolean(FIRST_HIDE, value) }

    var userState: String
        get() = prefs.getString(USER_STATE, Constants.UserState.START).toString()
        set(value) = prefs.edit { putString(USER_STATE, value) }

    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit { putBoolean(LOCK_MODE, value) }

    var autoShowKeyboard: Boolean
        get() = prefs.getBoolean(AUTO_SHOW_KEYBOARD, true)
        set(value) = prefs.edit { putBoolean(AUTO_SHOW_KEYBOARD, value) }

    var keyboardMessageShown: Boolean
        get() = prefs.getBoolean(KEYBOARD_MESSAGE, false)
        set(value) = prefs.edit { putBoolean(KEYBOARD_MESSAGE, value) }

    var dailyWallpaper: Boolean
        get() = prefs.getBoolean(DAILY_WALLPAPER, false)
        set(value) = prefs.edit { putBoolean(DAILY_WALLPAPER, value) }

    var dailyWallpaperUrl: String
        get() = prefs.getString(DAILY_WALLPAPER_URL, "").toString()
        set(value) = prefs.edit { putString(DAILY_WALLPAPER_URL, value) }

    var homeAppsNum: Int
        get() = prefs.getInt(HOME_APPS_NUM, 4)
        set(value) = prefs.edit { putInt(HOME_APPS_NUM, value) }

    var homeAlignment: Int
        get() = prefs.getInt(HOME_ALIGNMENT, Gravity.START)
        set(value) = prefs.edit { putInt(HOME_ALIGNMENT, value) }

    var homeBottomAlignment: Boolean
        get() = prefs.getBoolean(HOME_BOTTOM_ALIGNMENT, false)
        set(value) = prefs.edit { putBoolean(HOME_BOTTOM_ALIGNMENT, value) }

    var appLabelAlignment: Int
        get() = prefs.getInt(APP_LABEL_ALIGNMENT, Gravity.START)
        set(value) = prefs.edit { putInt(APP_LABEL_ALIGNMENT, value) }

    var showStatusBar: Boolean
        get() = prefs.getBoolean(STATUS_BAR, false)
        set(value) = prefs.edit { putBoolean(STATUS_BAR, value) }

    var dateTimeVisibility: Int
        get() = prefs.getInt(DATE_TIME_VISIBILITY, Constants.DateTime.ON)
        set(value) = prefs.edit { putInt(DATE_TIME_VISIBILITY, value) }

    var swipeLeftEnabled: Boolean
        get() = prefs.getBoolean(SWIPE_LEFT_ENABLED, true)
        set(value) = prefs.edit { putBoolean(SWIPE_LEFT_ENABLED, value) }

    var swipeRightEnabled: Boolean
        get() = prefs.getBoolean(SWIPE_RIGHT_ENABLED, true)
        set(value) = prefs.edit { putBoolean(SWIPE_RIGHT_ENABLED, value) }

    var appTheme: Int
        get() = prefs.getInt(APP_THEME, AppCompatDelegate.MODE_NIGHT_YES)
        set(value) = prefs.edit { putInt(APP_THEME, value) }

    var textSizeScale: Float
        get() = prefs.getFloat(TEXT_SIZE_SCALE, 1.0f)
        set(value) = prefs.edit { putFloat(TEXT_SIZE_SCALE, value) }

    var proMessageShown: Boolean
        get() = prefs.getBoolean(PRO_MESSAGE_SHOWN, false)
        set(value) = prefs.edit { putBoolean(PRO_MESSAGE_SHOWN, value) }

    var hideSetDefaultLauncher: Boolean
        get() = prefs.getBoolean(HIDE_SET_DEFAULT_LAUNCHER, false)
        set(value) = prefs.edit { putBoolean(HIDE_SET_DEFAULT_LAUNCHER, value) }

    var screenTimeLastUpdated: Long
        get() = prefs.getLong(SCREEN_TIME_LAST_UPDATED, 0L)
        set(value) = prefs.edit { putLong(SCREEN_TIME_LAST_UPDATED, value) }

    var launcherRestartTimestamp: Long
        get() = prefs.getLong(LAUNCHER_RESTART_TIMESTAMP, 0L)
        set(value) = prefs.edit { putLong(LAUNCHER_RESTART_TIMESTAMP, value) }

    var shownOnDayOfYear: Int
        get() = prefs.getInt(SHOWN_ON_DAY_OF_YEAR, 0)
        set(value) = prefs.edit { putInt(SHOWN_ON_DAY_OF_YEAR, value) }

    // Home button for recents feature disabled
    // var homeButtonShowRecents: Boolean
    //     get() = prefs.getBoolean(HOME_BUTTON_SHOW_RECENTS, false)
    //     set(value) = prefs.edit { putBoolean(HOME_BUTTON_SHOW_RECENTS, value).apply() }

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, null)?.toMutableSet() ?: mutableSetOf()
        set(value) = prefs.edit { putStringSet(HIDDEN_APPS, value.toSet()) }

    var hiddenAppsUpdated: Boolean
        get() = prefs.getBoolean(HIDDEN_APPS_UPDATED, false)
        set(value) = prefs.edit { putBoolean(HIDDEN_APPS_UPDATED, value) }

    var toShowHintCounter: Int
        get() = prefs.getInt(SHOW_HINT_COUNTER, 1)
        set(value) = prefs.edit { putInt(SHOW_HINT_COUNTER, value) }

    var aboutClicked: Boolean
        get() = prefs.getBoolean(ABOUT_CLICKED, false)
        set(value) = prefs.edit { putBoolean(ABOUT_CLICKED, value) }

    var rateClicked: Boolean
        get() = prefs.getBoolean(RATE_CLICKED, false)
        set(value) = prefs.edit { putBoolean(RATE_CLICKED, value) }

    var wallpaperMsgShown: Boolean
        get() = prefs.getBoolean(WALLPAPER_MSG_SHOWN, false)
        set(value) = prefs.edit { putBoolean(WALLPAPER_MSG_SHOWN, value) }

    var shareShownTime: Long
        get() = prefs.getLong(SHARE_SHOWN_TIME, 0L)
        set(value) = prefs.edit { putLong(SHARE_SHOWN_TIME, value) }

    var swipeDownAction: Int
        get() = prefs.getInt(SWIPE_DOWN_ACTION, Constants.SwipeDownAction.NOTIFICATIONS)
        set(value) = prefs.edit { putInt(SWIPE_DOWN_ACTION, value) }

    var appName1: String
        get() = prefs.getString(APP_NAME_1, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_1, value) }

    var appName2: String
        get() = prefs.getString(APP_NAME_2, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_2, value) }

    var appName3: String
        get() = prefs.getString(APP_NAME_3, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_3, value) }

    var appName4: String
        get() = prefs.getString(APP_NAME_4, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_4, value) }

    var appName5: String
        get() = prefs.getString(APP_NAME_5, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_5, value) }

    var appName6: String
        get() = prefs.getString(APP_NAME_6, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_6, value) }

    var appName7: String
        get() = prefs.getString(APP_NAME_7, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_7, value) }

    var appName8: String
        get() = prefs.getString(APP_NAME_8, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_8, value) }

    var appPackage1: String
        get() = prefs.getString(APP_PACKAGE_1, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_1, value) }

    var appPackage2: String
        get() = prefs.getString(APP_PACKAGE_2, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_2, value) }

    var appPackage3: String
        get() = prefs.getString(APP_PACKAGE_3, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_3, value) }

    var appPackage4: String
        get() = prefs.getString(APP_PACKAGE_4, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_4, value) }

    var appPackage5: String
        get() = prefs.getString(APP_PACKAGE_5, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_5, value) }

    var appPackage6: String
        get() = prefs.getString(APP_PACKAGE_6, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_6, value) }

    var appPackage7: String
        get() = prefs.getString(APP_PACKAGE_7, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_7, value) }

    var appPackage8: String
        get() = prefs.getString(APP_PACKAGE_8, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_8, value) }

    var appActivityClassName1: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_1, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_1, value) }

    var appActivityClassName2: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_2, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_2, value) }

    var appActivityClassName3: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_3, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_3, value) }

    var appActivityClassName4: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_4, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_4, value) }

    var appActivityClassName5: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_5, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_5, value) }

    var appActivityClassName6: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_6, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_6, value) }

    var appActivityClassName7: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_7, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_7, value) }

    var appActivityClassName8: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_8, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_8, value) }

    var appUser1: String
        get() = prefs.getString(APP_USER_1, "").toString()
        set(value) = prefs.edit { putString(APP_USER_1, value) }

    var appUser2: String
        get() = prefs.getString(APP_USER_2, "").toString()
        set(value) = prefs.edit { putString(APP_USER_2, value) }

    var appUser3: String
        get() = prefs.getString(APP_USER_3, "").toString()
        set(value) = prefs.edit { putString(APP_USER_3, value) }

    var appUser4: String
        get() = prefs.getString(APP_USER_4, "").toString()
        set(value) = prefs.edit { putString(APP_USER_4, value) }

    var appUser5: String
        get() = prefs.getString(APP_USER_5, "").toString()
        set(value) = prefs.edit { putString(APP_USER_5, value) }

    var appUser6: String
        get() = prefs.getString(APP_USER_6, "").toString()
        set(value) = prefs.edit { putString(APP_USER_6, value) }

    var appUser7: String
        get() = prefs.getString(APP_USER_7, "").toString()
        set(value) = prefs.edit { putString(APP_USER_7, value) }

    var appUser8: String
        get() = prefs.getString(APP_USER_8, "").toString()
        set(value) = prefs.edit { putString(APP_USER_8, value) }

    var appNameSwipeLeft: String
        get() = prefs.getString(APP_NAME_SWIPE_LEFT, "Camera").toString()
        set(value) = prefs.edit { putString(APP_NAME_SWIPE_LEFT, value) }

    var appNameSwipeRight: String
        get() = prefs.getString(APP_NAME_SWIPE_RIGHT, "Phone").toString()
        set(value) = prefs.edit { putString(APP_NAME_SWIPE_RIGHT, value) }

    var appPackageSwipeLeft: String
        get() = prefs.getString(APP_PACKAGE_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_SWIPE_LEFT, value) }

    var appActivityClassNameSwipeLeft: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT, value) }

    var appPackageSwipeRight: String
        get() = prefs.getString(APP_PACKAGE_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_SWIPE_RIGHT, value) }

    var appActivityClassNameRight: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT, value) }

    var appUserSwipeLeft: String
        get() = prefs.getString(APP_USER_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_USER_SWIPE_LEFT, value) }

    var appUserSwipeRight: String
        get() = prefs.getString(APP_USER_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_USER_SWIPE_RIGHT, value) }

    var clockAppPackage: String
        get() = prefs.getString(CLOCK_APP_PACKAGE, "").toString()
        set(value) = prefs.edit { putString(CLOCK_APP_PACKAGE, value) }

    var clockAppUser: String
        get() = prefs.getString(CLOCK_APP_USER, "").toString()
        set(value) = prefs.edit { putString(CLOCK_APP_USER, value) }

    var clockAppClassName: String?
        get() = prefs.getString(CLOCK_APP_CLASS_NAME, "").toString()
        set(value) = prefs.edit { putString(CLOCK_APP_CLASS_NAME, value) }

    var calendarAppPackage: String
        get() = prefs.getString(CALENDAR_APP_PACKAGE, "").toString()
        set(value) = prefs.edit { putString(CALENDAR_APP_PACKAGE, value) }

    var calendarAppUser: String
        get() = prefs.getString(CALENDAR_APP_USER, "").toString()
        set(value) = prefs.edit { putString(CALENDAR_APP_USER, value) }

    var calendarAppClassName: String?
        get() = prefs.getString(CALENDAR_APP_CLASS_NAME, "").toString()
        set(value) = prefs.edit { putString(CALENDAR_APP_CLASS_NAME, value) }

    var screenTimeAppPackage: String
        get() = prefs.getString(SCREEN_TIME_APP_PACKAGE, "").toString()
        set(value) = prefs.edit { putString(SCREEN_TIME_APP_PACKAGE, value) }

    var screenTimeAppUser: String
        get() = prefs.getString(SCREEN_TIME_APP_USER, "").toString()
        set(value) = prefs.edit { putString(SCREEN_TIME_APP_USER, value) }

    var screenTimeAppClassName: String?
        get() = prefs.getString(SCREEN_TIME_APP_CLASS_NAME, "").toString()
        set(value) = prefs.edit { putString(SCREEN_TIME_APP_CLASS_NAME, value) }

    var isShortcut1: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_1, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_1, value) }

    var shortcutId1: String
        get() = prefs.getString(SHORTCUT_ID_1, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_1, value) }

    var isShortcut2: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_2, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_2, value) }

    var shortcutId2: String
        get() = prefs.getString(SHORTCUT_ID_2, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_2, value) }

    var isShortcut3: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_3, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_3, value) }

    var shortcutId3: String
        get() = prefs.getString(SHORTCUT_ID_3, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_3, value) }

    var isShortcut4: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_4, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_4, value) }

    var shortcutId4: String
        get() = prefs.getString(SHORTCUT_ID_4, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_4, value) }

    var isShortcut5: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_5, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_5, value) }

    var shortcutId5: String
        get() = prefs.getString(SHORTCUT_ID_5, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_5, value) }

    var isShortcut6: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_6, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_6, value) }

    var shortcutId6: String
        get() = prefs.getString(SHORTCUT_ID_6, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_6, value) }

    var isShortcut7: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_7, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_7, value) }

    var shortcutId7: String
        get() = prefs.getString(SHORTCUT_ID_7, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_7, value) }

    var isShortcut8: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_8, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_8, value) }

    var shortcutId8: String
        get() = prefs.getString(SHORTCUT_ID_8, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_8, value) }

    var shortcutIdSwipeLeft: String
        get() = prefs.getString(SHORTCUT_ID_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_SWIPE_LEFT, value) }

    var isShortcutSwipeLeft: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_SWIPE_LEFT, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_SWIPE_LEFT, value) }

    var shortcutIdSwipeRight: String
        get() = prefs.getString(SHORTCUT_ID_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_SWIPE_RIGHT, value) }

    var isShortcutSwipeRight: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_SWIPE_RIGHT, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_SWIPE_RIGHT, value) }

    fun getAppName(location: Int): String {
        return when (location) {
            1 -> prefs.getString(APP_NAME_1, "").toString()
            2 -> prefs.getString(APP_NAME_2, "").toString()
            3 -> prefs.getString(APP_NAME_3, "").toString()
            4 -> prefs.getString(APP_NAME_4, "").toString()
            5 -> prefs.getString(APP_NAME_5, "").toString()
            6 -> prefs.getString(APP_NAME_6, "").toString()
            7 -> prefs.getString(APP_NAME_7, "").toString()
            8 -> prefs.getString(APP_NAME_8, "").toString()
            else -> ""
        }
    }

    fun getAppPackage(location: Int): String {
        return when (location) {
            1 -> prefs.getString(APP_PACKAGE_1, "").toString()
            2 -> prefs.getString(APP_PACKAGE_2, "").toString()
            3 -> prefs.getString(APP_PACKAGE_3, "").toString()
            4 -> prefs.getString(APP_PACKAGE_4, "").toString()
            5 -> prefs.getString(APP_PACKAGE_5, "").toString()
            6 -> prefs.getString(APP_PACKAGE_6, "").toString()
            7 -> prefs.getString(APP_PACKAGE_7, "").toString()
            8 -> prefs.getString(APP_PACKAGE_8, "").toString()
            else -> ""
        }
    }

    fun getAppActivityClassName(location: Int): String {
        return when (location) {
            1 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_1, "").toString()
            2 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_2, "").toString()
            3 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_3, "").toString()
            4 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_4, "").toString()
            5 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_5, "").toString()
            6 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_6, "").toString()
            7 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_7, "").toString()
            8 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_8, "").toString()
            else -> ""
        }
    }

    fun getAppUser(location: Int): String {
        return when (location) {
            1 -> prefs.getString(APP_USER_1, "").toString()
            2 -> prefs.getString(APP_USER_2, "").toString()
            3 -> prefs.getString(APP_USER_3, "").toString()
            4 -> prefs.getString(APP_USER_4, "").toString()
            5 -> prefs.getString(APP_USER_5, "").toString()
            6 -> prefs.getString(APP_USER_6, "").toString()
            7 -> prefs.getString(APP_USER_7, "").toString()
            8 -> prefs.getString(APP_USER_8, "").toString()
            else -> ""
        }
    }

    fun getShortcutId(location: Int): String {
        return when (location) {
            1 -> shortcutId1
            2 -> shortcutId2
            3 -> shortcutId3
            4 -> shortcutId4
            5 -> shortcutId5
            6 -> shortcutId6
            7 -> shortcutId7
            8 -> shortcutId8
            else -> ""
        }
    }

    fun getIsShortcut(location: Int): Boolean {
        return when (location) {
            1 -> isShortcut1
            2 -> isShortcut2
            3 -> isShortcut3
            4 -> isShortcut4
            5 -> isShortcut5
            6 -> isShortcut6
            7 -> isShortcut7
            8 -> isShortcut8
            else -> false
        }
    }

    fun setAppActivityClassName(location: Int, activityClassName: String) {
        when (location) {
            1 -> appActivityClassName1 = activityClassName
            2 -> appActivityClassName2 = activityClassName
            3 -> appActivityClassName3 = activityClassName
            4 -> appActivityClassName4 = activityClassName
            5 -> appActivityClassName5 = activityClassName
            6 -> appActivityClassName6 = activityClassName
            7 -> appActivityClassName7 = activityClassName
            8 -> appActivityClassName8 = activityClassName
        }
    }

    fun updateAppActivityClassName(packageName: String, activityClassName: String) {
        for (i in 1..8) {
            if (getAppPackage(i) == packageName) setAppActivityClassName(i, activityClassName)
        }
        if (clockAppPackage == packageName) clockAppClassName = activityClassName
        if (calendarAppPackage == packageName) calendarAppClassName = activityClassName
        if (screenTimeAppPackage == packageName) screenTimeAppClassName = activityClassName
        if (appPackageSwipeLeft == packageName) appActivityClassNameSwipeLeft = activityClassName
        if (appPackageSwipeRight == packageName) appActivityClassNameRight = activityClassName
    }

    fun getAppRenameLabel(appPackage: String, user: UserHandle? = null): String {
        val userSuffix = user?.toString()?.let { "|$it" } ?: ""
        val renamed = prefs.getString(appPackage + userSuffix, "").toString()
        if (renamed.isBlank() && userSuffix.isNotEmpty()) {
            return prefs.getString(appPackage, "").toString()
        }
        return renamed
    }

    fun setAppRenameLabel(appPackage: String, renameLabel: String, user: UserHandle? = null) = prefs.edit {
        val userSuffix = user?.toString()?.let { "|$it" } ?: ""
        putString(appPackage + userSuffix, renameLabel)
    }

    fun clearHomeAppSlot(location: Int) {
        val nameKey = when (location) {
            1 -> APP_NAME_1
            2 -> APP_NAME_2
            3 -> APP_NAME_3
            4 -> APP_NAME_4
            5 -> APP_NAME_5
            6 -> APP_NAME_6
            7 -> APP_NAME_7
            8 -> APP_NAME_8
            else -> return
        }
        val packageKey = when (location) {
            1 -> APP_PACKAGE_1
            2 -> APP_PACKAGE_2
            3 -> APP_PACKAGE_3
            4 -> APP_PACKAGE_4
            5 -> APP_PACKAGE_5
            6 -> APP_PACKAGE_6
            7 -> APP_PACKAGE_7
            8 -> APP_PACKAGE_8
            else -> return
        }
        prefs.edit {
            putString(nameKey, "")
            putString(packageKey, "")
        }
    }

    var weatherMode: Int
        get() = prefs.getInt(WEATHER_MODE, Constants.WeatherMode.CELSIUS)
        set(value) = prefs.edit { putInt(WEATHER_MODE, value) }

    var cachedWeatherText: String
        get() = prefs.getString(CACHED_WEATHER_TEXT, "") ?: ""
        set(value) = prefs.edit { putString(CACHED_WEATHER_TEXT, value) }

    var weatherLastUpdated: Long
        get() = prefs.getLong(WEATHER_LAST_UPDATED, 0L)
        set(value) = prefs.edit { putLong(WEATHER_LAST_UPDATED, value) }

    var lastWeatherLat: Float
        get() = prefs.getFloat(LAST_WEATHER_LAT, Float.NaN)
        set(value) = prefs.edit { putFloat(LAST_WEATHER_LAT, value) }

    var lastWeatherLon: Float
        get() = prefs.getFloat(LAST_WEATHER_LON, Float.NaN)
        set(value) = prefs.edit { putFloat(LAST_WEATHER_LON, value) }

    var fontFamily: String
        get() = prefs.getString(FONT_FAMILY, Constants.FontFamily.LIGHT) ?: Constants.FontFamily.LIGHT
        set(value) = prefs.edit { putString(FONT_FAMILY, value) }

    var lockHiddenApps: Boolean
        get() = prefs.getBoolean(LOCK_HIDDEN_APPS, false)
        set(value) = prefs.edit { putBoolean(LOCK_HIDDEN_APPS, value) }

    var mindfulApps: MutableSet<String>
        get() = (prefs.getStringSet(MINDFUL_APPS, emptySet()) ?: emptySet()).toMutableSet()
        set(value) = prefs.edit { putStringSet(MINDFUL_APPS, value.toSet()) }

    fun exportToJson(): String {
        val transientKeys = setOf(
            LAUNCHER_RESTART_TIMESTAMP,
            SCREEN_TIME_LAST_UPDATED,
            WEATHER_LAST_UPDATED
        )
        val booleans = JSONObject()
        val ints = JSONObject()
        val longs = JSONObject()
        val floats = JSONObject()
        val strings = JSONObject()
        val stringSets = JSONObject()

        for ((key, value) in prefs.all) {
            if (key in transientKeys) continue
            when (value) {
                is Boolean -> booleans.put(key, value)
                is Int -> ints.put(key, value)
                is Long -> longs.put(key, value)
                is Float -> if (!value.isNaN() && !value.isInfinite()) floats.put(key, value.toDouble())
                is String -> strings.put(key, value)
                is Set<*> -> {
                    val arr = JSONArray()
                    value.filterIsInstance<String>().forEach { arr.put(it) }
                    stringSets.put(key, arr)
                }
            }
        }

        return JSONObject().apply {
            put("schema", "litelauncher_backup_v1")
            put("exportedAt", System.currentTimeMillis())
            put("booleans", booleans)
            put("ints", ints)
            put("longs", longs)
            put("floats", floats)
            put("strings", strings)
            put("stringSets", stringSets)
        }.toString(2)
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (root.optString("schema") != "litelauncher_backup_v1") return false

            prefs.edit {
                clear()
                root.optJSONObject("booleans")?.let { obj ->
                    obj.keys().forEach { key -> putBoolean(key, obj.getBoolean(key)) }
                }
                root.optJSONObject("ints")?.let { obj ->
                    obj.keys().forEach { key -> putInt(key, obj.getInt(key)) }
                }
                root.optJSONObject("longs")?.let { obj ->
                    obj.keys().forEach { key -> putLong(key, obj.getLong(key)) }
                }
                root.optJSONObject("floats")?.let { obj ->
                    obj.keys().forEach { key -> putFloat(key, obj.getDouble(key).toFloat()) }
                }
                root.optJSONObject("strings")?.let { obj ->
                    obj.keys().forEach { key -> putString(key, obj.getString(key)) }
                }
                root.optJSONObject("stringSets")?.let { obj ->
                    obj.keys().forEach { key ->
                        val arr = obj.getJSONArray(key)
                        val set = mutableSetOf<String>()
                        for (i in 0 until arr.length()) {
                            set.add(arr.getString(i))
                        }
                        putStringSet(key, set)
                    }
                }
                putBoolean(FIRST_OPEN, false)
                putBoolean(FIRST_SETTINGS_OPEN, false)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}