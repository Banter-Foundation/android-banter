package com.banter.android.ui.screen.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banter.android.data.model.CategoryLayout
import com.banter.android.data.model.Channel
import com.banter.android.data.model.DirectChat
import com.banter.android.data.model.VoiceMember
import com.banter.android.ui.screen.main.MainTab
import com.banter.android.ui.screen.main.MainUiState
import com.banter.android.ui.theme.BanterColors

@Composable
fun ChannelSidebar(
    uiState: MainUiState,
    onChannelClick: (Long, String) -> Unit,
    onDmClick: (Long, String) -> Unit,
    onVoiceJoin: (Long, String) -> Unit,
    onVoiceLeave: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleDeafen: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddFriendClick: () -> Unit,
    hideHeader: Boolean = false,
    userBarOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (userBarOnly) Modifier else Modifier.fillMaxHeight())
            .background(BanterColors.ChannelSidebar)
    ) {
        if (userBarOnly) {
            if (uiState.inVoiceChannelId != null) {
                VoiceBar(
                    isMuted = uiState.isMuted,
                    isDeafened = uiState.isDeafened,
                    onToggleMute = onToggleMute,
                    onToggleDeafen = onToggleDeafen,
                    onLeave = onVoiceLeave
                )
            }
            UserBar(
                displayName = uiState.currentDisplayName,
                username = uiState.currentUsername,
                onOpenSettings = onOpenSettings,
                onLogout = onLogout
            )
        } else {
            if (!hideHeader) {
                SidebarHeader(uiState, onAddFriendClick)
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.activeTab == MainTab.SERVERS && uiState.selectedServerId != null -> {
                        ServerChannelList(
                            layout = uiState.layout,
                            selectedChannelId = uiState.selectedChannelId,
                            voiceByChannel = uiState.voiceByChannel,
                            inVoiceChannelId = uiState.inVoiceChannelId,
                            onChannelClick = onChannelClick,
                            onVoiceJoin = onVoiceJoin,
                            onVoiceLeave = onVoiceLeave
                        )
                    }
                    else -> {
                        DmList(
                            dms = uiState.dms,
                            selectedDmId = uiState.selectedDmId,
                            presence = uiState.presence,
                            onDmClick = onDmClick
                        )
                    }
                }
            }

            if (uiState.inVoiceChannelId != null) {
                VoiceBar(
                    isMuted = uiState.isMuted,
                    isDeafened = uiState.isDeafened,
                    onToggleMute = onToggleMute,
                    onToggleDeafen = onToggleDeafen,
                    onLeave = onVoiceLeave
                )
            }

            UserBar(
                displayName = uiState.currentDisplayName,
                username = uiState.currentUsername,
                onOpenSettings = onOpenSettings,
                onLogout = onLogout
            )
        }
    }
}

