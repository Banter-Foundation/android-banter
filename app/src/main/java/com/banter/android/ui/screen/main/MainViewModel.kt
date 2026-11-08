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
    val servers: List<ServerSummary> = emptyList(),
    val selectedServerId: Long? = null,
    val selectedServerName: String = "",
    val selectedChannelId: Long? = null,
    val selectedChannelName: String = "",
    val selectedDmId: Long? = null,
    val selectedDmPeerName: String = "",
    /** Nested category → channels from /v1/servers/{id}/layout */
    val layout: List<CategoryLayout> = emptyList(),
    val members: List<Member> = emptyList(),
    val roles: List<GuildRole> = emptyList(),
    /** channelId → voice members currently in that channel */
    val voiceByChannel: Map<Long, List<VoiceMember>> = emptyMap(),
    val dms: List<DirectChat> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val friendRequests: List<FriendRequest> = emptyList(),
    val presence: Map<Long, PresenceEntry> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inVoiceChannelId: Long? = null,
    val isMuted: Boolean = false,
    val isDeafened: Boolean = false,
    val hasCamera: Boolean = false,
    val hasScreen: Boolean = false,
    val activeTab: MainTab = MainTab.HOME,
    /** Discord-mobile: true when chat/members fills the screen. */
    val inConversation: Boolean = false,
    val showMembers: Boolean = false
)

