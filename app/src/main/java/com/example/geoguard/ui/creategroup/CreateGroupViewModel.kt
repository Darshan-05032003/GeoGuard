package com.example.geoguard.ui.creategroup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geoguard.data.location.LocationRepository
import com.example.geoguard.data.model.SafeZone
import com.example.geoguard.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MIN_RADIUS_METERS = 150f
const val MAX_RADIUS_METERS = 1000f
const val DEFAULT_RADIUS_METERS = 200f

data class CreateGroupUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float = DEFAULT_RADIUS_METERS,
    val isLocating: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val createdGroupId: String? = null
)

class CreateGroupViewModel(application: Application) : AndroidViewModel(application) {

    private val groupRepository = GroupRepository()
    private val locationRepository = LocationRepository(application)

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    fun onRadiusChange(radius: Float) {
        _uiState.update { state -> state.copy(radiusMeters = radius) }
    }

    fun onPermissionDenied() {
        _uiState.update { state ->
            state.copy(errorMessage = "Precise location permission is needed to set the zone centre")
        }
    }

    fun fetchCurrentLocation() {
        _uiState.update { state -> state.copy(isLocating = true, errorMessage = null) }
        viewModelScope.launch {
            locationRepository.getCurrentLocation()
                .onSuccess { location ->
                    _uiState.update { state ->
                        state.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isLocating = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { state -> state.copy(isLocating = false, errorMessage = e.message) }
                }
        }
    }

    fun createGroup(name: String) {
        val state = _uiState.value
        if (state.isSaving) return

        val error = CreateGroupValidator.validate(name, state.latitude, state.longitude, state.radiusMeters)
        val lat = state.latitude
        val lng = state.longitude
        if (error != null || lat == null || lng == null) {
            _uiState.update { it.copy(errorMessage = error ?: CreateGroupValidator.ERROR_NO_LOCATION) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val zone = SafeZone(lat = lat, lng = lng, radiusMeters = state.radiusMeters.toDouble())
            groupRepository.createGroup(name, zone)
                .onSuccess { group ->
                    _uiState.update { it.copy(isSaving = false, createdGroupId = group.id) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
                }
        }
    }
}