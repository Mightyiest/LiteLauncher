package app.olauncher.listener

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import app.olauncher.BuildConfig
import app.olauncher.R
import app.olauncher.data.Prefs

class DeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        if (!BuildConfig.USE_ACCESSIBILITY_LOCK || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Prefs(context).lockModeOn = true
        }
        Toast.makeText(context, context.getString(R.string.enabled), Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        if (!BuildConfig.USE_ACCESSIBILITY_LOCK || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Prefs(context).lockModeOn = false
        }
        Toast.makeText(context, context.getString(R.string.disabled), Toast.LENGTH_SHORT).show()
    }
}