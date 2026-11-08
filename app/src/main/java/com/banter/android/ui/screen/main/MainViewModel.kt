package com.banter.android.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banter.android.data.api.WebSocketManager
import com.banter.android.data.local.SessionStore
import com.banter.android.data.model.*
import com.banter.android.data.repository.*
import com.banter.android.media.BanterRtcManager
import com.banter.android.media.ScreenShareManager
import com.banter.android.ui.screen.main.components.CallKind
import com.banter.android.ui.screen.main.components.CallParticipant
import com.banter.android.ui.screen.main.components.CallPhaseUi
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
    val layout: List<CategoryLayout> = emptyList(),
    val members: List<Member> = emptyList(),
    val roles: List<GuildRole> = emptyList(),
    val voiceByChannel: Map<Long, List<VoiceMember>> = emptyMap(),
    val dms: List<DirectChat> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val friendRequests: List<FriendRequest> = emptyList(),
    val presence: Map<Long, PresenceEntry> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inVoiceChannelId: Long? = null,
    val inVoiceChannelName: String = "",
    val isMuted: Boolean = false,
    val isDeafened: Boolean = false,
    val hasCamera: Boolean = false,
    val hasScreen: Boolean = false,
    val cameraFrontFacing: Boolean = true,
    val mirrorCamera: Boolean = true,
    val pendingCameraPermission: Boolean = false,
    val activeTab: MainTab = MainTab.HOME,
    val inConversation: Boolean = false,
    val showMembers: Boolean = false,
    /** Full-screen phone-call UI for VC or DM. */
    val callOpen: Boolean = false,
    val callMinimized: Boolean = false,
    val callKind: CallKind = CallKind.VoiceChannel,
    val callPhase: CallPhaseUi = CallPhaseUi.Connecting,
    val activeDmCall: CallSnapshot? = null,
    val pendingScreenSharePermission: Boolean = false,
    /** Featured remote Go Live publisher (cycle with watch controls). */
    val focusedStreamerId: Long? = null
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
    val webSocketManager: WebSocketManager,
    val screenShareManager: ScreenShareManager,
    val rtcManager: BanterRtcManager
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
            val cameraFront = sessionStore.cameraFrontFacing.first()
            val mirror = sessionStore.mirrorCamera.first()

            _uiState.update {
                it.copy(
                    currentUserId = userId,
                    currentUsername = username,
                    currentDisplayName = displayName,
                    cameraFrontFacing = cameraFront,
                    mirrorCamera = mirror
                )
            }

            if (token.isNotBlank()) {
                webSocketManager.connect(serverUrl, token)
            }
            observeWebSocketEvents()
            viewModelScope.launch {
                screenShareManager.isSharing.collect { sharing ->
                    if (_uiState.value.hasScreen != sharing) {
                        setScreenFlag(sharing)
                    }
                }
            }
            viewModelScope.launch {
                sessionStore.cameraFrontFacing.collect { front ->
                    _uiState.update { it.copy(cameraFrontFacing = front) }
                }
            }
            viewModelScope.launch {
                sessionStore.mirrorCamera.collect { mirrorCam ->
                    _uiState.update { it.copy(mirrorCamera = mirrorCam) }
                }
            }
            loadServers()
            loadDms()
            loadFriends()
            // Re-SubscribeLayer every 2s while seated so late Go Live gets an IDR.
            viewModelScope.launch {
                while (true) {
                    kotlinx.coroutines.delay(2_000)
                    val s = _uiState.value
                    if (s.inVoiceChannelId != null ||
                        (s.activeDmCall != null && s.callPhase == CallPhaseUi.Active)
                    ) {
                        syncRemoteSubscriptions()
                    }
                }
            }
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
                            val self = event.members.find { it.userId == state.currentUserId }
                            state.copy(
                                voiceByChannel = map,
                                inVoiceChannelId = when {
                                    self != null -> event.channelId
                                    state.inVoiceChannelId == event.channelId -> null
                                    else -> state.inVoiceChannelId
                                },
                                callOpen = when {
                                    self != null -> true
                                    state.inVoiceChannelId == event.channelId && self == null -> false
                                    else -> state.callOpen
                                },
                                callPhase = if (self != null) CallPhaseUi.Active else state.callPhase,
                                isMuted = self?.flags?.muted ?: state.isMuted,
                                isDeafened = self?.flags?.deafened ?: state.isDeafened,
                                hasCamera = self?.flags?.camera ?: state.hasCamera,
                                hasScreen = self?.flags?.screen ?: state.hasScreen
                            )
                        }
                        syncRtcSession()
                        syncRemoteSubscriptions()
                    }
                    is ServerEvent.Call -> {
                        val call = event.call
                        val selfId = _uiState.value.currentUserId
                        val isCallee = call.callee == selfId
                        val isCaller = call.caller == selfId
                        if (!isCallee && !isCaller) return@collect
                        val peerName = _uiState.value.dms.find { it.id == call.dmId }?.peerDisplayName
                            ?: "Direct Call"
                        val phase = when (call.phase.lowercase()) {
                            "active" -> CallPhaseUi.Active
                            else -> CallPhaseUi.Ringing
                        }
                        _uiState.update {
                            it.copy(
                                activeDmCall = call,
                                callOpen = true,
                                callMinimized = false,
                                callKind = CallKind.DmCall,
                                callPhase = phase,
                                selectedDmId = call.dmId,
                                selectedDmPeerName = peerName,
                                isMuted = if (isCaller) call.callerFlags.muted else call.calleeFlags.muted,
                                hasCamera = if (isCaller) call.callerFlags.camera else call.calleeFlags.camera,
                                hasScreen = if (isCaller) call.callerFlags.screen else call.calleeFlags.screen
                            )
                        }
                        syncRtcSession()
                        syncRemoteSubscriptions()
                    }
                    is ServerEvent.CallEnded -> {
                        if (_uiState.value.activeDmCall?.dmId == event.dmId) {
                            screenShareManager.stop()
                            if (_uiState.value.inVoiceChannelId == null) {
                                rtcManager.stop()
                            }
                            _uiState.update {
                                it.copy(
                                    activeDmCall = null,
                                    callOpen = it.inVoiceChannelId != null,
                                    callKind = if (it.inVoiceChannelId != null) CallKind.VoiceChannel else it.callKind,
                                    callPhase = CallPhaseUi.Active,
                                    hasScreen = false,
                                    focusedStreamerId = null
                                )
                            }
                            syncRtcSession()
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

    fun callParticipants(): List<CallParticipant> {
        val state = _uiState.value
        return when (state.callKind) {
            CallKind.VoiceChannel -> {
                val channelId = state.inVoiceChannelId ?: return emptyList()
                val voice = state.voiceByChannel[channelId].orEmpty()
                voice.map { vm ->
                    val member = state.members.find { it.userId == vm.userId }
                    val name = member?.nickname ?: member?.displayName
                        ?: if (vm.userId == state.currentUserId) state.currentDisplayName
                        else "User ${vm.userId}"
                    CallParticipant(vm.userId, name, vm.flags, vm.userId == state.currentUserId)
                }
            }
            CallKind.DmCall -> {
                val call = state.activeDmCall ?: return emptyList()
                val peer = state.dms.find { it.id == call.dmId }
                val peerName = peer?.peerDisplayName ?: state.selectedDmPeerName
                val peerId = peer?.peerId ?: if (call.caller == state.currentUserId) call.callee else call.caller
                val selfFlags = if (call.caller == state.currentUserId) call.callerFlags else call.calleeFlags
                val peerFlags = if (call.caller == state.currentUserId) call.calleeFlags else call.callerFlags
                listOf(
                    CallParticipant(state.currentUserId, state.currentDisplayName, selfFlags, isSelf = true),
                    CallParticipant(peerId, peerName, peerFlags, isSelf = false)
                )
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

    fun joinVoice(channelId: Long, channelName: String = "") {
        val name = channelName.ifBlank {
            _uiState.value.layout.flatMap { it.channels }.find { it.id == channelId }?.name
                ?: "Voice"
        }
        viewModelScope.launch {
            voiceRepository.join(channelId).onSuccess {
                _uiState.update {
                    it.copy(
                        inVoiceChannelId = channelId,
                        inVoiceChannelName = name,
                        callOpen = true,
                        callMinimized = false,
                        callKind = CallKind.VoiceChannel,
                        callPhase = CallPhaseUi.Active,
                        activeDmCall = null
                    )
                }
                syncRtcSession()
            }
        }
    }

    fun leaveVoice() {
        viewModelScope.launch {
            screenShareManager.stop()
            voiceRepository.leave().onSuccess {
                if (_uiState.value.activeDmCall == null) {
                    rtcManager.stop()
                }
                _uiState.update {
                    it.copy(
                        inVoiceChannelId = null,
                        inVoiceChannelName = "",
                        callOpen = it.activeDmCall != null,
                        hasScreen = false,
                        hasCamera = false,
                        focusedStreamerId = null
                    )
                }
                syncRtcSession()
            }
        }
    }

    fun startDmCall(dmId: Long) {
        viewModelScope.launch {
            dmRepository.startCall(dmId).onSuccess {
                val peer = _uiState.value.dms.find { it.id == dmId }?.peerDisplayName ?: "Call"
                _uiState.update {
                    it.copy(
                        callOpen = true,
                        callMinimized = false,
                        callKind = CallKind.DmCall,
                        callPhase = CallPhaseUi.Ringing,
                        selectedDmId = dmId,
                        selectedDmPeerName = peer,
                        activeDmCall = CallSnapshot(
                            dmId = dmId,
                            phase = "ringing",
                            caller = it.currentUserId,
                            callee = it.dms.find { d -> d.id == dmId }?.peerId ?: 0L
                        )
                    )
                }
            }
        }
    }

    fun acceptDmCall() {
        val dmId = _uiState.value.activeDmCall?.dmId ?: return
        viewModelScope.launch {
            dmRepository.acceptCall(dmId).onSuccess {
                _uiState.update { it.copy(callPhase = CallPhaseUi.Active) }
                syncRtcSession()
            }
        }
    }

    fun declineDmCall() {
        val dmId = _uiState.value.activeDmCall?.dmId ?: return
        viewModelScope.launch {
            dmRepository.declineCall(dmId)
            screenShareManager.stop()
            if (_uiState.value.inVoiceChannelId == null) rtcManager.stop()
            _uiState.update {
                it.copy(activeDmCall = null, callOpen = it.inVoiceChannelId != null, hasScreen = false)
            }
            syncRtcSession()
        }
    }

    fun endDmCall() {
        val dmId = _uiState.value.activeDmCall?.dmId ?: return
        viewModelScope.launch {
            dmRepository.endCall(dmId)
            screenShareManager.stop()
            if (_uiState.value.inVoiceChannelId == null) rtcManager.stop()
            _uiState.update {
                it.copy(
                    activeDmCall = null,
                    callOpen = it.inVoiceChannelId != null,
                    hasScreen = false,
                    focusedStreamerId = null
                )
            }
            syncRtcSession()
        }
    }

    /** Live Go Live publishers in the current call (remote + self). */
    fun liveStreamerIds(): List<Long> {
        val state = _uiState.value
        val me = state.currentUserId
        val ids = linkedSetOf<Long>()
        when (state.callKind) {
            CallKind.VoiceChannel -> {
                val channelId = state.inVoiceChannelId ?: return emptyList()
                for (m in state.voiceByChannel[channelId].orEmpty()) {
                    if (m.flags.screen) ids += m.userId
                }
            }
            CallKind.DmCall -> {
                for (p in callParticipants()) {
                    if (p.flags.screen) ids += p.userId
                }
                // Remote frames may arrive before roster flags update.
                for ((key, _) in rtcManager.remoteFrames.value) {
                    if (key.kind == MediaKind.Screen) ids += key.publisher
                }
            }
        }
        if (state.hasScreen) ids += me
        return ids.toList()
    }

    fun focusedStreamerId(): Long? {
        val live = liveStreamerIds()
        if (live.isEmpty()) return null
        val focus = _uiState.value.focusedStreamerId
        return if (focus != null && focus in live) focus else live.first()
    }

    fun cycleFocusedStreamer() {
        val live = liveStreamerIds()
        if (live.isEmpty()) {
            _uiState.update { it.copy(focusedStreamerId = null) }
            return
        }
        val current = focusedStreamerId()
        val idx = live.indexOf(current)
        val next = live[(idx + 1).floorMod(live.size)]
        _uiState.update { it.copy(focusedStreamerId = next) }
    }

    private fun Int.floorMod(m: Int): Int = ((this % m) + m) % m

    private fun syncRtcSession() {
        val state = _uiState.value
        val me = state.currentUserId
        when {
            state.inVoiceChannelId != null && state.callPhase == CallPhaseUi.Active -> {
                // Guild SFU: everyone offers (desktop webrtc_offerer).
                rtcManager.start(
                    place = MediaPlace.VoiceChannel(state.inVoiceChannelId),
                    me = me,
                    offerer = true
                )
                syncRemoteSubscriptions()
            }
            state.activeDmCall != null && state.callPhase == CallPhaseUi.Active -> {
                val call = state.activeDmCall
                rtcManager.start(
                    place = MediaPlace.DirectCall(call.dmId),
                    me = me,
                    offerer = call.caller == me
                )
                syncRemoteSubscriptions()
            }
            else -> {
                // Keep PC only while seated in voice or active DM call.
                if (state.inVoiceChannelId == null &&
                    (state.activeDmCall == null || state.callPhase != CallPhaseUi.Active)
                ) {
                    rtcManager.stop()
                }
            }
        }
    }

    private fun syncRemoteSubscriptions() {
        val state = _uiState.value
        val pairs = when {
            state.inVoiceChannelId != null -> {
                state.voiceByChannel[state.inVoiceChannelId].orEmpty()
                    .map { it.userId to it.flags }
            }
            state.activeDmCall != null -> {
                callParticipants().map { it.userId to it.flags }
            }
            else -> emptyList()
        }
        rtcManager.ensureSubsForMembers(pairs)
    }

    fun hangUp() {
        when (_uiState.value.callKind) {
            CallKind.VoiceChannel -> leaveVoice()
            CallKind.DmCall -> endDmCall()
        }
    }

    fun minimizeCall() {
        _uiState.update { it.copy(callMinimized = true) }
    }

    fun expandCall() {
        _uiState.update { it.copy(callMinimized = false, callOpen = true) }
    }

    fun toggleMute() {
        val current = _uiState.value
        val newMuted = !current.isMuted
        pushFlags(newMuted, current.isDeafened, current.hasCamera, current.hasScreen)
    }

    fun toggleDeafen() {
        val current = _uiState.value
        val newDeafened = !current.isDeafened
        pushFlags(
            muted = current.isMuted || newDeafened,
            deafened = newDeafened,
            camera = current.hasCamera,
            screen = current.hasScreen
        )
    }

    fun toggleCamera() {
        val current = _uiState.value
        if (current.hasCamera) {
            pushFlags(current.isMuted, current.isDeafened, camera = false, current.hasScreen)
        } else {
            // Ask UI to ensure CAMERA permission, then enable.
            _uiState.update { it.copy(pendingCameraPermission = true) }
        }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(pendingCameraPermission = false) }
        if (granted) {
            val current = _uiState.value
            pushFlags(current.isMuted, current.isDeafened, camera = true, current.hasScreen)
        }
    }

    fun requestScreenShare() {
        if (_uiState.value.hasScreen || screenShareManager.isSharing.value) {
            screenShareManager.stop()
            pushFlags(
                _uiState.value.isMuted,
                _uiState.value.isDeafened,
                _uiState.value.hasCamera,
                screen = false
            )
        } else {
            _uiState.update { it.copy(pendingScreenSharePermission = true) }
        }
    }

    fun onScreenSharePermissionHandled() {
        _uiState.update { it.copy(pendingScreenSharePermission = false) }
    }

    private fun setScreenFlag(sharing: Boolean) {
        val current = _uiState.value
        pushFlags(current.isMuted, current.isDeafened, current.hasCamera, sharing)
    }

    private fun pushFlags(muted: Boolean, deafened: Boolean, camera: Boolean, screen: Boolean) {
        viewModelScope.launch {
            voiceRepository.setFlags(muted, deafened, camera, screen).onSuccess {
                _uiState.update {
                    it.copy(
                        isMuted = muted,
                        isDeafened = deafened,
                        hasCamera = camera,
                        hasScreen = screen
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
