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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.banter.android.ui.theme.BanterColors

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val passwordFocus = FocusRequester()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            viewModel.clearState()
            onLoginSuccess()
        }
    }

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
            Text(
                text = "Welcome back!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BanterColors.TextPrimary
            )
            Text(
                text = "Sign in to continue",
                fontSize = 14.sp,
                color = BanterColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BanterColors.ModalBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Username
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("USERNAME", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = BanterColors.TextSecondary, letterSpacing = 0.5.sp)
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                            colors = banterTextFieldColors()
                        )
                    }

                    // Password
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("PASSWORD", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = BanterColors.TextSecondary, letterSpacing = 0.5.sp)
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(passwordFocus),
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None
                                else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                viewModel.login(username, password)
                            }),
                            trailingIcon = {
                                TextButton(onClick = { showPassword = !showPassword }) {
                                    Text(
                                        if (showPassword) "Hide" else "Show",
                                        color = BanterColors.TextLink,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            colors = banterTextFieldColors()
                        )
                    }

                    uiState.error?.let { err ->
                        Text(err, color = BanterColors.TextDanger, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login(username, password)
                        },
                        enabled = !uiState.isLoading && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BanterColors.Blurple)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Log In", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Need an account? ", color = BanterColors.TextSecondary, fontSize = 14.sp)
                TextButton(onClick = onNavigateToRegister, contentPadding = PaddingValues(0.dp)) {
                    Text("Register", color = BanterColors.TextLink, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun banterTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BanterColors.Blurple,
    unfocusedBorderColor = BanterColors.Divider,
    focusedTextColor = BanterColors.TextPrimary,
    unfocusedTextColor = BanterColors.TextPrimary,
    cursorColor = BanterColors.Blurple,
    focusedContainerColor = BanterColors.InputBackground,
    unfocusedContainerColor = BanterColors.InputBackground
)
