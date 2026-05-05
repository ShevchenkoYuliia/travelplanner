package com.example.travelplanner.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockBiometricManager(
    private val preferences: SecurityPreferences = InMemorySecurityPreferences(),
    private var availability: BiometricAvailability = BiometricAvailability(
        isSupported = true,
        type = BiometricType.Fingerprint
    ),
    private var nextResult: BiometricAuthResult = BiometricAuthResult.Success
) : BiometricManager {
    private val _state = MutableStateFlow<BiometricAuthState>(BiometricAuthState.Idle)
    override val state: StateFlow<BiometricAuthState> = _state

    override fun checkAvailability(): BiometricAvailability = availability

    override suspend fun authenticate(reason: String): BiometricAuthResult {
        _state.value = BiometricAuthState.Authenticating
        val result = if (availability.isSupported) {
            nextResult
        } else {
            BiometricAuthResult.Unavailable(availability.message ?: "Біометрія недоступна")
        }
        _state.value = result.toState()
        return result
    }

    override fun isEnabledByUser(): Boolean = preferences.isBiometricEnabled()

    override fun setEnabledByUser(enabled: Boolean) {
        preferences.setBiometricEnabled(enabled)
    }

    fun setAvailability(availability: BiometricAvailability) {
        this.availability = availability
    }

    fun setNextResult(result: BiometricAuthResult) {
        nextResult = result
    }
}

internal fun BiometricAuthResult.toState(): BiometricAuthState =
    when (this) {
        BiometricAuthResult.Success -> BiometricAuthState.Success
        BiometricAuthResult.UserCancelled -> BiometricAuthState.Failed("Автентифікацію скасовано")
        is BiometricAuthResult.Failed -> BiometricAuthState.Failed(message)
        is BiometricAuthResult.SystemError -> BiometricAuthState.Failed(message)
        is BiometricAuthResult.Unavailable -> BiometricAuthState.Unavailable(message)
    }
