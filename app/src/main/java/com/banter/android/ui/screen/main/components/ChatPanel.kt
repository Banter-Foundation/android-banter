package com.banter.android.ui.screen.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tag
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.banter.android.data.api.WebSocketManager
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
    serverId: Long?,
    currentUserId: Long,
    onMemberListToggle: () -> Unit,
    webSocketManager: WebSocketManager,
    viewModel: ChatViewModel = hiltViewModel()
) {
    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId, isDm = false)
    }

    LaunchedEffect(webSocketManager) {
        webSocketManager.events.collect { event ->
            when (event) {
                is ServerEvent.NewMessage -> {
                    if (event.message.channelId == channelId) {
                        viewModel.onNewMessage(event.message)
                    }
                }
                is ServerEvent.MessageEdited -> viewModel.onMessageEdited(event.id, event.content)
                is ServerEvent.MessageDeleted -> viewModel.onMessageDeleted(event.id)
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        ChatTopBar(
            title = "#${uiState.channelName}",
            onMemberListToggle = onMemberListToggle
        )

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(BanterColors.Background),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BanterColors.Blurple)
                    }
                }
            }
            items(uiState.messages, key = { it.id }) { message ->
                MessageRow(
                    message = message,
                    isMine = message.authorId == currentUserId,
                    prevMessage = uiState.messages.getOrNull(uiState.messages.indexOf(message) - 1)
                )
            }
        }

        // Input
        ChatInput(
            placeholder = "#${uiState.channelName}",
            onSend = { viewModel.sendMessage(it) }
        )
    }
}

@Composable
fun DmChatPanel(
    dmId: Long,
    currentUserId: Long,
    webSocketManager: WebSocketManager,
    viewModel: ChatViewModel = hiltViewModel()
) {
    LaunchedEffect(dmId) {
        viewModel.loadChannel(dmId, isDm = true)
    }

    LaunchedEffect(webSocketManager) {
        webSocketManager.events.collect { event ->
            when (event) {
                is ServerEvent.NewMessage -> {
                    if (event.message.dmId == dmId) {
                        viewModel.onNewMessage(event.message)
                    }
                }
                is ServerEvent.MessageEdited -> viewModel.onMessageEdited(event.id, event.content)
                is ServerEvent.MessageDeleted -> viewModel.onMessageDeleted(event.id)
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

    Column(modifier = Modifier.fillMaxSize()) {
        ChatTopBar(title = uiState.channelName, onMemberListToggle = {})

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().background(BanterColors.Background),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BanterColors.Blurple)
                    }
                }
            }
            items(uiState.messages, key = { it.id }) { message ->
                MessageRow(
                    message = message,
                    isMine = message.authorId == currentUserId,
                    prevMessage = uiState.messages.getOrNull(uiState.messages.indexOf(message) - 1)
                )
            }
        }

        ChatInput(
            placeholder = uiState.channelName,
            onSend = { viewModel.sendMessage(it) }
        )
    }
}

@Composable
private fun ChatTopBar(title: String, onMemberListToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(BanterColors.Background)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Tag, contentDescription = null,
            tint = BanterColors.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BanterColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onMemberListToggle) {
            Icon(Icons.Filled.People, contentDescription = "Member List",
                tint = BanterColors.TextSecondary)
        }
    }
    HorizontalDivider(color = BanterColors.Divider)
}

@Composable
fun MessageRow(message: Message, isMine: Boolean, prevMessage: Message?) {
    val showHeader = prevMessage == null
            || prevMessage.authorId != message.authorId
            || (message.sentAt - prevMessage.sentAt) > 5 * 60 * 1000

    if (showHeader) {
        Spacer(Modifier.height(16.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = if (showHeader) 0.dp else 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (showHeader) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(avatarColor(message.authorId)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    message.author.displayName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            Spacer(Modifier.width(40.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column {
            if (showHeader) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = message.author.displayName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = BanterColors.TextPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(message.sentAt),
                        fontSize = 11.sp,
                        color = BanterColors.TextMuted
                    )
                }
            }

            // Reply
            message.replyTo?.let { reply ->
                Row(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .background(BanterColors.MessageHover, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Reply, contentDescription = null,
                        tint = BanterColors.TextMuted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(reply.author.displayName, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = BanterColors.TextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text(reply.content.take(50), fontSize = 12.sp, color = BanterColors.TextMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Message content
            Text(
                text = message.content,
                fontSize = 15.sp,
                color = BanterColors.TextPrimary,
                lineHeight = 22.sp
            )

            // Edited indicator
            if (message.editedAt != null) {
                Text("(edited)", fontSize = 10.sp, color = BanterColors.TextMuted)
            }

            // Attachments
            message.attachments.forEach { attachment ->
                AttachmentChip(attachment)
            }
        }
    }
}

@Composable
private fun AttachmentChip(attachment: com.banter.android.data.model.Attachment) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .background(BanterColors.Surface, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AttachFile, contentDescription = null,
            tint = BanterColors.Blurple, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(attachment.filename, fontSize = 13.sp, color = BanterColors.TextLink,
                fontWeight = FontWeight.Medium)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(BanterColors.InputBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (text.isEmpty()) {
                Text("Message $placeholder", color = BanterColors.TextMuted, fontSize = 14.sp)
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = BanterColors.TextPrimary,
                    fontSize = 14.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 8
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
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Filled.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) BanterColors.Blurple else BanterColors.TextMuted
            )
        }
    }
}

private val dateFormat = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())
private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
private val calendar = Calendar.getInstance()

fun formatTimestamp(ms: Long): String {
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
    return colors[(userId % colors.size).toInt()]
}
