package com.banter.android.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.banter.android.ui.screen.main.components.*
import com.banter.android.ui.theme.BanterColors

/**
 * Discord-mobile style shell:
 * - Browse: server rail + channel/DM/friends list
 * - Conversation: full-screen chat with system back
 * - Members: side overlay from chat top bar
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateServer by remember { mutableStateOf(false) }
    var showJoinServer by remember { mutableStateOf(false) }
    var showAddFriend by remember { mutableStateOf(false) }
    var homePane by remember { mutableStateOf(HomePane.MESSAGES) }

    BackHandler(enabled = uiState.inConversation || uiState.showMembers) {
        when {
            uiState.showMembers -> viewModel.toggleMembers()
            uiState.inConversation -> viewModel.leaveConversation()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BanterColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
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
                                    onVoiceJoin = { viewModel.joinVoice(it) },
                                    onVoiceLeave = { viewModel.leaveVoice() },
                                    onToggleMute = { viewModel.toggleMute() },
                                    onToggleDeafen = { viewModel.toggleDeafen() },
                                    onLogout = { viewModel.logout(onLogout) },
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
                                // Keep user bar at bottom on friends too
                                ChannelSidebar(
                                    uiState = uiState,
                                    onChannelClick = { _, _ -> },
                                    onDmClick = { _, _ -> },
                                    onVoiceJoin = {},
                                    onVoiceLeave = {},
                                    onToggleMute = { viewModel.toggleMute() },
                                    onToggleDeafen = { viewModel.toggleDeafen() },
                                    onLogout = { viewModel.logout(onLogout) },
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
                            onVoiceJoin = { viewModel.joinVoice(it) },
                            onVoiceLeave = { viewModel.leaveVoice() },
                            onToggleMute = { viewModel.toggleMute() },
                            onToggleDeafen = { viewModel.toggleDeafen() },
                            onLogout = { viewModel.logout(onLogout) },
                            onAddFriendClick = { showAddFriend = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
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
