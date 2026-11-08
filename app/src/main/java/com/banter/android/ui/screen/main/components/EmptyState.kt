package com.banter.android.ui.screen.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banter.android.ui.theme.BanterColors

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Forum,
                contentDescription = null,
                tint = BanterColors.TextMuted,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "Pick a channel or DM",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BanterColors.TextPrimary
            )
            Text(
                "Select a channel in the sidebar to start chatting",
                fontSize = 14.sp,
                color = BanterColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
