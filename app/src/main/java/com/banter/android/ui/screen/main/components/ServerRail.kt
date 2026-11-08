package com.banter.android.ui.screen.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banter.android.data.model.ServerSummary
import com.banter.android.ui.screen.main.MainTab
import com.banter.android.ui.theme.BanterColors

@Composable
fun ServerRail(
    servers: List<ServerSummary>,
    selectedServerId: Long?,
    activeTab: MainTab,
    onServerClick: (Long) -> Unit,
    onHomeClick: () -> Unit,
    onAddServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val homeActive = activeTab == MainTab.HOME

    Column(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(BanterColors.ServerRail)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RailIcon(
            selected = homeActive,
            onClick = onHomeClick,
            bg = if (homeActive) BanterColors.Blurple else BanterColors.ServerIconBg
        ) {
            Icon(
                Icons.Filled.Home,
                contentDescription = "Home",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        RailDivider()

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(servers, key = { it.id }) { server ->
                val selected = server.id == selectedServerId
                RailIcon(
                    selected = selected,
                    onClick = { onServerClick(server.id) },
                    bg = if (selected) BanterColors.ServerIconBgActive else BanterColors.ServerIconBg
                ) {
                    Text(
                        text = serverInitials(server.name),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                RailIcon(
                    selected = false,
                    onClick = onAddServerClick,
                    bg = BanterColors.ServerIconBg
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add Server",
                        tint = BanterColors.Green,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RailIcon(
    selected: Boolean,
    onClick: () -> Unit,
    bg: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (selected) 36.dp else 0.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(Color.White)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(if (selected) RoundedCornerShape(16.dp) else CircleShape)
                .background(bg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
private fun RailDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .width(32.dp)
            .height(2.dp)
            .background(BanterColors.Divider, RoundedCornerShape(1.dp))
    )
}

private fun serverInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
