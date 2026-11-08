package com.banter.android.ui.screen.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banter.android.data.model.VoiceMember
import com.banter.android.ui.theme.BanterColors

@Composable
fun VoiceChannelView(
    channelName: String,
    members: List<VoiceMember>,
    isMuted: Boolean,
    isDeafened: Boolean,
    hasCamera: Boolean,
    onToggleMute: () -> Unit,
    onToggleDeafen: () -> Unit,
    onToggleCamera: () -> Unit,
    onLeave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BanterColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = BanterColors.VoiceConnected,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    channelName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BanterColors.TextPrimary
                )
            }
        }
        HorizontalDivider(color = BanterColors.Divider)

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (members.isEmpty()) {
                Text("Nobody here yet", color = BanterColors.TextMuted, fontSize = 16.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(members, key = { it.userId }) { member ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(avatarColor(member.userId)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                                if (member.flags.muted) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(BanterColors.Red),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.MicOff,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "User ${member.userId}",
                                fontSize = 12.sp,
                                color = BanterColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BanterColors.ModalBackgroundDark)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VoiceBtn(
                icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                danger = isMuted,
                onClick = onToggleMute
            )
            VoiceBtn(
                icon = if (isDeafened) Icons.Filled.HeadsetOff else Icons.Filled.Headset,
                label = if (isDeafened) "Undeafen" else "Deafen",
                danger = isDeafened,
                onClick = onToggleDeafen
            )
            VoiceBtn(
                icon = if (hasCamera) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                label = "Camera",
                danger = false,
                onClick = onToggleCamera
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BanterColors.Red),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onLeave) {
                    Icon(Icons.Filled.CallEnd, contentDescription = "Leave", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun VoiceBtn(
    icon: ImageVector,
    label: String,
    danger: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (danger) BanterColors.Red.copy(alpha = 0.2f) else BanterColors.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (danger) BanterColors.Red else BanterColors.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = BanterColors.TextSecondary)
    }
}
