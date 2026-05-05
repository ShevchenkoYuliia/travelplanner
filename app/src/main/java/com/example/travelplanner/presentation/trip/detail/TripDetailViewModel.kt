package com.example.travelplanner.presentation.trip.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelplanner.domain.model.RoutePoint
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.domain.result.SyncWriteResult
import com.example.travelplanner.domain.usecase.trip.TripUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripDetailViewModel(
    private val useCases: TripUseCases,
    private val tripId: String
) : ViewModel() {

    val trip: StateFlow<Trip?> = useCases
        .observeTripById(tripId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val routePoints: StateFlow<List<RoutePoint>> = useCases
        .observeRoutePoints(tripId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    suspend fun addRoutePointAndAwait(point: RoutePoint): SyncWriteResult =
        useCases.saveRoutePoint(point)

    fun deleteRoutePoint(point: RoutePoint) {
        viewModelScope.launch { useCases.deleteRoutePoint(point) }
    }

    private val _isGeneratingInvite = MutableStateFlow(false)
    val isGeneratingInvite: StateFlow<Boolean> = _isGeneratingInvite

    fun createInviteLink(
        onLinkGenerated: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isGeneratingInvite.value = true
            try {
                val token = useCases.createInviteToken(tripId)
                val link = "myapp://invite/$token"
                onLinkGenerated(link)
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackToken = java.util.UUID.randomUUID().toString()
                val link = "myapp://invite/$fallbackToken"
                onLinkGenerated(link)
                onError("Бекенд недоступний. Згенеровано локальний токен.")
            } finally {
                _isGeneratingInvite.value = false
            }
        }
    }
}
