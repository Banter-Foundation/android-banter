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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banter.android.data.model.Category
import com.banter.android.data.model.Channel
import com.banter.android.data.model.Dm
import com.banter.android.ui.screen.main.MainTab
import com.banter.android.ui.screen.main.MainUiState
import com.banter.android.ui.theme.BanterColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChannelSidebar(
    uiState: MainUiState,
    onChannelClick: (Long) -> Unit,
    onDmClick: (Long) -> Unit,
    onVoiceJoin: (Long) -> Unit,
    onVoiceLeave: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleDeafen: () -> Unit,
    onLogout: () -> Unit,
    onMemberListToggle: () -> Unit,
    onAddFriendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(BanterColors.ChannelSidebar)
    ) {
        // Header
        SidebarHeader(uiState, onAddFriendClick)

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.selectedServerId != null && uiState.serverLayout != null -> {
                    ServerChannelList(
                        layout = uiState.serverLayout!!,
                        selectedChannelId = uiState.selectedChannelId,
                        voiceMembers = uiState.voiceMembers,
                        inVoiceChannelId = uiState.inVoiceChannelId,
                        onChannelClick = onChannelClick,
                        onVoiceJoin = onVoiceJoin,
                        onVoiceLeave = onVoiceLeave,
                        presence = uiState.presence
                    )
                }
                uiState.activeTab == MainTab.HOME || uiState.activeTab == MainTab.DMS -> {
                    DmList(
                        dms = uiState.dms,
                        selectedDmId = uiState.selectedDmId,
                        presence = uiState.presence,
                        onDmClick = onDmClick
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

        // Voice bar
        if (uiState.inVoiceChannelId != null) {
            VoiceBar(
                isMuted = uiState.isMuted,
                isDeafened = uiState.isDeafened,
                onToggleMute = onToggleMute,
                onToggleDeafen = onToggleDeafen,
                onLeave = onVoiceLeave
            )
        }

        // User bar
        UserBar(
            displayName = uiState.currentDisplayName,
            username = uiState.currentUsername,
            userId = uiState.currentUserId,
            onLogout = onLogout
        )
    }
}

@Composable
private fun SidebarHeader(uiState: MainUiState, onAddFriendClick: () -> Unit) {
    val serverName = uiState.servers.find { it.id == uiState.selectedServerId }?.name
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(BanterColors.ChannelSidebar)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = serverName ?: when (uiState.activeTab) {
                    MainTab.HOME, MainTab.DMS -> "Direct Messages"
                    else -> "Banter"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BanterColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (uiState.activeTab == MainTab.HOME || uiState.activeTab == MainTab.DMS) {
                IconButton(onClick = onAddFriendClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Add Friend",
                        tint = BanterColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
    HorizontalDivider(color = BanterColors.Divider, thickness = 1.dp)
}

@Composable
private fun ServerChannelList(
    layout: com.banter.android.data.model.ServerLayout,
    selectedChannelId: Long?,
    voiceMembers: List<com.banter.android.data.model.VoiceMember>,
    inVoiceChannelId: Long?,
    presence: Map<Long, com.banter.android.data.model.PresenceEntry>,
    onChannelClick: (Long) -> Unit,
    onVoiceJoin: (Long) -> Unit,
    onVoiceLeave: () -> Unit
) {
    val categorized = layout.channels.groupBy { it.categoryId }
    val uncategorized = categorized[null] ?: emptyList()
    val categories = layout.categories.sortedBy { it.position }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        // Uncategorized channels
        items(uncategorized.filter { it.kind == "text" }) { channel ->
            ChannelRow(
                channel = channel,
                isSelected = channel.id == selectedChannelId,
                onClick = { onChannelClick(channel.id) }
            )
        }

        // Categories + their channels
        categories.forEach { category ->
            item(key = "cat_${category.id}") {
                CategoryRow(category)
            }
            val channelsInCat = (categorized[category.id] ?: emptyList()).sortedBy { it.position }
            items(channelsInCat, key = { it.id }) { channel ->
                when (channel.kind) {
                    "text", "board" -> ChannelRow(
                        channel = channel,
                        isSelected = channel.id == selectedChannelId,
                        onClick = { onChannelClick(channel.id) }
                    )
                    "voice" -> {
                        val members = voiceMembers.filter { it.channelId == channel.id }
                        VoiceChannelRow(
                            channel = channel,
                            members = members,
                            presence = presence,
                            inVoice = inVoiceChannelId == channel.id,
                            onClick = {
                                if (inVoiceChannelId == channel.id) onVoiceLeave()
                                else onVoiceJoin(channel.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 16.dp, bottom = 2.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null,
            tint = BanterColors.CategoryText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(2.dp))
        Text(
            text = category.name.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = BanterColors.CategoryText,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (channel.kind) {
        "board" -> Icons.Filled.Dashboard
        else -> Icons.Filled.Tag
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) BanterColors.ChannelRowActive else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) BanterColors.ChannelTextActive else BanterColors.ChannelText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
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
    members: List<com.banter.android.data.model.VoiceMember>,
    presence: Map<Long, com.banter.android.data.model.PresenceEntry>,
    inVoice: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 1.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (inVoice) BanterColors.ChannelRowActive else Color.Transparent)
                .clickable { onClick() }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.VolumeUp, contentDescription = null,
                tint = if (inVoice) BanterColors.VoiceConnected else BanterColors.ChannelText,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = channel.name,
                fontSize = 15.sp,
                color = if (inVoice) BanterColors.ChannelTextActive else BanterColors.ChannelText,
                fontWeight = if (inVoice) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (inVoice) {
                Icon(Icons.Filled.ExitToApp, contentDescription = "Leave",
                    tint = BanterColors.Red, modifier = Modifier.size(16.dp))
            }
        }
        // Voice members
        members.forEach { vm ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, end = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(BanterColors.ServerIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text("?", fontSize = 10.sp, color = Color.White)
                }
                Spacer(Modifier.width(6.dp))
                Text("User ${vm.userId}", fontSize = 13.sp, color = BanterColors.TextSecondary,
                    maxLines = 1)
                if (vm.muted) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.MicOff, contentDescription = "Muted",
                        tint = BanterColors.TextMuted, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun DmList(
    dms: List<Dm>,
    selectedDmId: Long?,
    presence: Map<Long, com.banter.android.data.model.PresenceEntry>,
    onDmClick: (Long) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        item {
            Text(
                "DIRECT MESSAGES",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BanterColors.CategoryText,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 8.dp)
            )
        }
        items(dms) { dm ->
            val presenceState = presence[dm.peerId]?.state ?: "offline"
            DmRow(
                dm = dm,
                isSelected = dm.id == selectedDmId,
                presenceState = presenceState,
                onClick = { onDmClick(dm.id) }
            )
        }
    }
}

@Composable
private fun DmRow(
    dm: Dm,
    isSelected: Boolean,
    presenceState: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) BanterColors.ChannelRowActive else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BanterColors.ServerIconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dm.peerDisplayName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            // Presence dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(BanterColors.ChannelSidebar)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(presenceColor(presenceState))
                    .align(Alignment.BottomEnd)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = dm.peerDisplayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) BanterColors.TextPrimary else BanterColors.ChannelText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            dm.lastMessage?.let { msg ->
                Text(
                    text = msg.content.take(30),
                    fontSize = 11.sp,
                    color = BanterColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.VolumeUp, contentDescription = null,
            tint = BanterColors.VoiceConnected, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Voice Connected", fontSize = 12.sp, color = BanterColors.VoiceConnected,
            modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleMute, modifier = Modifier.size(28.dp)) {
            Icon(
                if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = if (isMuted) BanterColors.Red else BanterColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(onClick = onToggleDeafen, modifier = Modifier.size(28.dp)) {
            Icon(
                if (isDeafened) Icons.Filled.HeadsetOff else Icons.Filled.Headset,
                contentDescription = if (isDeafened) "Undeafen" else "Deafen",
                tint = if (isDeafened) BanterColors.Red else BanterColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(onClick = onLeave, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.CallEnd, contentDescription = "Leave Voice",
                tint = BanterColors.Red, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun UserBar(
    displayName: String,
    username: String,
    userId: Long,
    onLogout: () -> Unit
) {
    HorizontalDivider(color = BanterColors.Divider)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BanterColors.ModalBackgroundDark)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BanterColors.Blurple),
            contentAlignment = Alignment.Center
        ) {
            Text(
                displayName.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = BanterColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(username, fontSize = 11.sp, color = BanterColors.TextMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onLogout, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Logout, contentDescription = "Logout",
                tint = BanterColors.TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

fun presenceColor(state: String) = when (state) {
    "online" -> BanterColors.Online
    "idle" -> BanterColors.Idle
    "dnd" -> BanterColors.Dnd
    else -> BanterColors.Offline
}
