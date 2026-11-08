package com.banter.android.ui.screen.main.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banter.android.data.model.VoiceFlags
import com.banter.android.data.model.VoiceMember
import com.banter.android.ui.theme.BanterColors

enum class CallKind { VoiceChannel, DmCall }

enum class CallPhaseUi { Connecting, Ringing, Active }

/**
 * Discord-mobile phone-call stage for guild voice channels and DM calls.
 */
@Composable
fun CallScreen(
    title: String,
    subtitle: String,
    kind: CallKind,
    phase: CallPhaseUi,
    members: List<CallParticipant>,
    selfFlags: VoiceFlags,
    isScreenSharing: Boolean,
    onToggleMute: () -> Unit,
    onToggleDeafen: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onHangUp: () -> Unit,
    onAccept: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1B1E), Color(0xFF2B2D31), Color(0xFF1E1F22))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMinimize) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = BanterColors.TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BanterColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        when (phase) {
                            CallPhaseUi.Ringing -> if (kind == CallKind.DmCall) "Ringing…" else "Connecting…"
                            CallPhaseUi.Connecting -> "Connecting…"
                            CallPhaseUi.Active -> subtitle
                        },
                        fontSize = 13.sp,
                        color = when (phase) {
                            CallPhaseUi.Active -> BanterColors.VoiceConnected
                            else -> BanterColors.TextSecondary
                        }
                    )
                }
                Spacer(Modifier.size(48.dp))
            }

            // Stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    phase == CallPhaseUi.Ringing && members.size <= 1 -> {
                        RingingAvatar(
                            name = members.firstOrNull()?.displayName ?: title,
                            userId = members.firstOrNull()?.userId ?: 0L
                        )
                    }
                    members.isEmpty() -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Call,
                                contentDescription = null,
                                tint = BanterColors.TextMuted,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (kind == CallKind.VoiceChannel) "Waiting for others to join…"
                                else "Calling…",
                                color = BanterColors.TextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                    members.size == 1 -> {
                        val p = members.first()
                        LargeParticipant(p, phase == CallPhaseUi.Active)
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 140.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(members, key = { it.userId }) { p ->
                                ParticipantTile(p)
                            }
                        }
                    }
                }
            }

            // Controls
            if (phase == CallPhaseUi.Ringing && onAccept != null && onDecline != null) {
                IncomingControls(onAccept = onAccept, onDecline = onDecline)
            } else {
                ActiveControls(
                    muted = selfFlags.muted,
                    deafened = selfFlags.deafened,
                    camera = selfFlags.camera,
                    screen = isScreenSharing || selfFlags.screen,
                    onToggleMute = onToggleMute,
                    onToggleDeafen = onToggleDeafen,
                    onToggleCamera = onToggleCamera,
                    onToggleScreenShare = onToggleScreenShare,
                    onHangUp = onHangUp
                )
            }
        }
    }
}

data class CallParticipant(
    val userId: Long,
    val displayName: String,
    val flags: VoiceFlags = VoiceFlags(),
    val isSelf: Boolean = false
)

fun VoiceMember.toParticipant(name: String? = null, isSelf: Boolean = false) =
    CallParticipant(
        userId = userId,
        displayName = name ?: "User $userId",
        flags = flags,
        isSelf = isSelf
    )

@Composable
private fun RingingAvatar(name: String, userId: Long) {
    val infinite = rememberInfiniteTransition(label = "ring")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(avatarColor(userId)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1).uppercase().ifEmpty { "?" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            name,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BanterColors.TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text("Incoming call", color = BanterColors.TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun LargeParticipant(p: CallParticipant, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(avatarColor(p.userId)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                p.displayName.take(1).uppercase().ifEmpty { "?" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp
            )
            FlagBadges(p.flags, Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            p.displayName + if (p.isSelf) " (You)" else "",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = BanterColors.TextPrimary
        )
        if (p.flags.screen) {
            Spacer(Modifier.height(6.dp))
            Text("Go Live", color = BanterColors.Fuchsia, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        } else if (active) {
            Spacer(Modifier.height(6.dp))
            Text("In call", color = BanterColors.VoiceConnected, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ParticipantTile(p: CallParticipant) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BanterColors.ModalBackgroundDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(avatarColor(p.userId)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                p.displayName.take(1).uppercase().ifEmpty { "?" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            FlagBadges(p.flags, Modifier.align(Alignment.BottomEnd))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            p.displayName + if (p.isSelf) " (You)" else "",
            fontSize = 13.sp,
            color = BanterColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (p.flags.screen) {
            Text("Go Live", fontSize = 11.sp, color = BanterColors.Fuchsia)
        }
    }
}

@Composable
private fun FlagBadges(flags: VoiceFlags, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (flags.muted) {
            BadgeIcon(Icons.Filled.MicOff, BanterColors.Red)
        }
        if (flags.deafened) {
            BadgeIcon(Icons.Filled.HeadsetOff, BanterColors.Red)
        }
        if (flags.camera) {
            BadgeIcon(Icons.Filled.Videocam, BanterColors.Green)
        }
        if (flags.screen) {
            BadgeIcon(Icons.Filled.PresentToAll, BanterColors.Fuchsia)
        }
    }
}

@Composable
private fun BadgeIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun IncomingControls(onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CallRoundButton(
            icon = Icons.Filled.CallEnd,
            label = "Decline",
            bg = BanterColors.Red,
            onClick = onDecline,
            large = true
        )
        CallRoundButton(
            icon = Icons.Filled.Call,
            label = "Accept",
            bg = BanterColors.Green,
            onClick = onAccept,
            large = true
        )
    }
}

@Composable
private fun ActiveControls(
    muted: Boolean,
    deafened: Boolean,
    camera: Boolean,
    screen: Boolean,
    onToggleMute: () -> Unit,
    onToggleDeafen: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onHangUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CallRoundButton(
            icon = if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
            label = if (muted) "Unmute" else "Mute",
            bg = if (muted) BanterColors.Red else BanterColors.SurfaceRaised,
            onClick = onToggleMute
        )
        CallRoundButton(
            icon = if (deafened) Icons.Filled.HeadsetOff else Icons.Filled.Headset,
            label = if (deafened) "Undeafen" else "Deafen",
            bg = if (deafened) BanterColors.Red else BanterColors.SurfaceRaised,
            onClick = onToggleDeafen
        )
        CallRoundButton(
            icon = if (camera) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
            label = "Video",
            bg = if (camera) BanterColors.Blurple else BanterColors.SurfaceRaised,
            onClick = onToggleCamera
        )
        CallRoundButton(
            icon = Icons.Filled.PresentToAll,
            label = if (screen) "Stop Share" else "Share",
            bg = if (screen) BanterColors.Fuchsia else BanterColors.SurfaceRaised,
            onClick = onToggleScreenShare
        )
        CallRoundButton(
            icon = Icons.Filled.CallEnd,
            label = "Leave",
            bg = BanterColors.Red,
            onClick = onHangUp,
            large = true
        )
    }
}

@Composable
private fun CallRoundButton(
    icon: ImageVector,
    label: String,
    bg: Color,
    onClick: () -> Unit,
    large: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (large) 64.dp else 52.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(if (large) 28.dp else 22.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = BanterColors.TextSecondary, maxLines = 1)
    }
}
