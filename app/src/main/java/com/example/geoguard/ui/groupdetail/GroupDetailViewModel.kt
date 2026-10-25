package com.example.geoguard.ui.groupdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geoguard.data.model.Group
import com.example.geoguard.data.model.ZoneEvent
import com.example.geoguard.data.repository.EventRepository
import com.example.geoguard.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val group: Group? = null,
    val members: List<MemberStatus> = emptyList(),
    val events: List<ZoneEvent> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/** groupId comes from the navigation route "group_detail/{groupId}" via SavedStateHandle. */
class GroupDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle.get<String>("groupId"))
    private val groupRepository = GroupRepository()
    private val eventRepository = EventRepository()

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Two live streams merged into one screen state: the group doc and its latest events.
            combine(
                groupRepository.observeGroup(groupId),
                eventRepository.observeRecentEvents(groupId)
            ) { group, events ->
                GroupDetailUiState(
                    group = group,
                    members = group?.let {
                        MemberStatusCalculator.calculate(it.memberIds, it.memberNames, events)
                    }.orEmpty(),
                    events = events,
                    isLoading = false,
                    errorMessage = if (group == null) "Group not found" else null
                )
            }
                .catch { e -> emit(GroupDetailUiState(isLoading = false, errorMessage = e.message)) }
                .collect { state -> _uiState.value = state }
        }
    }
}