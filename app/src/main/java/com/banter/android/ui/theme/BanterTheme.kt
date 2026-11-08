package com.banter.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Discord-inspired color palette
object BanterColors {
    val Background = Color(0xFF313338)
    val BackgroundSecondary = Color(0xFF2B2D31)
    val BackgroundTertiary = Color(0xFF1E1F22)

    val InputBackground = Color(0xFF383A40)
    val ChannelRowActive = Color(0xFF404249)

    val ServerRail = Color(0xFF1E1F22)
    val ServerIconBg = Color(0xFF36393F)
    val ServerIconBgActive = Color(0xFF5865F2)

    val Blurple = Color(0xFF5865F2)
    val BlurpleDark = Color(0xFF4752C4)
    val Green = Color(0xFF23A55A)
    val Red = Color(0xFFF23F43)
    val Yellow = Color(0xFFF0B232)

    val TextPrimary = Color(0xFFDBDEE1)
    val TextSecondary = Color(0xFF949BA4)
    val TextMuted = Color(0xFF80848E)
    val TextLink = Color(0xFF00AFF4)
    val TextDanger = Color(0xFFFA777C)

    val Online = Color(0xFF23A55A)
    val Idle = Color(0xFFF0B232)
    val Dnd = Color(0xFFF23F43)
    val Offline = Color(0xFF80848E)

    val MessageHover = Color(0xFF2E3035)
    val Mention = Color(0x405865F2)

    val ChannelSidebar = Color(0xFF2B2D31)
    val ChannelText = Color(0xFF949BA4)
    val ChannelTextActive = Color(0xFFDBDEE1)
    val CategoryText = Color(0xFF80848E)

    val VoiceBar = Color(0xFF232428)
    val VoiceConnected = Color(0xFF23A55A)

    val Divider = Color(0xFF3F4147)

    val Surface = Color(0xFF383A40)
    val SurfaceRaised = Color(0xFF404249)

    val Overlay = Color(0x80000000)
    val ModalBackground = Color(0xFF2B2D31)
    val ModalBackgroundDark = Color(0xFF232428)
}

object BanterTheme {
    val colors = BanterColors
}

@Composable
fun BanterTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = BanterColors.Blurple,
        primaryContainer = BanterColors.BlurpleDark,
        secondary = BanterColors.Green,
        background = BanterColors.Background,
        surface = BanterColors.BackgroundSecondary,
        error = BanterColors.Red,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = BanterColors.TextPrimary,
        onSurface = BanterColors.TextPrimary,
        onError = Color.White,
        outline = BanterColors.Divider,
        surfaceVariant = BanterColors.InputBackground,
        onSurfaceVariant = BanterColors.TextSecondary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BanterTypography,
        content = content
    )
}
