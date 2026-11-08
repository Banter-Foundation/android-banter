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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.banter.android.data.model.ServerRailEntry
import com.banter.android.ui.screen.main.MainTab
import com.banter.android.ui.theme.BanterColors

@Composable
fun ServerRail(
    servers: List<ServerRailEntry>,
    selectedServerId: Long?,
    activeTab: MainTab,
    onServerClick: (Long) -> Unit,
    onHomeClick: () -> Unit,
    onDmsClick: () -> Unit,
    onAddServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(BanterColors.ServerRail)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Home button
        RailPill(active = activeTab == MainTab.HOME || activeTab == MainTab.DMS)
        RailIconButton(
            isActive = activeTab == MainTab.HOME || activeTab == MainTab.DMS,
            onClick = onHomeClick
        ) {
            Icon(Icons.Filled.Home, contentDescription = "Home", tint = Color.White,
                modifier = Modifier.size(24.dp))
        }

        RailDivider()

        // Server list
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(servers) { server ->
                val isSelected = server.id == selectedServerId
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RailPill(active = isSelected)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(if (isSelected) RoundedCornerShape(16.dp) else CircleShape)
                            .background(if (isSelected) BanterColors.ServerIconBgActive else BanterColors.ServerIconBg)
                            .clickable { onServerClick(server.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = server.name.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                RailPill(active = false)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BanterColors.ServerIconBg)
                        .clickable { onAddServerClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Server",
                        tint = BanterColors.Green, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Bottom: DMs icon
        RailDivider()
        RailPill(active = false)
        RailIconButton(
            isActive = false,
            onClick = onDmsClick
        ) {
            Icon(Icons.Filled.Chat, contentDescription = "Messages", tint = Color.White,
                modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RailDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .width(32.dp)
            .height(2.dp)
            .background(BanterColors.Divider, RoundedCornerShape(1.dp))
    )
}

@Composable
private fun RailPill(active: Boolean) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .height(if (active) 40.dp else 0.dp)
            .background(Color.White, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
    )
}

@Composable
private fun RailIconButton(
    isActive: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(if (isActive) RoundedCornerShape(16.dp) else CircleShape)
            .background(if (isActive) BanterColors.Blurple else BanterColors.ServerIconBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
        content = content
    )
}
