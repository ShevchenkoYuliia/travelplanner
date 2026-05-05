package com.example.travelplanner.security

import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.biometric.BiometricManager as AndroidxBiometricManager

class AndroidBiometricManager(
    private val activity: FragmentActivity?,
    context: Context,
    private val preferences: SecurityPreferences
) : BiometricManager {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow<BiometricAuthState>(BiometricAuthState.Idle)
    override val state: StateFlow<BiometricAuthState> = _state

    override fun checkAvailability(): BiometricAvailability {
        val manager = AndroidxBiometricManager.from(appContext)
        return when (manager.canAuthenticate(BIOMETRIC_STRONG)) {
            AndroidxBiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricAvailability(true, detectBiometricType())
            }
            AndroidxBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricAvailability(false, BiometricType.None, "Біометрію не налаштовано на пристрої")
            }
            AndroidxBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricAvailability(false, BiometricType.None, "На пристрої немає біометричного датчика")
            }
            AndroidxBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricAvailability(false, BiometricType.None, "Біометричний датчик тимчасово недоступний")
            }
            else -> BiometricAvailability(false, BiometricType.None, "Біометрія недоступна")
        }
    }

    override suspend fun authenticate(reason: String): BiometricAuthResult {
        val availability = checkAvailability()
        if (!availability.isSupported) {
            val result = BiometricAuthResult.Unavailable(availability.message ?: "Біометрія недоступна")
            _state.value = result.toState()
            return result
        }
        val hostActivity = activity
        if (hostActivity == null) {
            val result = BiometricAuthResult.Unavailable("Екран не готовий до біометричної перевірки")
            _state.value = result.toState()
            return result
        }

        _state.value = BiometricAuthState.Authenticating
        return suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(
                hostActivity,
                ContextCompat.getMainExecutor(hostActivity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) {
                            _state.value = BiometricAuthState.Success
                            continuation.resume(BiometricAuthResult.Success)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (!continuation.isActive) return
                        val result = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED -> BiometricAuthResult.UserCancelled
                            BiometricPrompt.ERROR_HW_UNAVAILABLE,
                            BiometricPrompt.ERROR_NO_BIOMETRICS,
                            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                            BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                                BiometricAuthResult.Unavailable(errString.toString())
                            }
                            else -> BiometricAuthResult.SystemError(errString.toString())
                        }
                        _state.value = result.toState()
                        continuation.resume(result)
                    }

                    override fun onAuthenticationFailed() {
                        _state.value = BiometricAuthState.Failed("Біометричні дані не розпізнано")
                    }
                }
            )
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(
                PromptInfo.Builder()
                    .setTitle("Біометрична автентифікація")
                    .setSubtitle(reason)
                    .setNegativeButtonText("Скасувати")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
                    .build()
            )
        }
    }

    override fun isEnabledByUser(): Boolean = preferences.isBiometricEnabled()

    override fun setEnabledByUser(enabled: Boolean) {
        preferences.setBiometricEnabled(enabled)
    }

    private fun detectBiometricType(): BiometricType {
        val packageManager = appContext.packageManager
        return when {
            packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) -> BiometricType.Fingerprint
            packageManager.hasSystemFeature("android.hardware.biometrics.face") -> BiometricType.FaceId
            else -> BiometricType.Biometric
        }
    }
}
