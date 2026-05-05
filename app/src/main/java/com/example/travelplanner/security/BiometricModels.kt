package com.example.travelplanner.security

import kotlinx.coroutines.flow.StateFlow

enum class BiometricType(val displayName: String) {
    FaceId("Face ID"),
    TouchId("Touch ID"),
    Fingerprint("Fingerprint"),
    Biometric("Biometric"),
    None("Відсутній")
}

data class BiometricAvailability(
    val isSupported: Boolean,
    val type: BiometricType,
    val message: String? = null
)

sealed class BiometricAuthResult {
    data object Success : BiometricAuthResult()
    data object UserCancelled : BiometricAuthResult()
    data class Failed(val message: String) : BiometricAuthResult()
    data class SystemError(val message: String) : BiometricAuthResult()
    data class Unavailable(val message: String) : BiometricAuthResult()
}

sealed class BiometricAuthState {
    data object Idle : BiometricAuthState()
    data object Authenticating : BiometricAuthState()
    data object Success : BiometricAuthState()
    data class Failed(val message: String) : BiometricAuthState()
    data class Unavailable(val message: String) : BiometricAuthState()
}

interface BiometricManager {
    val state: StateFlow<BiometricAuthState>

    fun checkAvailability(): BiometricAvailability

    suspend fun authenticate(reason: String): BiometricAuthResult

    fun isEnabledByUser(): Boolean

    fun setEnabledByUser(enabled: Boolean)
}
