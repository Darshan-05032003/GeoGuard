package com.example.geoguard.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geoguard.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.geoguard.data.repository.TokenRepository
import kotlinx.coroutines.withTimeoutOrNull

data class AuthUiState(
    val isLoading : Boolean = false,
    val isLoggedIn : Boolean = false,
    val errorMessage : String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository(),
    private val tokenRepository: TokenRepository = TokenRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isLoggedIn= repository.isLoggedIn()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email:String,password: String){
        if(email.isBlank() || password.isBlank()){
            _uiState.update{ state -> state.copy(errorMessage = "Email and password are required ")}
            return
        }
        _uiState.update{state->state.copy(isLoading= true, errorMessage= null)}

        viewModelScope.launch{
            repository.login(email.trim(), password)
                .onSuccess{
                    _uiState.update{state->state.copy(isLoading=false, isLoggedIn = true)}

                }
                .onFailure { e ->
                    _uiState.update{state->state.copy(isLoading=false, errorMessage= e.message)}
                }

        }
    }

    fun register(name:String, email:String, password: String){
        if(name.isBlank() || email.isBlank() || password.length < 6){
            _uiState.update {
                state->state.copy(errorMessage = "Enter name , email and a password of at least 6 characters ")
            }
            return

        }

        _uiState.update{state->state.copy(isLoading= true, errorMessage= null)}

        viewModelScope.launch{
            repository.register(name.trim(), email.trim(), password)
                .onSuccess{
                    _uiState.update{state->state.copy(isLoading= false, isLoggedIn = true)}
                }
                .onFailure { e ->
                    _uiState.update{ state->state.copy(isLoading= false, errorMessage = e.message)}
                }
        }
    }
    fun logout() {
        viewModelScope.launch {
            // Remove this phone's push token first (needs auth), so the old account stops getting alerts here.
            // Offline, a Firestore write never confirms; don't let that block logout (the delete is still queued).
            withTimeoutOrNull(3_000) { tokenRepository.clearToken() }
            repository.logout()
            _uiState.update { AuthUiState() }
        }
    }

}