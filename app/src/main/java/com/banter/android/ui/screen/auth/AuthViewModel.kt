package com.banter.android.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banter.android.data.local.SessionStore
import com.banter.android.data.repository.AuthRepository
import com.banter.android.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _startRoute = MutableStateFlow<String?>(null)
    val startRoute: StateFlow<String?> = _startRoute

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        viewModelScope.launch {
            val token = sessionStore.token.first()
            val serverUrl = sessionStore.serverUrl.first()
            _startRoute.value = when {
                serverUrl == null -> Screen.ServerSetup.route
                token == null -> Screen.Login.route
                else -> Screen.Main.route
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.login(username, password)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, success = true) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.register(username, password)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, success = true) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun clearState() {
        _uiState.value = AuthUiState()
    }

    fun saveServerUrl(url: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val normalized = url.trim().trimEnd('/')
            sessionStore.saveServerUrl(normalized.ifEmpty { "http://127.0.0.1:3970" })
            onDone?.invoke()
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)
