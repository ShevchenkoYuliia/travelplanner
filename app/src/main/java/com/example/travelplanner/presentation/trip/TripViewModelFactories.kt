package com.example.travelplanner.presentation.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelplanner.data.realtime.SocketManager
import com.example.travelplanner.domain.usecase.trip.TripUseCases
import com.example.travelplanner.presentation.trip.detail.TripDetailViewModel
import com.example.travelplanner.presentation.trip.list.TripListViewModel

class TripListViewModelFactory(
    private val useCases: TripUseCases,
    private val socketManager: SocketManager,
    private val filter: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TripListViewModel(useCases, socketManager, filter) as T
}

class TripDetailViewModelFactory(
    private val useCases: TripUseCases,
    private val tripId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TripDetailViewModel(useCases, tripId) as T
}
