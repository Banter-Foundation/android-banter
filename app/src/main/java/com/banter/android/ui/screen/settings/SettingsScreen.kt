package com.banter.android.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.banter.android.ui.theme.BanterColors

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var serverDraft by remember(state.serverUrl) { mutableStateOf(state.serverUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BanterColors.BackgroundTertiary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BanterColors.BackgroundSecondary)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BanterColors.TextPrimary
                )
            }
            Text(
                "Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BanterColors.TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("My Account") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(BanterColors.Blurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            state.displayName.take(1).uppercase().ifEmpty { "?" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            state.displayName.ifBlank { "—" },
                            color = BanterColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            "@${state.username.ifBlank { "—" }}",
                            color = BanterColors.TextMuted,
                            fontSize = 13.sp
                        )
                        Text(
                            "ID ${state.userId}",
                            color = BanterColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            SettingsSection("Server") {
                Text(
                    "Banter host URL",
                    color = BanterColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverDraft,
                    onValueChange = { serverDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BanterColors.TextPrimary,
                        unfocusedTextColor = BanterColors.TextPrimary,
                        focusedBorderColor = BanterColors.Blurple,
                        unfocusedBorderColor = BanterColors.Divider,
                        cursorColor = BanterColors.Blurple,
                        focusedContainerColor = BanterColors.InputBackground,
                        unfocusedContainerColor = BanterColors.InputBackground
                    )
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.saveServerUrl(serverDraft) },
                    colors = ButtonDefaults.buttonColors(containerColor = BanterColors.Blurple),
                    enabled = serverDraft.trim().trimEnd('/') != state.serverUrl
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save server")
                }
                if (state.savedMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(state.savedMessage!!, color = BanterColors.VoiceConnected, fontSize = 12.sp)
                }
                Text(
                    "Changing host takes effect on next login / reconnect.",
                    color = BanterColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            SettingsSection("Voice & Video") {
                SettingsToggle(
                    title = "Front camera",
                    subtitle = "Use the front-facing camera for video calls",
                    checked = state.cameraFrontFacing,
                    onCheckedChange = viewModel::setCameraFrontFacing
                )
                HorizontalDivider(color = BanterColors.Divider, modifier = Modifier.padding(vertical = 8.dp))
                SettingsToggle(
                    title = "Mirror my video",
                    subtitle = "Flip the local camera preview horizontally",
                    checked = state.mirrorCamera,
                    onCheckedChange = viewModel::setMirrorCamera
                )
                HorizontalDivider(color = BanterColors.Divider, modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.setCameraFrontFacing(!state.cameraFrontFacing) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Cameraswitch,
                        contentDescription = null,
                        tint = BanterColors.TextSecondary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (state.cameraFrontFacing) "Currently using front camera"
                        else "Currently using rear camera",
                        color = BanterColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            SettingsSection("About") {
                Text("Banter for Android", color = BanterColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Version ${state.appVersion}", color = BanterColors.TextMuted, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    viewModel.logout { onLogout() }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BanterColors.Red)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Log Out")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title.uppercase(),
            color = BanterColors.TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BanterColors.BackgroundSecondary)
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = BanterColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = BanterColors.TextMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BanterColors.Blurple,
                uncheckedThumbColor = BanterColors.TextSecondary,
                uncheckedTrackColor = BanterColors.Surface
            )
        )
    }
}
