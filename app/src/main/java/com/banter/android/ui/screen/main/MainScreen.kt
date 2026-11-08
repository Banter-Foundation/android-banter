package com.banter.android.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.banter.android.ui.screen.main.components.*
import com.banter.android.ui.theme.BanterColors

/**
 * Root shell: server rail (far left) | channel sidebar | chat area | member list
 * On narrow screens the sidebar slides in as a drawer.
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
    var sidebarOpen by remember { mutableStateOf(false) }
    var memberListOpen by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Server Rail
        ServerRail(
            servers = uiState.servers,
            selectedServerId = uiState.selectedServerId,
            activeTab = uiState.activeTab,
            onServerClick = { viewModel.selectServer(it) },
            onHomeClick = { viewModel.setActiveTab(MainTab.HOME) },
            onDmsClick = { viewModel.setActiveTab(MainTab.DMS) },
            onAddServerClick = { showCreateServer = true }
        )

        // Sidebar (channel list or DM list or friends)
        ChannelSidebar(
            uiState = uiState,
            onChannelClick = { viewModel.selectChannel(it) },
            onDmClick = { viewModel.selectDm(it) },
            onVoiceJoin = { viewModel.joinVoice(it) },
            onVoiceLeave = { viewModel.leaveVoice() },
            onToggleMute = { viewModel.toggleMute() },
            onToggleDeafen = { viewModel.toggleDeafen() },
            onLogout = { viewModel.logout(onLogout) },
            onMemberListToggle = { memberListOpen = !memberListOpen },
            onAddFriendClick = { showAddFriend = true }
        )

        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(BanterColors.Background)
        ) {
            when {
                uiState.selectedChannelId != null -> {
                    ChatPanel(
                        channelId = uiState.selectedChannelId!!,
                        serverId = uiState.selectedServerId,
                        currentUserId = uiState.currentUserId,
                        onMemberListToggle = { memberListOpen = !memberListOpen },
                        webSocketManager = viewModel.webSocketManager
                    )
                }
                uiState.selectedDmId != null -> {
                    DmChatPanel(
                        dmId = uiState.selectedDmId!!,
                        currentUserId = uiState.currentUserId,
                        webSocketManager = viewModel.webSocketManager
                    )
                }
                uiState.activeTab == MainTab.HOME || uiState.activeTab == MainTab.DMS -> {
                    FriendsPanel(
                        friends = uiState.friends,
                        friendRequests = uiState.friendRequests,
                        presence = uiState.presence,
                        onOpenDm = { viewModel.openDm(it) },
                        onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                        onDeclineRequest = { viewModel.declineFriendRequest(it) }
                    )
                }
                else -> {
                    EmptyState()
                }
            }
        }

        // Member list panel
        if (memberListOpen && uiState.selectedServerId != null) {
            MemberListPanel(
                members = uiState.members,
                roles = uiState.roles,
                presence = uiState.presence,
                currentUserId = uiState.currentUserId
            )
        }
    }

    // Dialogs
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
