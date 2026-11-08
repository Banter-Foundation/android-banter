package com.banter.android.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banter.android.data.api.WebSocketManager
import com.banter.android.data.local.SessionStore
import com.banter.android.data.model.*
import com.banter.android.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val currentUserId: Long = 0,
    val currentUsername: String = "",
    val currentDisplayName: String = "",
    val servers: List<ServerRailEntry> = emptyList(),
    val selectedServerId: Long? = null,
    val selectedChannelId: Long? = null,
    val selectedDmId: Long? = null,
    val serverLayout: ServerLayout? = null,
    val members: List<Member> = emptyList(),
    val roles: List<GuildRole> = emptyList(),
    val voiceMembers: List<VoiceMember> = emptyList(),
    val dms: List<Dm> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val friendRequests: List<FriendRequest> = emptyList(),
    val presence: Map<Long, PresenceEntry> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // voice state
    val inVoiceChannelId: Long? = null,
    val isMuted: Boolean = false,
    val isDeafened: Boolean = false,
    val hasCamera: Boolean = false,
    val hasScreen: Boolean = false,
    // navigation
    val activeTab: MainTab = MainTab.HOME
)

enum class MainTab { HOME, SERVERS, DMS, MENTIONS }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val serverRepository: ServerRepository,
    private val messageRepository: MessageRepository,
    private val dmRepository: DmRepository,
    private val friendRepository: FriendRepository,
    private val voiceRepository: VoiceRepository,
    private val authRepository: AuthRepository,
    val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        viewModelScope.launch {
            val userId = sessionStore.userId.first() ?: 0L
            val username = sessionStore.username.first() ?: ""
            val displayName = sessionStore.displayName.first() ?: username
            val token = sessionStore.token.first() ?: ""
            val serverUrl = sessionStore.serverUrl.first() ?: "http://127.0.0.1:3970"

            _uiState.update { it.copy(
                currentUserId = userId,
                currentUsername = username,
                currentDisplayName = displayName
            )}

            webSocketManager.connect(serverUrl, token)
            observeWebSocketEvents()

            loadServers()
            loadDms()
            loadFriends()
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                when (event) {
                    is ServerEvent.NewMessage -> {
                        // Handled by ChatViewModel
                    }
                    is ServerEvent.Presence -> {
                        _uiState.update { state ->
                            val updated = state.presence.toMutableMap()
                            updated[event.userId] = PresenceEntry(event.userId, event.state, event.customStatus)
                            state.copy(presence = updated)
                        }
                    }
                    is ServerEvent.VoiceJoin -> {
                        if (event.userId == _uiState.value.currentUserId) {
                            _uiState.update { it.copy(inVoiceChannelId = event.channelId) }
                        }
                        refreshVoiceForCurrentServer()
                    }
                    is ServerEvent.VoiceLeave -> {
                        if (event.userId == _uiState.value.currentUserId) {
                            _uiState.update { it.copy(inVoiceChannelId = null) }
                        }
                        refreshVoiceForCurrentServer()
                    }
                    is ServerEvent.VoiceFlags -> {
                        if (event.userId == _uiState.value.currentUserId) {
                            _uiState.update { it.copy(
                                isMuted = event.muted,
                                isDeafened = event.deafened,
                                hasCamera = event.camera,
                                hasScreen = event.screen
                            )}
                        }
                        refreshVoiceForCurrentServer()
                    }
                    is ServerEvent.DmOpen -> {
                        val existing = _uiState.value.dms.any { it.id == event.dm.id }
                        if (!existing) {
                            _uiState.update { it.copy(dms = listOf(event.dm) + it.dms) }
                        }
                    }
                    is ServerEvent.FriendAdded -> {
                        _uiState.update { it.copy(friends = it.friends + event.friend) }
                    }
                    is ServerEvent.FriendRemoved -> {
                        _uiState.update { it.copy(friends = it.friends.filter { f -> f.id != event.userId }) }
                    }
                    is ServerEvent.FriendRequestReceived -> {
                        _uiState.update { it.copy(friendRequests = it.friendRequests + event.request) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun refreshVoiceForCurrentServer() {
        val serverId = _uiState.value.selectedServerId ?: return
        viewModelScope.launch {
            serverRepository.serverVoice(serverId).onSuccess { members ->
                _uiState.update { it.copy(voiceMembers = members) }
            }
        }
    }

    fun loadServers() {
        viewModelScope.launch {
            serverRepository.serverRail().onSuccess { rail ->
                _uiState.update { it.copy(servers = rail) }
            }
        }
    }

    fun selectServer(serverId: Long) {
        _uiState.update { it.copy(
            selectedServerId = serverId,
            selectedChannelId = null,
            selectedDmId = null,
            activeTab = MainTab.SERVERS
        )}
        viewModelScope.launch {
            serverRepository.serverLayout(serverId).onSuccess { layout ->
                _uiState.update { it.copy(serverLayout = layout) }
            }
            serverRepository.members(serverId).onSuccess { members ->
                _uiState.update { it.copy(members = members) }
            }
            serverRepository.roles(serverId).onSuccess { roles ->
                _uiState.update { it.copy(roles = roles) }
            }
            serverRepository.serverVoice(serverId).onSuccess { voice ->
                _uiState.update { it.copy(voiceMembers = voice) }
            }
        }
    }

    fun selectChannel(channelId: Long) {
        _uiState.update { it.copy(selectedChannelId = channelId, selectedDmId = null) }
    }

    fun selectDm(dmId: Long) {
        _uiState.update { it.copy(
            selectedDmId = dmId,
            selectedChannelId = null,
            selectedServerId = null,
            serverLayout = null,
            activeTab = MainTab.DMS
        )}
    }

    fun loadDms() {
        viewModelScope.launch {
            dmRepository.listDms().onSuccess { dms ->
                _uiState.update { it.copy(dms = dms) }
            }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            friendRepository.listFriends().onSuccess { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
            friendRepository.listRequests().onSuccess { reqs ->
                _uiState.update { it.copy(friendRequests = reqs) }
            }
            friendRepository.listPresence().onSuccess { pres ->
                _uiState.update { state ->
                    state.copy(presence = pres.associateBy { it.userId })
                }
            }
        }
    }

    fun setActiveTab(tab: MainTab) {
        _uiState.update { it.copy(activeTab = tab, selectedServerId = null, serverLayout = null) }
    }

    fun joinVoice(channelId: Long) {
        viewModelScope.launch {
            voiceRepository.join(channelId).onSuccess {
                _uiState.update { it.copy(inVoiceChannelId = channelId) }
            }
        }
    }

    fun leaveVoice() {
        viewModelScope.launch {
            voiceRepository.leave().onSuccess {
                _uiState.update { it.copy(inVoiceChannelId = null) }
            }
        }
    }

    fun toggleMute() {
        val current = _uiState.value
        val newMuted = !current.isMuted
        viewModelScope.launch {
            voiceRepository.setFlags(newMuted, current.isDeafened, current.hasCamera, current.hasScreen)
                .onSuccess { _uiState.update { it.copy(isMuted = newMuted) } }
        }
    }

    fun toggleDeafen() {
        val current = _uiState.value
        val newDeafened = !current.isDeafened
        viewModelScope.launch {
            voiceRepository.setFlags(current.isMuted, newDeafened, current.hasCamera, current.hasScreen)
                .onSuccess { _uiState.update { it.copy(isDeafened = newDeafened) } }
        }
    }

    fun createServer(name: String) {
        viewModelScope.launch {
            serverRepository.createServer(name).onSuccess { _ ->
                loadServers()
            }
        }
    }

    fun joinServerByInvite(code: String) {
        viewModelScope.launch {
            serverRepository.joinInvite(code).onSuccess { _ ->
                loadServers()
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            webSocketManager.disconnect()
            authRepository.logout()
            onDone()
        }
    }

    fun openDm(userId: Long) {
        viewModelScope.launch {
            dmRepository.openDm(userId).onSuccess { dm ->
                val existing = _uiState.value.dms.any { it.id == dm.id }
                if (!existing) {
                    _uiState.update { it.copy(dms = listOf(dm) + it.dms) }
                }
                selectDm(dm.id)
            }
        }
    }

    fun sendFriendRequest(username: String) {
        viewModelScope.launch {
            friendRepository.sendRequest(username)
        }
    }

    fun acceptFriendRequest(id: Long) {
        viewModelScope.launch {
            friendRepository.acceptRequest(id).onSuccess { _ ->
                _uiState.update { it.copy(friendRequests = it.friendRequests.filter { r -> r.id != id }) }
                loadFriends()
            }
        }
    }

    fun declineFriendRequest(id: Long) {
        viewModelScope.launch {
            friendRepository.declineRequest(id).onSuccess {
                _uiState.update { it.copy(friendRequests = it.friendRequests.filter { r -> r.id != id }) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
