package com.banter.android.ui.screen.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.banter.android.ui.screen.auth.banterTextFieldColors
import com.banter.android.ui.theme.BanterColors

@Composable
fun CreateServerDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onJoinWithCode: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BanterColors.ModalBackground)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Create a Server", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    color = BanterColors.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Give your server a name. You can always change it later.",
                    fontSize = 14.sp, color = BanterColors.TextSecondary)
                Spacer(Modifier.height(16.dp))

                Text("SERVER NAME", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = BanterColors.TextSecondary, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("My server", color = BanterColors.TextMuted) },
                    colors = banterTextFieldColors()
                )

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onJoinWithCode, modifier = Modifier.weight(1f)) {
                        Text("Join with code", color = BanterColors.TextLink)
                    }
                    Button(
                        onClick = { if (name.isNotBlank()) onCreate(name) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BanterColors.Blurple)
                    ) {
                        Text("Create", color = Color.White)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = BanterColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun JoinServerDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BanterColors.ModalBackground)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Join a Server", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    color = BanterColors.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Enter an invite code to join an existing server.",
                    fontSize = 14.sp, color = BanterColors.TextSecondary)
                Spacer(Modifier.height(16.dp))

                Text("INVITE CODE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = BanterColors.TextSecondary, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("abc123", color = BanterColors.TextMuted) },
                    colors = banterTextFieldColors()
                )

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = BanterColors.TextSecondary)
                    }
                    Button(
                        onClick = { if (code.isNotBlank()) onJoin(code.trim()) },
                        enabled = code.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BanterColors.Blurple)
                    ) {
                        Text("Join", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddFriendDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var username by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BanterColors.ModalBackground)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Add Friend", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    color = BanterColors.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Enter their exact Banter username.",
                    fontSize = 14.sp, color = BanterColors.TextSecondary)
                Spacer(Modifier.height(16.dp))

                Text("USERNAME", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = BanterColors.TextSecondary, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("username", color = BanterColors.TextMuted) },
                    colors = banterTextFieldColors()
                )

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = BanterColors.TextSecondary)
                    }
                    Button(
                        onClick = { if (username.isNotBlank()) onSend(username.trim()) },
                        enabled = username.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BanterColors.Blurple)
                    ) {
                        Text("Send Request", color = Color.White)
                    }
                }
            }
        }
    }
}
