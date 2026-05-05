package com.example.travelplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travelplanner.data.UserRepository
import com.example.travelplanner.domain.result.ProfileSaveResult
import com.example.travelplanner.models.UserProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = userRepository
        .getUserProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = userRepository.getCachedUserProfile()
        )

    private val _profileSaveResult = MutableSharedFlow<ProfileSaveResult>(extraBufferCapacity = 1)
    val profileSaveResult: SharedFlow<ProfileSaveResult> = _profileSaveResult.asSharedFlow()

    fun updateProfile(
        displayName: String,
        email: String,
        homeCity: String,
        preferredCurrency: String
    ) {
        viewModelScope.launch {
            val result = userRepository.updateProfile(
                displayName = displayName,
                email = email,
                homeCity = homeCity,
                preferredCurrency = preferredCurrency
            )
            _profileSaveResult.emit(result)
        }
    }
}

class ProfileViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(userRepository) as T
    }
}