enum class MainTab { HOME, SERVERS }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val serverRepository: ServerRepository,
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

            _uiState.update {
                it.copy(
                    currentUserId = userId,
                    currentUsername = username,
                    currentDisplayName = displayName
                )
            }

            if (token.isNotBlank()) {
                webSocketManager.connect(serverUrl, token)
            }
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
                    is ServerEvent.Presence -> {
                        _uiState.update { state ->
                            val updated = state.presence.toMutableMap()
                            updated[event.userId] =
                                PresenceEntry(event.userId, event.state, event.customStatus)
                            state.copy(presence = updated)
                        }
                    }
                    is ServerEvent.Voice -> {
                        _uiState.update { state ->
                            val map = state.voiceByChannel.toMutableMap()
                            map[event.channelId] = event.members
                            val selfIn = event.members.any { it.userId == state.currentUserId }
                            state.copy(
                                voiceByChannel = map,
                                inVoiceChannelId = when {
                                    selfIn -> event.channelId
                                    state.inVoiceChannelId == event.channelId -> null
                                    else -> state.inVoiceChannelId
                                }
                            )
                        }
                    }
                    is ServerEvent.FriendRequestEvent -> {
                        _uiState.update {
                            if (it.friendRequests.any { r -> r.id == event.request.id }) it
                            else it.copy(friendRequests = it.friendRequests + event.request)
                        }
                    }
                    is ServerEvent.FriendsChanged -> loadFriends()
                    else -> {}
                }
            }
        }
    }

    fun loadServers() {
        viewModelScope.launch {
            // Prefer rail; fall back to flat list so guilds still show.
            val rail = serverRepository.serverRail()
            rail.onSuccess { data ->
                val servers = data.servers
                    .sortedBy { it.position }
                    .map { it.server }
                if (servers.isNotEmpty()) {
                    _uiState.update { it.copy(servers = servers) }
                    return@launch
                }
            }
            serverRepository.listServers().onSuccess { list ->
                _uiState.update { it.copy(servers = list) }
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message) }
            }
        }
    }

    fun selectServer(serverId: Long) {
        val name = _uiState.value.servers.find { it.id == serverId }?.name ?: ""
        _uiState.update {
            it.copy(
                selectedServerId = serverId,
                selectedServerName = name,
                selectedChannelId = null,
                selectedChannelName = "",
                selectedDmId = null,
                selectedDmPeerName = "",
                inConversation = false,
                showMembers = false,
                activeTab = MainTab.SERVERS,
                layout = emptyList()
            )
        }
        viewModelScope.launch {
            serverRepository.serverLayout(serverId).onSuccess { layout ->
                _uiState.update { it.copy(layout = layout) }
            }
            serverRepository.members(serverId).onSuccess { members ->
                _uiState.update { it.copy(members = members) }
            }
            serverRepository.roles(serverId).onSuccess { roles ->
                _uiState.update { it.copy(roles = roles) }
            }
            serverRepository.serverVoice(serverId).onSuccess { rosters ->
                _uiState.update { state ->
                    state.copy(voiceByChannel = rosters.associate { it.channelId to it.members })
                }
            }
        }
    }

    fun selectChannel(channelId: Long, channelName: String = "") {
        val name = channelName.ifBlank {
            _uiState.value.layout
                .flatMap { it.channels }
                .find { it.id == channelId }
                ?.name
                ?: "#channel"
        }
        _uiState.update {
            it.copy(
                selectedChannelId = channelId,
                selectedChannelName = name,
                selectedDmId = null,
                selectedDmPeerName = "",
                inConversation = true,
                showMembers = false
            )
        }
    }

    fun selectDm(dmId: Long, peerName: String = "") {
        val name = peerName.ifBlank {
            _uiState.value.dms.find { it.id == dmId }?.peerDisplayName ?: "DM"
        }
        _uiState.update {
            it.copy(
                selectedDmId = dmId,
                selectedDmPeerName = name,
                selectedChannelId = null,
                selectedChannelName = "",
                selectedServerId = null,
                selectedServerName = "",
                layout = emptyList(),
                inConversation = true,
                showMembers = false,
                activeTab = MainTab.HOME
            )
        }
    }

    /** Discord-mobile back: leave conversation, return to channel/DM list. */
    fun leaveConversation() {
        _uiState.update {
            it.copy(
                selectedChannelId = null,
                selectedChannelName = "",
                selectedDmId = null,
                selectedDmPeerName = "",
                inConversation = false,
                showMembers = false
            )
        }
    }

    fun toggleMembers() {
        _uiState.update { it.copy(showMembers = !it.showMembers) }
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
                _uiState.update { it.copy(friendRequests = reqs.incoming) }
            }
            friendRepository.listPresence().onSuccess { pres ->
                _uiState.update { state ->
                    state.copy(presence = pres.associateBy { it.userId })
                }
            }
        }
    }

    fun setActiveTab(tab: MainTab) {
        _uiState.update {
            it.copy(
                activeTab = tab,
                selectedServerId = if (tab == MainTab.HOME) null else it.selectedServerId,
                selectedServerName = if (tab == MainTab.HOME) "" else it.selectedServerName,
                layout = if (tab == MainTab.HOME) emptyList() else it.layout,
                selectedChannelId = null,
                selectedDmId = null,
                inConversation = false,
                showMembers = false
            )
        }
        if (tab == MainTab.HOME) {
            loadDms()
            loadFriends()
        }
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
            voiceRepository.setFlags(current.isMuted || newDeafened, newDeafened, current.hasCamera, current.hasScreen)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDeafened = newDeafened,
                            isMuted = if (newDeafened) true else it.isMuted
                        )
                    }
                }
        }
    }

    fun createServer(name: String) {
        viewModelScope.launch {
            serverRepository.createServer(name).onSuccess { home ->
                loadServers()
                selectServer(home.server.id)
            }
        }
    }

    fun joinServerByInvite(code: String) {
        viewModelScope.launch {
            serverRepository.joinInvite(code).onSuccess { home ->
                loadServers()
                selectServer(home.server.id)
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
                selectDm(dm.id, dm.peerDisplayName)
            }
        }
    }

    fun sendFriendRequest(username: String) {
        viewModelScope.launch { friendRepository.sendRequest(username) }
    }

    fun acceptFriendRequest(id: Long) {
        viewModelScope.launch {
            friendRepository.acceptRequest(id).onSuccess {
                _uiState.update {
                    it.copy(friendRequests = it.friendRequests.filter { r -> r.id != id })
                }
                loadFriends()
            }
        }
    }

    fun declineFriendRequest(id: Long) {
        viewModelScope.launch {
            friendRepository.declineRequest(id).onSuccess {
                _uiState.update {
                    it.copy(friendRequests = it.friendRequests.filter { r -> r.id != id })
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
