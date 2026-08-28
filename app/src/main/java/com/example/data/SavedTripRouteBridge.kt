package com.example.data

import com.example.model.SavedTrip
import com.example.model.TripRoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SavedTripRouteSelection(
    val tripId: String,
    val name: String,
    val points: List<TripRoutePoint>
)

object SavedTripRouteBridge {
    private val _selection = MutableStateFlow<SavedTripRouteSelection?>(null)
    val selection: StateFlow<SavedTripRouteSelection?> = _selection.asStateFlow()

    fun show(trip: SavedTrip) {
        _selection.value = SavedTripRouteSelection(trip.id, trip.name, trip.route)
    }

    fun clear() {
        _selection.value = null
    }
}
