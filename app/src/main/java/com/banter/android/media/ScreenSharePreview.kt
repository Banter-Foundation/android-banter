package com.banter.android.media

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banter.android.ui.theme.BanterColors

/**
 * Live local preview of the screen being shared (Go Live).
 */
@Composable
fun ScreenSharePreview(
    screenShareManager: ScreenShareManager,
    modifier: Modifier = Modifier,
    showLiveBadge: Boolean = true
) {
    val frame by screenShareManager.previewFrame.collectAsState()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        val bitmap: Bitmap? = frame
        if (bitmap != null && !bitmap.isRecycled) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Screen share preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                "Starting stream…",
                color = BanterColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        if (showLiveBadge) {
            Text(
                "YOU'RE LIVE",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BanterColors.Fuchsia)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
