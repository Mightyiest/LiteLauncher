package app.olauncher.helper

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

fun isAccessServiceEnabled(context: Context): Boolean {
    val enabled = try {
        Settings.Secure.getInt(context.applicationContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
    } catch (e: Exception) {
        0
    }
    if (enabled == 1) {
        val enabledServicesString: String = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expectedComponent = ComponentName(context, MyAccessibilityService::class.java)
        val fullName = expectedComponent.flattenToString()
        val shortName = expectedComponent.flattenToShortString()
        return enabledServicesString.split(':').any { service ->
            service.equals(fullName, ignoreCase = true) || service.equals(shortName, ignoreCase = true)
        }
    }
    return false
}
