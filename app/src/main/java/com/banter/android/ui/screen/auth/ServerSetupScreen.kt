package com.banter.android.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.banter.android.ui.theme.BanterColors

@Composable
fun ServerSetupScreen(
    onDone: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var serverUrl by remember { mutableStateOf("http://127.0.0.1:3970") }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo / App name
            Text(
                text = "Banter",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BanterColors.Blurple
            )
            Text(
                text = "Connect to your Banter server",
                fontSize = 16.sp,
                color = BanterColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BanterColors.ModalBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Server URL",
                        fontWeight = FontWeight.SemiBold,
                        color = BanterColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("http://127.0.0.1:3970", color = BanterColors.TextMuted) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BanterColors.Blurple,
                            unfocusedBorderColor = BanterColors.Divider,
                            focusedTextColor = BanterColors.TextPrimary,
                            unfocusedTextColor = BanterColors.TextPrimary,
                            cursorColor = BanterColors.Blurple,
                            focusedContainerColor = BanterColors.InputBackground,
                            unfocusedContainerColor = BanterColors.InputBackground
                        )
                    )

                    Button(
                        onClick = {
                            val url = serverUrl.trim().ifEmpty { "http://127.0.0.1:3970" }
                            viewModel.saveServerUrl(url)
                            onDone()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BanterColors.Blurple)
                    ) {
                        Text("Continue", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            TextButton(onClick = onDone) {
                Text("Use local server (127.0.0.1:3970)", color = BanterColors.TextLink, fontSize = 14.sp)
            }
        }
    }
}
