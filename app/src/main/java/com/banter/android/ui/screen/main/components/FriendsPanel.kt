package com.banter.android.ui.screen.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
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
import com.banter.android.data.model.Friend
import com.banter.android.data.model.FriendRequest
import com.banter.android.data.model.PresenceEntry
import com.banter.android.ui.theme.BanterColors

enum class FriendsTab { ONLINE, ALL, PENDING }

@Composable
fun FriendsPanel(
    friends: List<Friend>,
    friendRequests: List<FriendRequest>,
    presence: Map<Long, PresenceEntry>,
    onOpenDm: (Long) -> Unit,
    onAcceptRequest: (Long) -> Unit,
    onDeclineRequest: (Long) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(FriendsTab.ONLINE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BanterColors.Background)
    ) {
        if (!compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.People,
                    contentDescription = null,
                    tint = BanterColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Friends",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BanterColors.TextPrimary
                )
            }
            HorizontalDivider(color = BanterColors.Divider)
        } else {
            Text(
                "FRIENDS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BanterColors.CategoryText,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FriendsTab.entries.forEach { t ->
                val selected = t == tab
                val label = when (t) {
                    FriendsTab.ONLINE -> "Online"
                    FriendsTab.ALL -> "All"
                    FriendsTab.PENDING ->
                        if (friendRequests.isNotEmpty()) "Pending (${friendRequests.size})"
                        else "Pending"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) BanterColors.ChannelRowActive else Color.Transparent)
                        .clickable { tab = t }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        color = if (selected) BanterColors.TextPrimary else BanterColors.TextSecondary,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            when (tab) {
                FriendsTab.ALL -> {
                    val sorted = friends.sortedBy { it.displayName.lowercase() }
                    items(sorted, key = { it.id }) { friend ->
                        FriendRow(
                            friend = friend,
                            presenceState = presence[friend.id]?.state ?: "offline",
                            customStatus = presence[friend.id]?.customStatus,
                            onOpenDm = onOpenDm
                        )
                    }
                }
                FriendsTab.ONLINE -> {
                    val online = friends.filter {
                        (presence[it.id]?.state ?: "offline") != "offline"
                    }.sortedBy { it.displayName.lowercase() }
                    if (online.isEmpty()) {
                        item {
                            Text(
                                "No friends online right now",
                                color = BanterColors.TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(online, key = { it.id }) { friend ->
                        FriendRow(
                            friend = friend,
                            presenceState = presence[friend.id]?.state ?: "online",
                            customStatus = presence[friend.id]?.customStatus,
                            onOpenDm = onOpenDm
                        )
                    }
                }
                FriendsTab.PENDING -> {
                    if (friendRequests.isEmpty()) {
                        item {
                            Text(
                                "No pending friend requests",
                                color = BanterColors.TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(friendRequests, key = { it.id }) { req ->
                        FriendRequestRow(
                            request = req,
                            onAccept = { onAcceptRequest(req.id) },
                            onDecline = { onDeclineRequest(req.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRow(
    friend: Friend,
    presenceState: String,
    customStatus: String?,
    onOpenDm: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpenDm(friend.id) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(avatarColor(friend.id)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    friend.displayName.take(1).uppercase().ifEmpty { "?" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(BanterColors.Background)
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(presenceColor(presenceState))
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                friend.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BanterColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                customStatus ?: presenceState.replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                color = BanterColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { onOpenDm(friend.id) }) {
            Icon(
                Icons.Filled.Message,
                contentDescription = "Message",
                tint = BanterColors.TextSecondary
            )
        }
    }
}

@Composable
private fun FriendRequestRow(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor(request.fromId)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                request.fromUsername.take(1).uppercase().ifEmpty { "?" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                request.fromUsername,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BanterColors.TextPrimary
            )
            Text("Incoming Friend Request", fontSize = 12.sp, color = BanterColors.TextSecondary)
        }

        IconButton(onClick = onAccept) {
            Icon(Icons.Filled.Check, contentDescription = "Accept", tint = BanterColors.Green)
        }
        IconButton(onClick = onDecline) {
            Icon(Icons.Filled.Close, contentDescription = "Decline", tint = BanterColors.Red)
        }
    }
}
