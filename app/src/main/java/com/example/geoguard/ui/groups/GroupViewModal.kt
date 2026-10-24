package com.example.geoguard.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geoguard.data.model.Group
import com.example.geoguard.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isJoining: Boolean = false,
    val joinError: String? = null,
    val joinedGroupName: String? = null
)

class GroupsViewModel(
    private val repository: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeMyGroups()
                .catch { e ->
                    _uiState.update { state -> state.copy(isLoading = false, loadError = e.message) }
                }
                .collect { groups ->
                    _uiState.update { state ->
                        state.copy(groups = groups, isLoading = false, loadError = null)
                    }
                }
        }
    }

    fun joinGroup(code: String) {
        if (_uiState.value.isJoining) return
        _uiState.update { state -> state.copy(isJoining = true, joinError = null) }

        viewModelScope.launch {
            repository.joinGroup(code)
                .onSuccess { group ->
                    _uiState.update { state ->
                        state.copy(isJoining = false, joinedGroupName = group.name)
                    }
                }
                .onFailure { e ->
                    _uiState.update { state -> state.copy(isJoining = false, joinError = e.message) }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { state -> state.copy(joinError = null, joinedGroupName = null) }
    }
}