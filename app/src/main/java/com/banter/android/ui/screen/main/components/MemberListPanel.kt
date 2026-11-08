package com.banter.android.ui.screen.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.banter.android.data.model.Member
import com.banter.android.data.model.PresenceEntry
import com.banter.android.ui.theme.BanterColors

/** Full-screen members overlay (Discord mobile member list). */
@Composable
fun MemberListOverlay(
    members: List<Member>,
    presence: Map<Long, PresenceEntry>,
    currentUserId: Long,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClose)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.86f)
                .align(Alignment.CenterEnd)
                .background(BanterColors.ChannelSidebar)
                .clickable(enabled = false) {}
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Members — ${members.size}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BanterColors.TextPrimary,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = BanterColors.TextSecondary)
                }
            }
            HorizontalDivider(color = BanterColors.Divider)

            val online = members.filter {
                val state = presence[it.userId]?.state ?: it.presence ?: "offline"
                state != "offline"
            }.sortedBy { (it.nickname ?: it.displayName).lowercase() }

            val offline = members.filter {
                val state = presence[it.userId]?.state ?: it.presence ?: "offline"
                state == "offline"
            }.sortedBy { (it.nickname ?: it.displayName).lowercase() }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (online.isNotEmpty()) {
                    item {
                        Text(
                            "ONLINE — ${online.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BanterColors.CategoryText,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 8.dp)
                        )
                    }
                    items(online, key = { it.userId }) { member ->
                        MemberRow(
                            member = member,
                            presenceState = presence[member.userId]?.state ?: member.presence ?: "online",
                            isCurrentUser = member.userId == currentUserId
                        )
                    }
                }
                if (offline.isNotEmpty()) {
                    item {
                        Text(
                            "OFFLINE — ${offline.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BanterColors.CategoryText,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 16.dp)
                        )
                    }
                    items(offline, key = { it.userId }) { member ->
                        MemberRow(
                            member = member,
                            presenceState = "offline",
                            isCurrentUser = member.userId == currentUserId
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: Member, presenceState: String, isCurrentUser: Boolean) {
    val displayName = member.nickname ?: member.displayName
    val role = member.roles.filterNot { it.everyone }.maxByOrNull { it.position }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        avatarColor(member.userId).copy(
                            alpha = if (presenceState == "offline") 0.5f else 1f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    displayName.take(1).uppercase().ifEmpty { "?" },
                    color = Color.White.copy(alpha = if (presenceState == "offline") 0.5f else 1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(BanterColors.ChannelSidebar)
                    .align(Alignment.BottomEnd)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(presenceColor(presenceState))
                        .align(Alignment.Center)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Text(
                text = if (isCurrentUser) "$displayName (you)" else displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (presenceState == "offline") BanterColors.TextMuted else BanterColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (role != null) {
                val roleColor = if (role.color != 0L) {
                    Color((role.color.toInt() and 0xFFFFFF) or (0xFF shl 24))
                } else {
                    BanterColors.TextMuted
                }
                Text(role.name, fontSize = 11.sp, color = roleColor, maxLines = 1)
            }
        }
    }
}
