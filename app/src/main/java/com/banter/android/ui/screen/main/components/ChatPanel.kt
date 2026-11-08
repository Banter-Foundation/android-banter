package com.banter.android.ui.screen.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.banter.android.bfmd.BfmdText
import com.banter.android.data.api.WebSocketManager
import com.banter.android.data.model.Attachment
import com.banter.android.data.model.Message
import com.banter.android.data.model.ServerEvent
import com.banter.android.ui.screen.main.chat.ChatViewModel
import com.banter.android.ui.theme.BanterColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatPanel(
    channelId: Long,
    channelName: String,
    currentUserId: Long,
    isDm: Boolean,
    onBack: () -> Unit,
    onMemberListToggle: () -> Unit,
    showMemberButton: Boolean,
    onStartCall: (() -> Unit)? = null,
    webSocketManager: WebSocketManager,
    viewModel: ChatViewModel = hiltViewModel()
) {
    LaunchedEffect(channelId, isDm) {
        viewModel.loadChannel(channelId, isDm = isDm, name = channelName)
    }

    LaunchedEffect(channelId, isDm, webSocketManager) {
        webSocketManager.events.collect { event ->
            when (event) {
                is ServerEvent.MessageCreated -> {
                    val matches = if (isDm) {
                        event.target.type == "direct" && event.target.id == channelId
                    } else {
                        event.target.type == "channel" && event.target.id == channelId
                    }
                    if (matches) viewModel.onNewMessage(event.message)
                }
                is ServerEvent.MessageEdited -> {
                    val matches = if (isDm) {
                        event.target.type == "direct" && event.target.id == channelId
                    } else {
                        event.target.type == "channel" && event.target.id == channelId
                    }
                    if (matches) viewModel.onMessageEdited(event.message)
                }
                is ServerEvent.MessageDeleted -> {
                    val matches = if (isDm) {
                        event.target.type == "direct" && event.target.id == channelId
                    } else {
                        event.target.type == "channel" && event.target.id == channelId
                    }
                    if (matches) viewModel.onMessageDeleted(event.messageId)
                }
                else -> {}
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(uiState.messages.size - 1) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BanterColors.Background)
    ) {
        ChatTopBar(
            title = if (isDm) channelName else "#$channelName",
            isDm = isDm,
            onBack = onBack,
            showMemberButton = showMemberButton,
            onMemberListToggle = onMemberListToggle,
            onStartCall = onStartCall
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                uiState.isLoading && uiState.messages.isEmpty() -> {
                    CircularProgressIndicator(
                        color = BanterColors.Blurple,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null && uiState.messages.isEmpty() -> {
                    Text(
                        text = uiState.error ?: "Error",
                        color = BanterColors.TextDanger,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }
                uiState.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (isDm) "This is the beginning of your direct message history with $channelName."
                            else "Welcome to #$channelName!",
                            color = BanterColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Send a message to get the conversation started.",
                            color = BanterColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp, top = 4.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            val index = uiState.messages.indexOf(message)
                            MessageRow(
                                message = message,
                                prevMessage = uiState.messages.getOrNull(index - 1)
                            )
                        }
                    }
                }
            }
        }

        ChatInput(
            placeholder = if (isDm) "@$channelName" else "#$channelName",
            onSend = { viewModel.sendMessage(it) }
        )
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    isDm: Boolean,
    onBack: () -> Unit,
    showMemberButton: Boolean,
    onMemberListToggle: () -> Unit,
    onStartCall: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(BanterColors.Background)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = BanterColors.TextPrimary
            )
        }
        if (!isDm) {
            Icon(
                Icons.Filled.Tag,
                contentDescription = null,
                tint = BanterColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BanterColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onStartCall != null) {
            IconButton(onClick = onStartCall) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "Start Call",
                    tint = BanterColors.TextSecondary
                )
            }
        }
        if (showMemberButton) {
            IconButton(onClick = onMemberListToggle) {
                Icon(
                    Icons.Filled.People,
                    contentDescription = "Members",
                    tint = BanterColors.TextSecondary
                )
            }
        }
    }
    HorizontalDivider(color = BanterColors.Divider)
}

@Composable
fun MessageRow(message: Message, prevMessage: Message?) {
    val showHeader = prevMessage == null
            || prevMessage.authorId != message.authorId
            || (message.createdAt - prevMessage.createdAt) > 5 * 60 * 1000

    if (showHeader) Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (showHeader) 2.dp else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (showHeader) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(avatarColor(message.authorId)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    message.displayAuthor.take(1).uppercase().ifEmpty { "?" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            Spacer(Modifier.width(40.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (showHeader) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = message.displayAuthor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = BanterColors.TextPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(message.createdAt),
                        fontSize = 11.sp,
                        color = BanterColors.TextMuted
                    )
                }
            }

            BfmdText(
                source = message.body,
                defaultColor = BanterColors.TextPrimary,
                defaultSize = 15.sp,
                lineHeight = 22.sp
            )

            if (message.editedAt != null) {
                Text("(edited)", fontSize = 10.sp, color = BanterColors.TextMuted)
            }

            message.attachments.forEach { attachment ->
                AttachmentChip(attachment)
            }
        }
    }
}

@Composable
private fun AttachmentChip(attachment: Attachment) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .background(BanterColors.Surface, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.AttachFile,
            contentDescription = null,
            tint = BanterColors.Blurple,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                attachment.filename,
                fontSize = 13.sp,
                color = BanterColors.TextLink,
                fontWeight = FontWeight.Medium
            )
            Text(formatFileSize(attachment.byteSize), fontSize = 11.sp, color = BanterColors.TextMuted)
        }
    }
}

@Composable
fun ChatInput(placeholder: String, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BanterColors.Background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(BanterColors.InputBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (text.isEmpty()) {
                Text("Message $placeholder", color = BanterColors.TextMuted, fontSize = 15.sp)
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = BanterColors.TextPrimary,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(BanterColors.Blurple),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 6
            )
        }

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = {
                val trimmed = text.trim()
                if (trimmed.isNotEmpty()) {
                    onSend(trimmed)
                    text = ""
                }
            },
            enabled = text.isNotBlank(),
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (text.isNotBlank()) BanterColors.Blurple else BanterColors.Surface
                )
        ) {
            Icon(
                Icons.Filled.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) Color.White else BanterColors.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private val dateFormat = SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.getDefault())
private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

fun formatTimestamp(ms: Long): String {
    if (ms <= 0L) return ""
    val msgCal = Calendar.getInstance().apply { timeInMillis = ms }
    val now = Calendar.getInstance()
    return if (msgCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        && msgCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    ) {
        "Today at ${timeFormat.format(Date(ms))}"
    } else {
        dateFormat.format(Date(ms))
    }
}

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}

fun avatarColor(userId: Long): Color {
    val colors = listOf(
        Color(0xFF5865F2), Color(0xFF57F287), Color(0xFFFEE75C),
        Color(0xFFEB459E), Color(0xFFED4245), Color(0xFF3498DB)
    )
    val idx = ((userId % colors.size) + colors.size) % colors.size
    return colors[idx.toInt()]
}
