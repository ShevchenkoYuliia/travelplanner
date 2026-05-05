package com.example.travelplanner.presentation.trip.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelplanner.data.realtime.SocketConnectionState
import com.example.travelplanner.data.realtime.SocketEvent
import com.example.travelplanner.data.realtime.SocketManager
import com.example.travelplanner.data.remote.ApiConfig
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.domain.result.SyncWriteResult
import com.example.travelplanner.domain.usecase.trip.TripUseCases
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TripListViewModel(
    private val useCases: TripUseCases,
    private val socketManager: SocketManager,
    private val filter: String? = null
) : ViewModel() {
    companion object {
        private val sessionCreatedTripIds = mutableSetOf<String>()
    }
    private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val uiMessages: SharedFlow<String> = _uiMessages

    private val _socketEvents = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 16)
    val socketEvents = _socketEvents.asSharedFlow()
    val socketState: StateFlow<SocketConnectionState> = socketManager.state
    private var unsubscribeSocket: (() -> Unit)? = null

    val trips: StateFlow<List<Trip>> = useCases
        .observeTrips()
        .map { allTrips ->
            when (filter) {
                "new" -> allTrips.filter { sessionCreatedTripIds.contains(it.id) }
                "old" -> allTrips.filter { !sessionCreatedTripIds.contains(it.id) }
                else -> allTrips
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        unsubscribeSocket = socketManager.onMessage { event ->
            viewModelScope.launch {
                _socketEvents.emit(event)
                useCases.refreshTrips()
            }
        }
        socketManager.connect(ApiConfig.WS_URL)
        viewModelScope.launch {
            useCases.refreshTrips()
            useCases.syncPendingTrips()
        }
    }

    suspend fun addTripAndAwait(trip: Trip): SyncWriteResult {
        sessionCreatedTripIds.add(trip.id)
        val result = useCases.saveTrip(trip)
        if (result is SyncWriteResult.Queued) {
            _uiMessages.emit("Дані збережено локально. Синхронізація запуститься автоматично, коли з'явиться інтернет.")
        }
        return result
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch { useCases.deleteTrip(trip) }
    }

    fun syncAll() {
        viewModelScope.launch { useCases.syncPendingTrips() }
    }

    private val _invitePreview = MutableStateFlow<com.example.travelplanner.domain.model.InviteTripPreview?>(null)
    val invitePreview: StateFlow<com.example.travelplanner.domain.model.InviteTripPreview?> = _invitePreview

    fun loadInvitePreview(token: String) {
        viewModelScope.launch {
            try {
                _invitePreview.value = useCases.getInvitePreview(token)
            } catch (e: Exception) {
                _uiMessages.emit("Помилка завантаження запрошення")
            }
        }
    }

    suspend fun acceptInvite(token: String): Trip {
        val trip = useCases.acceptInvite(token)
        sessionCreatedTripIds.add(trip.id)
        return trip
    }

    override fun onCleared() {
        unsubscribeSocket?.invoke()
        unsubscribeSocket = null
        socketManager.disconnect()
        super.onCleared()
    }
}
