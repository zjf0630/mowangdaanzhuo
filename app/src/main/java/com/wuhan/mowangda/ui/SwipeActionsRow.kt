package com.wuhan.mowangda.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/// 左滑露出右侧两个按钮：完成（绿）在左、删除（红）在最右，与 iOS 版一致。
/// 不做全滑——删除是真删，必须明确点一下。
@Composable
fun SwipeActionsRow(
    enabled: Boolean,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = moColors()
    val actionsWidth = with(LocalDensity.current) { 152.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun snapOpen(open: Boolean) {
        scope.launch {
            offsetX.animateTo(
                if (open) -actionsWidth else 0f,
                spring(stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    Box(Modifier.fillMaxWidth()) {
        // 右侧露出的按钮区（最右是删除）
        Row(Modifier.align(Alignment.CenterEnd)) {
            ActionButton("完成", Icons.Filled.Check, colors.accent, colors.bg) {
                snapOpen(false)
                onComplete()
            }
            ActionButton("删除", Icons.Filled.Delete, colors.danger, colors.bg) {
                snapOpen(false)
                onDelete()
            }
        }

        // 内容本体，可水平拖动
        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = { snapOpen(offsetX.value < -actionsWidth / 2) },
                        onDragCancel = { snapOpen(false) },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            val next = (offsetX.value + amount).coerceIn(-actionsWidth, 0f)
                            scope.launch { offsetX.snapTo(next) }
                        }
                    )
                }
                .clickable(enabled = enabled && offsetX.value < 0f) { snapOpen(false) }
        ) { content() }
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .width(76.dp)
            .fillMaxHeight()
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
            Text(label, color = fg, fontSize = 11.sp)
        }
    }
}
