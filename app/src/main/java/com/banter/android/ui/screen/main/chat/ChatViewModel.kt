package com.banter.android.ui.screen.main.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banter.android.data.model.Message
import com.banter.android.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val channelId: Long = 0,
    val channelName: String = "",
    val isDm: Boolean = false,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    fun loadChannel(id: Long, isDm: Boolean, name: String = "") {
        _uiState.value = ChatUiState(channelId = id, isDm = isDm, channelName = name, isLoading = true)
        viewModelScope.launch {
            val result = if (isDm) {
                messageRepository.dmMessages(id)
            } else {
                messageRepository.channelMessages(id)
            }
            result.onSuccess { msgs ->
                _uiState.update { it.copy(
                    messages = msgs.sortedBy { m -> m.sentAt },
                    isLoading = false
                )}
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun sendMessage(content: String) {
        val state = _uiState.value
        viewModelScope.launch {
            val result = if (state.isDm) {
                messageRepository.sendDmMessage(state.channelId, content)
            } else {
                messageRepository.sendChannelMessage(state.channelId, content)
            }
            result.onSuccess { msg ->
                _uiState.update { it.copy(messages = it.messages + msg) }
            }
        }
    }

    fun onNewMessage(message: Message) {
        val exists = _uiState.value.messages.any { it.id == message.id }
        if (!exists) {
            _uiState.update { it.copy(messages = it.messages + message) }
        }
    }

    fun onMessageEdited(id: Long, newContent: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { msg ->
                if (msg.id == id) msg.copy(content = newContent) else msg
            })
        }
    }

    fun onMessageDeleted(id: Long) {
        _uiState.update { state ->
            state.copy(messages = state.messages.filter { it.id != id })
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMore) return
        val oldest = state.messages.firstOrNull()?.sentAt ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (state.isDm) {
                messageRepository.dmMessages(state.channelId, before = oldest)
            } else {
                messageRepository.channelMessages(state.channelId, before = oldest)
            }
            result.onSuccess { msgs ->
                _uiState.update { it.copy(
                    messages = msgs.sortedBy { m -> m.sentAt } + it.messages,
                    isLoading = false,
                    hasMore = msgs.size >= 50
                )}
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
