package com.wuhan.mowangda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wuhan.mowangda.data.TodoItem
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/// 详情编辑：改完（离开页面时）回写本地并重排提醒。
@Composable
fun DetailScreen(
    item: TodoItem,
    onUpdate: (TodoItem) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val colors = moColors()
    var title by remember { mutableStateOf(item.title) }
    var due by remember { mutableStateOf(item.due ?: LocalDateTime.now()) }
    var hasTime by remember { mutableStateOf(item.hasTime) }
    var hasDue by remember { mutableStateOf(item.due != null) }
    var remind by remember { mutableStateOf(item.remindMinutes) }
    var priority by remember { mutableStateOf(item.priority) }
    var note by remember { mutableStateOf(item.note) }
    var deadline by remember { mutableStateOf(item.deadline) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            val v = item.copy(
                title = title.ifEmpty { item.title },
                due = if (hasDue) due else null,
                hasTime = hasDue && hasTime,
                remindMinutes = remind,
                priority = priority,
                note = note,
                deadline = deadline
            )
            if (v != item) onUpdate(v)
        }
    }

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)) {
                Text("‹ 返回", fontSize = 13.sp, color = colors.accent, modifier = Modifier.clickable { onClose() })
                Spacer(Modifier.weight(1f))
                Text("编辑", fontSize = 12.sp, color = colors.muted)
                Spacer(Modifier.weight(1f))
                Text("完成", fontSize = 13.sp, color = colors.accent, modifier = Modifier.clickable { onClose() })
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    textStyle = TextStyle(fontSize = 19.sp, color = colors.ink),
                    cursorBrush = SolidColor(colors.accent),
                    decorationBox = { inner -> if (title.isEmpty()) { Text("事项标题", fontSize = 19.sp, color = colors.faint); inner() } else inner() }
                )

                Column(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .background(colors.card, RoundedCornerShape(16.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                ) {
                    // 时间
                    SettingRow("时间", colors) {
                        if (!hasDue) {
                            Text(
                                "设定时间", fontSize = 12.5.sp, color = colors.accent,
                                modifier = Modifier.clickable { hasDue = true }
                            )
                        } else {
                            Text(
                                due.format(DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)),
                                fontSize = 12.5.sp, color = colors.accent,
                                modifier = Modifier.clickable { showDate = true }
                            )
                            Spacer(Modifier.width(12.dp))
                            if (hasTime) {
                                Text(
                                    due.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    fontSize = 12.5.sp, color = colors.accent,
                                    modifier = Modifier.clickable { showTime = true }
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                            Switch(checked = hasTime, onCheckedChange = { hasTime = it })
                        }
                    }
                    Divider(colors)
                    // 优先级
                    SettingRow("优先级", colors) {
                        MenuPicker(
                            options = listOf(0, 1, 2, 3),
                            selected = priority,
                            labelOf = { nvName(it) },
                            display = { n ->
                                val c = colors.priority(n) ?: colors.tick
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).background(c, RoundedCornerShape(50)))
                                    Spacer(Modifier.width(6.dp))
                                    Text(nvName(n), fontSize = 12.5.sp, color = colors.ink)
                                }
                            },
                            onSelect = { priority = it }
                        )
                    }
                    Divider(colors)
                    // 提醒
                    SettingRow("提醒", colors) {
                        MenuPicker(
                            options = listOf(0, 5, 15, 30, 60),
                            selected = remind,
                            labelOf = { if (it == 0) "不提醒" else "提前 $it 分钟" },
                            display = { Text(if (it == 0) "不提醒" else "提前 $it 分钟", fontSize = 12.5.sp, color = colors.ink) },
                            onSelect = { remind = it }
                        )
                    }
                    Divider(colors)
                    // 截止
                    SettingRow("截止", colors) {
                        Switch(checked = deadline, onCheckedChange = { deadline = it })
                    }
                }

                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    textStyle = TextStyle(fontSize = 12.5.sp, color = colors.ink, lineHeight = 19.sp),
                    cursorBrush = SolidColor(colors.accent),
                    decorationBox = { inner ->
                        Column(
                            Modifier
                                .background(colors.card, RoundedCornerShape(16.dp))
                                .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            if (note.isEmpty()) Text("备注…", fontSize = 12.5.sp, color = colors.faint)
                            inner()
                        }
                    }
                )

                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Alignment.CenterHorizontally
                ) {
                    Text("数据保存在本机，到点由莫忘哒发通知提醒", fontSize = 10.5.sp, color = colors.muted)
                }

                Text(
                    "删除事项",
                    fontSize = 12.5.sp, color = colors.danger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp)
                        .clickable { onDelete() },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (showDate) {
        val init = due.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = init)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        val t = due.toLocalTime()
                        due = d.atTime(t)
                        hasDue = true
                    }
                    showDate = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }

    if (showTime) {
        val t: LocalTime = due.toLocalTime()
        val tp = rememberTimePickerState(initialHour = t.hour, initialMinute = t.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    due = due.with(LocalTime.of(tp.hour, tp.minute))
                    hasTime = true
                    showTime = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("取消") } },
            text = { TimePicker(state = tp) }
        )
    }
}

@Composable
private fun SettingRow(label: String, colors: MoColors, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.5.sp, color = colors.muted)
        Spacer(Modifier.weight(1f))
        content()
    }
}

@Composable
private fun Divider(colors: MoColors) {
    Box(Modifier.fillMaxWidth().padding(start = 15.dp).height(1.dp).background(colors.line))
}

@Composable
private fun <T> MenuPicker(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    display: @Composable (T) -> Unit,
    onSelect: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(Modifier.clickable { open = true }) { display(selected) }
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(labelOf(opt)) },
                    onClick = { onSelect(opt); open = false }
                )
            }
        }
    }
}

private fun nvName(p: Int) = when (p) {
    3 -> "高"
    2 -> "中"
    1 -> "低"
    else -> "无"
}

