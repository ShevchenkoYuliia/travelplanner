package com.example.travelplanner.presentation.realtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelplanner.data.realtime.SocketConnectionState
import com.example.travelplanner.data.realtime.SocketEvent
import com.example.travelplanner.data.realtime.SocketManager
import com.example.travelplanner.data.remote.ApiConfig
import kotlinx.coroutines.flow.StateFlow

class RealtimeViewModel(
    private val socketManager: SocketManager
) : ViewModel() {
    val state: StateFlow<SocketConnectionState> = socketManager.state
    val events: StateFlow<List<SocketEvent>> = socketManager.events

    init {
        socketManager.connect(ApiConfig.WS_URL)
    }

    fun sendPing() {
        socketManager.send("""{"type":"client","action":"ping","message":"Ping from app"}""")
    }

    override fun onCleared() {
        socketManager.disconnect()
        super.onCleared()
    }
}

class RealtimeViewModelFactory(
    private val socketManager: SocketManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RealtimeViewModel(socketManager) as T
}
