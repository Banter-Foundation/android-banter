package com.banter.android.ui.screen.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.banter.android.data.model.GuildRole
import com.banter.android.data.model.Member
import com.banter.android.data.model.PresenceEntry
import com.banter.android.ui.theme.BanterColors

@Composable
fun MemberListPanel(
    members: List<Member>,
    roles: List<GuildRole>,
    presence: Map<Long, PresenceEntry>,
    currentUserId: Long,
    modifier: Modifier = Modifier
) {
    // Group members by their highest role
    val membersWithRoles = members.map { member ->
        val highestRole = member.roles
            .mapNotNull { roleId -> roles.find { it.id == roleId } }
            .maxByOrNull { it.position }
        Pair(member, highestRole)
    }

    val onlineMembers = membersWithRoles.filter { (m, _) ->
        (presence[m.id]?.state ?: "offline") != "offline"
    }.sortedWith(compareByDescending<Pair<Member, GuildRole?>> { it.second?.position ?: 0 }
        .thenBy { it.first.displayName })

    val offlineMembers = membersWithRoles.filter { (m, _) ->
        (presence[m.id]?.state ?: "offline") == "offline"
    }.sortedBy { it.first.displayName }

    LazyColumn(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(BanterColors.ChannelSidebar)
            .padding(vertical = 8.dp)
    ) {
        if (onlineMembers.isNotEmpty()) {
            item {
                Text(
                    "ONLINE — ${onlineMembers.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BanterColors.CategoryText,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 8.dp)
                )
            }
            items(onlineMembers) { (member, role) ->
                MemberRow(
                    member = member,
                    presenceState = presence[member.id]?.state ?: "online",
                    role = role,
                    isCurrentUser = member.id == currentUserId
                )
            }
        }

        if (offlineMembers.isNotEmpty()) {
            item {
                Text(
                    "OFFLINE — ${offlineMembers.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BanterColors.CategoryText,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 16.dp)
                )
            }
            items(offlineMembers) { (member, role) ->
                MemberRow(
                    member = member,
                    presenceState = "offline",
                    role = role,
                    isCurrentUser = member.id == currentUserId
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: Member,
    presenceState: String,
    role: GuildRole?,
    isCurrentUser: Boolean
) {
    val displayName = member.nickname ?: member.displayName

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(avatarColor(member.id).copy(
                        alpha = if (presenceState == "offline") 0.5f else 1f
                    )),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    displayName.take(1).uppercase(),
                    color = Color.White.copy(alpha = if (presenceState == "offline") 0.5f else 1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            // Presence dot
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
                Text(
                    text = role.name,
                    fontSize = 11.sp,
                    color = if (role.color != 0L)
                        Color(role.color.toInt() or (0xFF shl 24).toInt())
                    else BanterColors.TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}
