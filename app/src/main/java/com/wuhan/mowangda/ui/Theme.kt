package com.wuhan.mowangda.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/// 品牌色板，与 iOS 版 Models.swift 一一对应，浅/深两套，跟随系统。
@Immutable
data class MoColors(
    val isDark: Boolean,
    val bg: Color,
    val card: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color,
    val amber: Color,
    val danger: Color,
    val tick: Color,
    val barNone: Color,
    val chip: Color,
    val chipWarn: Color,
    val chipWarnInk: Color,
    val disabled: Color,
    val edge: Color
) {
    /// 优先级配色：3 高=红、2 中=琥珀、1 低=墨绿、0 无=null
    fun priority(p: Int): Color? = when (p) {
        3 -> danger
        2 -> amber
        1 -> accent
        else -> null
    }

    fun priorityName(p: Int) = when (p) {
        3 -> "高"
        2 -> "中"
        1 -> "低"
        else -> "无"
    }

    /// 整卡染色。深色模式下透明度要高一档，否则在深底上像没上色
    fun priorityFill(p: Int): Color =
        priority(p)?.copy(alpha = if (isDark) 0.22f else 0.11f) ?: card

    fun priorityStroke(p: Int): Color =
        priority(p)?.copy(alpha = if (isDark) 0.45f else 0.30f) ?: line
}

private fun c(argb: Long) = Color(argb)

private val LightColors = MoColors(
    isDark = false,
    bg = c(0xFFF7F6F3), card = c(0xFFFFFFFF), ink = c(0xFF1D1D1F), ink2 = c(0xFF4A4A4E),
    muted = c(0xFF8E8E93), faint = c(0xFFB4B2AB), line = c(0xFFE9E7E1),
    accent = c(0xFF0F6E56), accentSoft = c(0xFFE1F5EE), amber = c(0xFFBA7517),
    danger = c(0xFFA32D2D), tick = c(0xFFD8D5CD), barNone = c(0xFFDCD9D1),
    chip = c(0xFFF1EFE9), chipWarn = c(0xFFFAEEDA), chipWarnInk = c(0xFF854F0B),
    disabled = c(0xFFD9D6CE), edge = c(0xFFE0DDD5)
)

private val DarkColors = MoColors(
    isDark = true,
    bg = c(0xFF0F1211), card = c(0xFF1B1F1E), ink = c(0xFFF2F3F1), ink2 = c(0xFFBFC2BE),
    muted = c(0xFF9A9E9B), faint = c(0xFF6B706C), line = c(0xFF2A2F2C),
    accent = c(0xFF46C79E), accentSoft = c(0xFF153A2F), amber = c(0xFFE3A94F),
    danger = c(0xFFE2685F), tick = c(0xFF4A504C), barNone = c(0xFF3A403C),
    chip = c(0xFF262B28), chipWarn = c(0xFF3A2E17), chipWarnInk = c(0xFFE3A94F),
    disabled = c(0xFF343A36), edge = c(0xFF3A403C)
)

@Composable
fun moColors(): MoColors = if (isSystemInDarkTheme()) DarkColors else LightColors
