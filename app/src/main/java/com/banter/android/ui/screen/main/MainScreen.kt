package com.banter.android.ui.screen.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.banter.android.data.model.VoiceFlags
import com.banter.android.ui.screen.main.components.*
import com.banter.android.ui.screen.settings.SettingsScreen
import com.banter.android.ui.theme.BanterColors

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateServer by remember { mutableStateOf(false) }
    var showJoinServer by remember { mutableStateOf(false) }
    var showAddFriend by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var homePane by remember { mutableStateOf(HomePane.MESSAGES) }
    val context = LocalContext.current

    val screenShareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.screenShareManager.onPermissionResult(result.resultCode, result.data)
        viewModel.onScreenSharePermissionHandled()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }

    LaunchedEffect(uiState.pendingScreenSharePermission) {
        if (uiState.pendingScreenSharePermission) {
            screenShareLauncher.launch(viewModel.screenShareManager.createScreenCaptureIntent())
        }
    }

    LaunchedEffect(uiState.pendingCameraPermission) {
        if (uiState.pendingCameraPermission) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.onCameraPermissionResult(true)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    BackHandler(
        enabled = showSettings || (uiState.callOpen && !uiState.callMinimized) ||
            uiState.inConversation || uiState.showMembers
    ) {
        when {
            showSettings -> showSettings = false
            uiState.callOpen && !uiState.callMinimized -> viewModel.minimizeCall()
            uiState.showMembers -> viewModel.toggleMembers()
            uiState.inConversation -> viewModel.leaveConversation()
        }
    }

    if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false },
            onLogout = {
                showSettings = false
                viewModel.logout(onLogout)
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BanterColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main content under call UI
        if (!(uiState.callOpen && !uiState.callMinimized)) {
            if (uiState.inConversation) {
                when {
                    uiState.selectedChannelId != null -> {
                        ChatPanel(
                            channelId = uiState.selectedChannelId!!,
                            channelName = uiState.selectedChannelName,
                            currentUserId = uiState.currentUserId,
                            isDm = false,
                            onBack = { viewModel.leaveConversation() },
                            onMemberListToggle = { viewModel.toggleMembers() },
                            showMemberButton = true,
                            onStartCall = null,
                            webSocketManager = viewModel.webSocketManager
                        )
                    }
                    uiState.selectedDmId != null -> {
                        ChatPanel(
                            channelId = uiState.selectedDmId!!,
                            channelName = uiState.selectedDmPeerName,
                            currentUserId = uiState.currentUserId,
                            isDm = true,
                            onBack = { viewModel.leaveConversation() },
                            onMemberListToggle = {},
                            showMemberButton = false,
                            onStartCall = { viewModel.startDmCall(uiState.selectedDmId!!) },
                            webSocketManager = viewModel.webSocketManager
                        )
                    }
                }

                if (uiState.showMembers && uiState.selectedServerId != null) {
                    MemberListOverlay(
                        members = uiState.members,
                        presence = uiState.presence,
                        currentUserId = uiState.currentUserId,
                        onClose = { viewModel.toggleMembers() }
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    ServerRail(
                        servers = uiState.servers,
                        selectedServerId = uiState.selectedServerId,
                        activeTab = uiState.activeTab,
                        onServerClick = { viewModel.selectServer(it) },
                        onHomeClick = {
                            homePane = HomePane.MESSAGES
                            viewModel.setActiveTab(MainTab.HOME)
                        },
                        onAddServerClick = { showCreateServer = true }
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(BanterColors.ChannelSidebar)
                    ) {
                        if (uiState.activeTab == MainTab.HOME) {
                            HomePaneTabs(
                                selected = homePane,
                                pendingCount = uiState.friendRequests.size,
                                onSelect = { homePane = it },
                                onAddFriend = { showAddFriend = true }
                            )
                            when (homePane) {
                                HomePane.MESSAGES -> {
                                    ChannelSidebar(
                                        uiState = uiState,
                                        onChannelClick = { id, name -> viewModel.selectChannel(id, name) },
                                        onDmClick = { id, name -> viewModel.selectDm(id, name) },
                                        onVoiceJoin = { id, name -> viewModel.joinVoice(id, name) },
                                        onVoiceLeave = { viewModel.leaveVoice() },
                                        onToggleMute = { viewModel.toggleMute() },
                                        onToggleDeafen = { viewModel.toggleDeafen() },
                                        onLogout = { viewModel.logout(onLogout) },
                                        onOpenSettings = { showSettings = true },
                                        onAddFriendClick = { showAddFriend = true },
                                        hideHeader = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HomePane.FRIENDS -> {
                                    FriendsPanel(
                                        friends = uiState.friends,
                                        friendRequests = uiState.friendRequests,
                                        presence = uiState.presence,
                                        onOpenDm = { viewModel.openDm(it) },
                                        onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                                        onDeclineRequest = { viewModel.declineFriendRequest(it) },
                                        compact = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChannelSidebar(
                                        uiState = uiState,
                                        onChannelClick = { _, _ -> },
                                        onDmClick = { _, _ -> },
                                        onVoiceJoin = { _, _ -> },
                                        onVoiceLeave = {},
                                        onToggleMute = { viewModel.toggleMute() },
                                        onToggleDeafen = { viewModel.toggleDeafen() },
                                        onLogout = { viewModel.logout(onLogout) },
                                        onOpenSettings = { showSettings = true },
                                        onAddFriendClick = { showAddFriend = true },
                                        hideHeader = true,
                                        userBarOnly = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            ChannelSidebar(
                                uiState = uiState,
                                onChannelClick = { id, name -> viewModel.selectChannel(id, name) },
                                onDmClick = { id, name -> viewModel.selectDm(id, name) },
                                onVoiceJoin = { id, name -> viewModel.joinVoice(id, name) },
                                onVoiceLeave = { viewModel.leaveVoice() },
                                onToggleMute = { viewModel.toggleMute() },
                                onToggleDeafen = { viewModel.toggleDeafen() },
                                onLogout = { viewModel.logout(onLogout) },
                                onOpenSettings = { showSettings = true },
                                onAddFriendClick = { showAddFriend = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Minimized call pill
        if (uiState.callOpen && uiState.callMinimized) {
            MinimizedCallBar(
                title = when (uiState.callKind) {
                    CallKind.VoiceChannel -> uiState.inVoiceChannelName.ifBlank { "Voice" }
                    CallKind.DmCall -> uiState.selectedDmPeerName.ifBlank { "Call" }
                },
                onExpand = { viewModel.expandCall() },
                onHangUp = { viewModel.hangUp() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        // Full phone-call stage
        if (uiState.callOpen && !uiState.callMinimized) {
            val title = when (uiState.callKind) {
                CallKind.VoiceChannel -> uiState.inVoiceChannelName.ifBlank { "Voice Channel" }
                CallKind.DmCall -> uiState.selectedDmPeerName.ifBlank { "Direct Call" }
            }
            val subtitle = when (uiState.callKind) {
                CallKind.VoiceChannel -> "Voice Connected"
                CallKind.DmCall -> if (uiState.callPhase == CallPhaseUi.Active) "Call in progress" else "Calling…"
            }
            val remoteFrames by viewModel.rtcManager.remoteFrames.collectAsState()
            CallScreen(
                title = title,
                subtitle = subtitle,
                kind = uiState.callKind,
                phase = uiState.callPhase,
                members = viewModel.callParticipants(),
                selfFlags = VoiceFlags(
                    muted = uiState.isMuted,
                    deafened = uiState.isDeafened,
                    camera = uiState.hasCamera,
                    screen = uiState.hasScreen
                ),
                isScreenSharing = uiState.hasScreen,
                screenShareManager = viewModel.screenShareManager,
                remoteFrames = remoteFrames,
                focusedStreamerId = viewModel.focusedStreamerId(),
                liveStreamerIds = viewModel.liveStreamerIds(),
                cameraFrontFacing = uiState.cameraFrontFacing,
                mirrorCamera = uiState.mirrorCamera,
                onCycleStreamer = { viewModel.cycleFocusedStreamer() },
                onToggleMute = { viewModel.toggleMute() },
                onToggleDeafen = { viewModel.toggleDeafen() },
                onToggleCamera = { viewModel.toggleCamera() },
                onToggleScreenShare = { viewModel.requestScreenShare() },
                onHangUp = { viewModel.hangUp() },
                onAccept = if (uiState.callKind == CallKind.DmCall && uiState.callPhase == CallPhaseUi.Ringing
                    && uiState.activeDmCall?.callee == uiState.currentUserId
                ) ({ viewModel.acceptDmCall() }) else null,
                onDecline = if (uiState.callKind == CallKind.DmCall && uiState.callPhase == CallPhaseUi.Ringing
                    && uiState.activeDmCall?.callee == uiState.currentUserId
                ) ({ viewModel.declineDmCall() }) else null,
                onMinimize = { viewModel.minimizeCall() }
            )
        }
    }

    if (showCreateServer) {
        CreateServerDialog(
            onDismiss = { showCreateServer = false },
            onCreate = { name -> viewModel.createServer(name); showCreateServer = false },
            onJoinWithCode = { showCreateServer = false; showJoinServer = true }
        )
    }
    if (showJoinServer) {
        JoinServerDialog(
            onDismiss = { showJoinServer = false },
            onJoin = { code -> viewModel.joinServerByInvite(code); showJoinServer = false }
        )
    }
    if (showAddFriend) {
        AddFriendDialog(
            onDismiss = { showAddFriend = false },
            onSend = { username -> viewModel.sendFriendRequest(username); showAddFriend = false }
        )
    }
}

private enum class HomePane { MESSAGES, FRIENDS }

@Composable
private fun MinimizedCallBar(
    title: String,
    onExpand: () -> Unit,
    onHangUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(BanterColors.GreenDark)
            .clickable(onClick = onExpand)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(BanterColors.Red)
                .clickable(onClick = onHangUp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Call, contentDescription = "Hang up", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun HomePaneTabs(
    selected: HomePane,
    pendingCount: Int,
    onSelect: (HomePane) -> Unit,
    onAddFriend: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Home",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BanterColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Add Friend",
                color = BanterColors.Green,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onAddFriend)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                HomePane.MESSAGES to "Messages",
                HomePane.FRIENDS to if (pendingCount > 0) "Friends ($pendingCount)" else "Friends"
            ).forEach { (pane, label) ->
                val active = selected == pane
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) BanterColors.ChannelRowActive else Color.Transparent)
                        .clickable { onSelect(pane) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) BanterColors.TextPrimary else BanterColors.TextSecondary
                    )
                }
            }
        }
    }
}
