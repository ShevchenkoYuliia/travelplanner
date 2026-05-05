package com.example.travelplanner.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SecurityUiState(
    val availability: BiometricAvailability = BiometricAvailability(false, BiometricType.None),
    val authState: BiometricAuthState = BiometricAuthState.Idle,
    val biometricEnabled: Boolean = false,
    val lockAfterSeconds: Int = 60,
    val message: String? = null
)

class SecurityViewModel(
    private val biometricManager: BiometricManager,
    private val preferences: SecurityPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SecurityUiState(
            availability = biometricManager.checkAvailability(),
            authState = biometricManager.state.value,
            biometricEnabled = biometricManager.isEnabledByUser(),
            lockAfterSeconds = preferences.lockAfterSeconds()
        )
    )
    val uiState: StateFlow<SecurityUiState> = _uiState

    init {
        viewModelScope.launch {
            biometricManager.state.collect { authState ->
                _uiState.value = _uiState.value.copy(authState = authState)
            }
        }
    }

    fun refreshAvailability() {
        _uiState.value = _uiState.value.copy(
            availability = biometricManager.checkAvailability(),
            biometricEnabled = biometricManager.isEnabledByUser(),
            lockAfterSeconds = preferences.lockAfterSeconds()
        )
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val availability = biometricManager.checkAvailability()
        if (enabled && !availability.isSupported) {
            _uiState.value = _uiState.value.copy(
                availability = availability,
                biometricEnabled = false,
                authState = BiometricAuthState.Unavailable(availability.message ?: "Біометрія недоступна"),
                message = availability.message ?: "Біометрія недоступна"
            )
            return
        }
        biometricManager.setEnabledByUser(enabled)
        _uiState.value = _uiState.value.copy(
            availability = availability,
            biometricEnabled = enabled,
            message = if (enabled) "Біометрію увімкнено" else "Біометрію вимкнено"
        )
    }

    fun setLockAfterSeconds(seconds: Int) {
        preferences.setLockAfterSeconds(seconds)
        _uiState.value = _uiState.value.copy(lockAfterSeconds = preferences.lockAfterSeconds())
    }

    suspend fun authenticate(reason: String): BiometricAuthResult {
        if (!biometricManager.isEnabledByUser()) {
            val result = BiometricAuthResult.Unavailable("Біометрія вимкнена в налаштуваннях")
            _uiState.value = _uiState.value.copy(
                authState = result.toState(),
                message = result.message
            )
            return result
        }
        val result = biometricManager.authenticate(reason)
        _uiState.value = _uiState.value.copy(
            authState = result.toState(),
            message = result.userMessageOrNull()
        )
        return result
    }

    fun authenticateAsync(reason: String, onResult: (BiometricAuthResult) -> Unit = {}) {
        viewModelScope.launch {
            onResult(authenticate(reason))
        }
    }
}

private fun BiometricAuthResult.userMessageOrNull(): String? =
    when (this) {
        BiometricAuthResult.Success -> null
        BiometricAuthResult.UserCancelled -> "Автентифікацію скасовано"
        is BiometricAuthResult.Failed -> message
        is BiometricAuthResult.SystemError -> message
        is BiometricAuthResult.Unavailable -> message
    }

class SecurityViewModelFactory(
    private val biometricManager: BiometricManager,
    private val preferences: SecurityPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SecurityViewModel(biometricManager, preferences) as T
}
