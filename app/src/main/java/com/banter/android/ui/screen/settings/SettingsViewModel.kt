package com.banter.android.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banter.android.data.local.SessionStore
import com.banter.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userId: Long = 0,
    val username: String = "",
    val displayName: String = "",
    val serverUrl: String = "",
    val cameraFrontFacing: Boolean = true,
    val mirrorCamera: Boolean = true,
    val appVersion: String = "1.0.0-alpha.5",
    val savedMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userId = sessionStore.userId.first() ?: 0L,
                    username = sessionStore.username.first().orEmpty(),
                    displayName = sessionStore.displayName.first()
                        ?: sessionStore.username.first().orEmpty(),
                    serverUrl = sessionStore.serverUrl.first() ?: "http://127.0.0.1:3970",
                    cameraFrontFacing = sessionStore.cameraFrontFacing.first(),
                    mirrorCamera = sessionStore.mirrorCamera.first()
                )
            }
        }
        viewModelScope.launch {
            sessionStore.cameraFrontFacing.collect { front ->
                _uiState.update { it.copy(cameraFrontFacing = front) }
            }
        }
        viewModelScope.launch {
            sessionStore.mirrorCamera.collect { mirror ->
                _uiState.update { it.copy(mirrorCamera = mirror) }
            }
        }
    }

    fun saveServerUrl(url: String) {
        viewModelScope.launch {
            val normalized = url.trim().trimEnd('/').ifEmpty { "http://127.0.0.1:3970" }
            sessionStore.saveServerUrl(normalized)
            _uiState.update {
                it.copy(serverUrl = normalized, savedMessage = "Server URL saved")
            }
        }
    }

    fun setCameraFrontFacing(front: Boolean) {
        viewModelScope.launch { sessionStore.setCameraFrontFacing(front) }
    }

    fun setMirrorCamera(mirror: Boolean) {
        viewModelScope.launch { sessionStore.setMirrorCamera(mirror) }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
