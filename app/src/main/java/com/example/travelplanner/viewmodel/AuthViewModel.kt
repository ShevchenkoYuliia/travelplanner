package com.example.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelplanner.data.UserRepository
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> = userRepository.isLoggedIn()
    val currentUserEmail: StateFlow<String?> = userRepository.currentUserEmail()

    fun login(email: String, password: String): Result<Unit> =
        userRepository.login(email, password)

    fun register(email: String, password: String): Result<Unit> =
        userRepository.register(email, password)

    fun hasRegisteredAccounts(): Boolean =
        userRepository.hasRegisteredAccounts()

    fun unlockWithBiometrics(): Result<Unit> =
        userRepository.unlockWithBiometrics()

    fun logout() = userRepository.logout()
}

class AuthViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(userRepository) as T
    }
}
