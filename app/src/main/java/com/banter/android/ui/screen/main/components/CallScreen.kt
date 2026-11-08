package com.banter.android.ui.screen.main.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banter.android.data.model.MediaKind
import com.banter.android.data.model.VoiceFlags
import com.banter.android.data.model.VoiceMember
import com.banter.android.media.BanterRtcManager
import com.banter.android.media.CameraPreview
import com.banter.android.media.ScreenShareManager
import com.banter.android.media.ScreenSharePreview
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
    screenShareManager: ScreenShareManager,
    remoteFrames: Map<BanterRtcManager.RemoteStreamKey, Bitmap>,
    focusedStreamerId: Long?,
    liveStreamerIds: List<Long>,
    cameraFrontFacing: Boolean,
    mirrorCamera: Boolean,
    onCycleStreamer: () -> Unit,
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

            // Stage — featured remote Go Live / camera, or local preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val showingScreen = isScreenSharing || selfFlags.screen
                val showingCamera = selfFlags.camera
                val hasRemoteStream = remoteFrames.keys.any {
                    it.kind == MediaKind.Screen || it.kind == MediaKind.Camera
                }
                when {
                    phase == CallPhaseUi.Ringing && members.size <= 1 -> {
                        RingingAvatar(
                            name = members.firstOrNull()?.displayName ?: title,
                            userId = members.firstOrNull()?.userId ?: 0L
                        )
                    }
                    hasRemoteStream || liveStreamerIds.isNotEmpty() || showingScreen || showingCamera -> {
                        StreamStage(
                            showingScreen = showingScreen,
                            showingCamera = showingCamera,
                            screenShareManager = screenShareManager,
                            remoteFrames = remoteFrames,
                            focusedStreamerId = focusedStreamerId,
                            liveStreamerIds = liveStreamerIds,
                            cameraFrontFacing = cameraFrontFacing,
                            mirrorCamera = mirrorCamera,
                            members = members,
                            phase = phase,
                            onCycleStreamer = onCycleStreamer
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

@Composable
private fun StreamStage(
    showingScreen: Boolean,
    showingCamera: Boolean,
    screenShareManager: ScreenShareManager,
    remoteFrames: Map<BanterRtcManager.RemoteStreamKey, Bitmap>,
    focusedStreamerId: Long?,
    liveStreamerIds: List<Long>,
    cameraFrontFacing: Boolean,
    mirrorCamera: Boolean,
    members: List<CallParticipant>,
    phase: CallPhaseUi,
    onCycleStreamer: () -> Unit
) {
    val selfId = members.firstOrNull { it.isSelf }?.userId
    val focus = focusedStreamerId
        ?: liveStreamerIds.firstOrNull()
        ?: remoteFrames.keys.firstOrNull { it.kind == MediaKind.Screen }?.publisher
        ?: remoteFrames.keys.firstOrNull { it.kind == MediaKind.Camera }?.publisher

    val remoteScreen = focus?.let { remoteFrames[BanterRtcManager.RemoteStreamKey(it, MediaKind.Screen)] }
    val remoteCamera = focus?.let { remoteFrames[BanterRtcManager.RemoteStreamKey(it, MediaKind.Camera)] }
    val focusIsSelf = focus != null && focus == selfId
    val focusName = members.find { it.userId == focus }?.displayName
        ?: if (focus != null) "User $focus" else null

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // Featured remote (or self) Go Live
            remoteScreen != null && !remoteScreen.isRecycled -> {
                RemoteStreamFrame(
                    bitmap = remoteScreen,
                    label = "${focusName ?: "Stream"} · LIVE",
                    badgeColor = BanterColors.Fuchsia,
                    modifier = Modifier.fillMaxSize()
                )
            }
            focusIsSelf && showingScreen -> {
                ScreenSharePreview(
                    screenShareManager = screenShareManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Remote camera as main stage when no screen
            remoteCamera != null && !remoteCamera.isRecycled -> {
                RemoteStreamFrame(
                    bitmap = remoteCamera,
                    label = "${focusName ?: "Camera"}",
                    badgeColor = BanterColors.Blurple,
                    modifier = Modifier.fillMaxSize()
                )
            }
            showingScreen -> {
                ScreenSharePreview(
                    screenShareManager = screenShareManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
            showingCamera -> {
                CameraPreview(
                    frontFacing = cameraFrontFacing,
                    mirrored = mirrorCamera,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
                Text(
                    "Camera",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BanterColors.Blurple)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            liveStreamerIds.isNotEmpty() -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    CircularProgressIndicator(color = BanterColors.Fuchsia)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Loading ${focusName ?: "stream"}…",
                        color = BanterColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Local camera PiP while watching a stream
        if (showingCamera && (remoteScreen != null || showingScreen || remoteCamera != null)) {
            CameraPreview(
                frontFacing = cameraFrontFacing,
                mirrored = mirrorCamera,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .width(112.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        // Watch chrome: cycle when multiple live streamers
        if (liveStreamerIds.size > 1) {
            IconButton(
                onClick = onCycleStreamer,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next stream",
                    tint = Color.White
                )
            }
        }

        // Streamer avatars for quick focus
        if (liveStreamerIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                liveStreamerIds.take(5).forEach { id ->
                    val p = members.find { it.userId == id }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (id == focus) BanterColors.Fuchsia else avatarColor(id)
                            )
                            .clickable(onClick = onCycleStreamer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (p?.displayName ?: "?").take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            val others = members.filterNot { it.isSelf }
            if (others.isNotEmpty() && phase == CallPhaseUi.Active) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    others.take(3).forEach { p ->
                        // Prefer remote camera tile if present
                        val cam = remoteFrames[BanterRtcManager.RemoteStreamKey(p.userId, MediaKind.Camera)]
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(avatarColor(p.userId)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cam != null && !cam.isRecycled) {
                                Image(
                                    bitmap = cam.asImageBitmap(),
                                    contentDescription = p.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    p.displayName.take(1).uppercase().ifEmpty { "?" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteStreamFrame(
    bitmap: Bitmap,
    label: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            label.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(badgeColor)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

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
