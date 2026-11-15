package app.olauncher.helper

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import app.olauncher.R

object BiometricAuthHelper {

    fun isDeviceSecure(context: Context): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguard?.isDeviceSecure == true
    }

    fun authenticate(
        fragment: Fragment,
        title: String,
        subtitle: String,
        onStart: () -> Unit = {},
        onSuccess: () -> Unit,
        onFinish: () -> Unit = {},
    ) {
        val context = fragment.context ?: return
        if (!isDeviceSecure(context)) {
            context.showToast(context.getString(R.string.set_screen_lock_first))
            onFinish()
            return
        }

        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onFinish()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onFinish()
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED &&
                    errString.isNotBlank()
                ) {
                    fragment.context?.showToast(errString.toString())
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        onStart()
        try {
            BiometricPrompt(fragment, executor, callback).authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            onFinish()
            fragment.context?.showToast(e.localizedMessage ?: "Authentication unavailable")
        }
    }
}