@Composable
private fun SidebarHeader(uiState: MainUiState, onAddFriendClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = when {
                uiState.activeTab == MainTab.SERVERS && uiState.selectedServerName.isNotBlank() ->
                    uiState.selectedServerName
                else -> "Direct Messages"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BanterColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (uiState.activeTab == MainTab.HOME) {
            IconButton(onClick = onAddFriendClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.PersonAdd,
                    contentDescription = "Add Friend",
                    tint = BanterColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    HorizontalDivider(color = BanterColors.Divider, thickness = 1.dp)
}

@Composable
private fun ServerChannelList(
    layout: List<CategoryLayout>,
    selectedChannelId: Long?,
    voiceByChannel: Map<Long, List<VoiceMember>>,
    inVoiceChannelId: Long?,
    onChannelClick: (Long, String) -> Unit,
    onVoiceJoin: (Long, String) -> Unit,
    onVoiceLeave: () -> Unit
) {
    val sorted = layout.sortedBy { it.category.position }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (sorted.isEmpty()) {
            item {
                Text(
                    "No channels yet",
                    color = BanterColors.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        sorted.forEach { catLayout ->
            item(key = "cat_${catLayout.category.id}") {
                CategoryRow(catLayout.category.name)
            }
            val channels = catLayout.channels.sortedBy { it.position }
            items(channels, key = { it.id }) { channel ->
                when (channel.kind) {
                    "voice" -> VoiceChannelRow(
                        channel = channel,
                        members = voiceByChannel[channel.id].orEmpty(),
                        inVoice = inVoiceChannelId == channel.id,
                        onClick = {
                            if (inVoiceChannelId == channel.id) onVoiceLeave()
                            else onVoiceJoin(channel.id, channel.name)
                        }
                    )
                    else -> ChannelRow(
                        channel = channel,
                        isSelected = channel.id == selectedChannelId,
                        onClick = { onChannelClick(channel.id, channel.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 14.dp, bottom = 2.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = BanterColors.CategoryText,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = name.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = BanterColors.CategoryText,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ChannelRow(channel: Channel, isSelected: Boolean, onClick: () -> Unit) {
    val icon = if (channel.kind == "board") Icons.Filled.Dashboard else Icons.Filled.Tag
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) BanterColors.ChannelRowActive else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) BanterColors.ChannelTextActive else BanterColors.ChannelText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = channel.name,
            fontSize = 15.sp,
            color = if (isSelected) BanterColors.ChannelTextActive else BanterColors.ChannelText,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VoiceChannelRow(
    channel: Channel,
    members: List<VoiceMember>,
    inVoice: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 1.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (inVoice) BanterColors.ChannelRowActive else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.VolumeUp,
                contentDescription = null,
                tint = if (inVoice) BanterColors.VoiceConnected else BanterColors.ChannelText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = channel.name,
                fontSize = 15.sp,
                color = if (inVoice) BanterColors.ChannelTextActive else BanterColors.ChannelText,
                fontWeight = if (inVoice) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        members.forEach { vm ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(avatarColor(vm.userId)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("·", color = Color.White, fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "User ${vm.userId}",
                    fontSize = 13.sp,
                    color = BanterColors.TextSecondary,
                    maxLines = 1
                )
                if (vm.flags.muted) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.MicOff,
                        contentDescription = "Muted",
                        tint = BanterColors.TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DmList(
    dms: List<DirectChat>,
    selectedDmId: Long?,
    presence: Map<Long, com.banter.android.data.model.PresenceEntry>,
    onDmClick: (Long, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            Text(
                "DIRECT MESSAGES",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BanterColors.CategoryText,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 10.dp)
            )
        }
        if (dms.isEmpty()) {
            item {
                Text(
                    "No direct messages yet.\nOpen a friend to start chatting.",
                    color = BanterColors.TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(dms, key = { it.id }) { dm ->
            DmRow(
                dm = dm,
                isSelected = dm.id == selectedDmId,
                presenceState = presence[dm.peerId]?.state ?: "offline",
                onClick = { onDmClick(dm.id, dm.peerDisplayName) }
            )
        }
    }
}

@Composable
private fun DmRow(
    dm: DirectChat,
    isSelected: Boolean,
    presenceState: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) BanterColors.ChannelRowActive else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(avatarColor(dm.peerId)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dm.peerDisplayName.take(1).uppercase().ifEmpty { "?" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(BanterColors.ChannelSidebar)
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(presenceColor(presenceState))
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = dm.peerDisplayName.ifBlank { dm.peerUsername },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) BanterColors.TextPrimary else BanterColors.ChannelText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${dm.peerUsername}",
                fontSize = 12.sp,
                color = BanterColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VoiceBar(
    isMuted: Boolean,
    isDeafened: Boolean,
    onToggleMute: () -> Unit,
    onToggleDeafen: () -> Unit,
    onLeave: () -> Unit
) {
    HorizontalDivider(color = BanterColors.Divider)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BanterColors.VoiceBar)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.VolumeUp,
            contentDescription = null,
            tint = BanterColors.VoiceConnected,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Voice Connected",
            fontSize = 12.sp,
            color = BanterColors.VoiceConnected,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onToggleMute, modifier = Modifier.size(32.dp)) {
            Icon(
                if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = if (isMuted) BanterColors.Red else BanterColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onToggleDeafen, modifier = Modifier.size(32.dp)) {
            Icon(
                if (isDeafened) Icons.Filled.HeadsetOff else Icons.Filled.Headset,
                contentDescription = if (isDeafened) "Undeafen" else "Deafen",
                tint = if (isDeafened) BanterColors.Red else BanterColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onLeave, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.CallEnd,
                contentDescription = "Leave Voice",
                tint = BanterColors.Red,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun UserBar(
    displayName: String,
    username: String,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    HorizontalDivider(color = BanterColors.Divider)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BanterColors.ModalBackgroundDark)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BanterColors.Blurple)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Text(
                displayName.take(1).uppercase().ifEmpty { "?" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenSettings)
        ) {
            Text(
                displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BanterColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                username,
                fontSize = 12.sp,
                color = BanterColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = BanterColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onLogout, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Logout,
                contentDescription = "Logout",
                tint = BanterColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

fun presenceColor(state: String) = when (state.lowercase()) {
    "online" -> BanterColors.Online
    "idle" -> BanterColors.Idle
    else -> BanterColors.Offline
}
