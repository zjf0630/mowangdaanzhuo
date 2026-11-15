package com.wuhan.mowangda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wuhan.mowangda.data.ParsedItem
import com.wuhan.mowangda.data.Prefs
import com.wuhan.mowangda.data.ReminderScheduler
import com.wuhan.mowangda.data.TodoItem
import com.wuhan.mowangda.data.TodoRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

private sealed interface ListRow {
    data class Section(val name: String, val count: Int, val date: LocalDateTime?) : ListRow
    data class Task(val item: TodoItem) : ListRow
    data class DoneHead(val count: Int) : ListRow
    data object Empty : ListRow
    data object Tail : ListRow
}

private val ListRow.key: String
    get() = when (this) {
        is ListRow.Section -> "s-$name"
        is ListRow.Task -> "t-${item.id}"
        ListRow.DoneHead -> "done-head"
        ListRow.Empty -> "empty"
        ListRow.Tail -> "tail"
    }

@Composable
fun RootScreen() {
    val context = LocalContext.current
    val colors = moColors()

    val items = remember { mutableStateListOf<TodoItem>().apply { addAll(TodoRepository.load(context)) } }
    val selected = remember { mutableStateListOf<String>() }
    var selectMode by remember { mutableStateOf(false) }
    var doneOpen by remember { mutableStateOf(false) }
    var showCompose by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<String?>(null) }

    fun persist() = TodoRepository.save(context, items)

    fun add(p: ParsedItem) {
        val item = TodoItem(
            id = UUID.randomUUID().toString(), title = p.title, due = p.due,
            hasTime = p.hasTime, deadline = p.deadline,
            remindMinutes = if (p.hasTime) Prefs.leadMinutes(context) else 0,
            note = "", priority = 0, done = false
        )
        items.add(0, item)
        persist()
        ReminderScheduler.schedule(context, item)
    }

    fun update(item: TodoItem) {
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) items[idx] = item
        persist()
        ReminderScheduler.schedule(context, item)
    }

    fun setDone(item: TodoItem, done: Boolean) {
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) items[idx] = items[idx].copy(done = done)
        persist()
        if (done) ReminderScheduler.cancel(context, item.id)
        else if (idx >= 0) ReminderScheduler.schedule(context, items[idx])
    }

    fun delete(ids: Set<String>) {
        items.removeAll { it.id in ids }
        persist()
        ids.forEach { ReminderScheduler.cancel(context, it) }
        selected.removeAll(ids.toSet())
    }

    val rows = remember(items.toList(), doneOpen) {
        buildList {
            val today = LocalDate.now()
            fun dayDiff(it: TodoItem): Int =
                it.due?.let { d -> ChronoUnit.DAYS.between(today, d.toLocalDate()).toInt() } ?: 999
            val active = items.filter { !it.done }.sortedWith(compareBy { it.due ?: LocalDateTime.MAX })
            val done = items.filter { it.done }.sortedWith(compareBy { it.due ?: LocalDateTime.MAX })

            fun add(name: String, list: List<TodoItem>) {
                if (list.isEmpty()) return
                add(ListRow.Section(name, list.size, list.first().due))
                list.forEach { add(ListRow.Task(it)) }
            }
            add("今天", active.filter { dayDiff(it) <= 0 })
            add("明天", active.filter { dayDiff(it) == 1 })
            val rest = active.filter { dayDiff(it) > 1 && it.due != null }
            add("本周", rest.filter { dayDiff(it) < 7 })
            add("以后", rest.filter { dayDiff(it) >= 7 })
            add("时间待定", active.filter { it.due == null })

            if (done.isNotEmpty()) {
                add(ListRow.DoneHead(done.size))
                if (doneOpen) done.forEach { add(ListRow.Task(it)) }
            }
            if (items.isEmpty()) add(ListRow.Empty)
            add(ListRow.Tail)
        }
    }

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize()) {
            Header(
                selectMode = selectMode,
                onSelect = {
                    selectMode = !selectMode
                    selected.clear()
                },
                onSettings = { showSettings = true }
            )

            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(rows, key = { _, r -> r.key }) { _, r ->
                    when (r) {
                        is ListRow.Section -> SectionHead(r.name, r.count, r.date)
                        is ListRow.Task -> TaskRow(
                            item = r.item,
                            colors = colors,
                            selectMode = selectMode,
                            picked = r.item.id in selected,
                            onComplete = { setDone(r.item, !r.item.done) },
                            onDelete = { delete(setOf(r.item.id)) },
                            onPick = {
                                if (selected.contains(r.item.id)) selected.remove(r.item.id)
                                else selected.add(r.item.id)
                            },
                            onOpen = { detailId = r.item.id }
                        )
                        is ListRow.DoneHead -> DoneHead(r.count, doneOpen) { doneOpen = !doneOpen }
                        ListRow.Empty -> EmptyHint()
                        ListRow.Tail -> Spacer(Modifier.height(104.dp))
                    }
                }
            }
        }

        // 底部悬浮操作条
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectMode) {
                BottomButton("全选", bg = colors.card, fg = colors.ink2, stroke = colors.edge) {
                    val ids = items.map { it.id }.toSet()
                    val all = ids.isNotEmpty() && ids.all { it in selected }
                    selected.clear()
                    if (!all) selected.addAll(ids)
                }
                BottomButton(
                    "删除（${selected.size}）",
                    bg = if (selected.isEmpty()) colors.danger.copy(alpha = 0.45f) else colors.danger,
                    fg = colors.bg
                ) {
                    delete(selected.toSet())
                    selectMode = false
                }
            } else {
                BottomButton("＋ 记一条", bg = colors.accent, fg = colors.bg) { showCompose = true }
            }
        }

        // 全屏覆盖页
        if (showCompose) {
            ComposeScreen(
                onAdd = { add(it) },
                onClose = {
                    showCompose = false
                    ReminderScheduler.rescheduleAll(context)
                }
            )
        }
        if (showSettings) SettingsScreen(onClose = { showSettings = false })
        detailId?.let { id ->
            items.firstOrNull { it.id == id }?.let { item ->
                DetailScreen(
                    item = item,
                    onUpdate = { update(it) },
                    onDelete = {
                        delete(setOf(item.id))
                        detailId = null
                    },
                    onClose = { detailId = null }
                )
            }
        }
    }
}

// MARK: 顶栏

@Composable
private fun Header(selectMode: Boolean, onSelect: () -> Unit, onSettings: () -> Unit) {
    val colors = moColors()
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(dateText, fontSize = 12.sp, color = colors.muted)
            Text("莫忘哒", fontSize = 26.sp, color = colors.ink)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.card)
                .border(1.dp, colors.line, CircleShape)
                .clickable { onSettings() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "设置", tint = colors.ink2, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                .clickable { onSelect() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(if (selectMode) "取消" else "选择", fontSize = 12.sp, color = colors.ink2)
        }
    }
}

// MARK: 列表行

@Composable
private fun SectionHead(name: String, count: Int, date: LocalDateTime?) {
    val colors = moColors()
    val title = date?.let {
        name + " · " + it.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
    } ?: name
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 11.5.sp, color = colors.muted)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(colors.line))
        Spacer(Modifier.width(8.dp))
        Text("$count 项", fontSize = 10.5.sp, color = colors.faint)
    }
}

@Composable
private fun DoneHead(count: Int, open: Boolean, onToggle: () -> Unit) {
    val colors = moColors()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 8.dp)
    ) {
        Text("已完成 · $count 项", fontSize = 11.5.sp, color = colors.muted, modifier = Modifier.weight(1f))
        Text(if (open) "收起 ▴" else "展开 ▾", fontSize = 10.5.sp, color = colors.muted)
    }
}

@Composable
private fun EmptyHint() {
    val colors = moColors()
    Text(
        "还没有事项\n点下面「＋ 记一条」，说一句就行",
        fontSize = 12.5.sp, color = colors.faint,
        lineHeight = 18.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    item: TodoItem,
    colors: MoColors,
    selectMode: Boolean,
    picked: Boolean,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onPick: () -> Unit,
    onOpen: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    SwipeActionsRow(
        enabled = !selectMode,
        onComplete = onComplete,
        onDelete = onDelete
    ) {
        Row(
            Modifier
                .background(colors.bg)
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .combinedClickable(
                    onClick = { if (selectMode) onPick() else onOpen() },
                    onLongClick = { if (!selectMode) menuOpen = true }
                )
        ) {
            TaskCard(item, colors, selectMode, picked, Modifier.weight(1f))

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (item.done) "标记为未完成" else "标记为已完成") },
                    onClick = { menuOpen = false; onComplete() }
                )
                DropdownMenuItem(
                    text = { Text("删除", color = moColors().danger) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun TaskCard(item: TodoItem, colors: MoColors, selectMode: Boolean, picked: Boolean, modifier: Modifier) {
    val pc = colors.priority(item.priority)
    val fill = if (item.done) colors.bg else colors.priorityFill(item.priority)
    val stroke = if (item.done) colors.bg else colors.priorityStroke(item.priority)
    val bar = if (item.done) colors.bg
    else pc ?: (if (item.deadline) colors.amber else if (item.due == null) colors.barNone else colors.accent)

    Row(modifier.padding(bottom = 8.dp)) {
        Text(
            timeText(item),
            fontSize = 11.sp,
            color = if (item.hasTime) colors.muted else colors.faint,
            modifier = Modifier.width(42.dp).padding(top = 13.dp)
        )
        Row(
            Modifier
                .weight(1f)
                .height(IntrinsicSize.Min)
                .background(fill, RoundedCornerShape(14.dp))
                .border(1.dp, stroke, RoundedCornerShape(14.dp))
                .padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(0.72f)
                    .background(bar, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontSize = 13.5.sp,
                    color = if (item.done) colors.faint else colors.ink,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(subText(item, colors), fontSize = 10.5.sp, color = colors.muted)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(21.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            selectMode && picked -> colors.accent
                            !selectMode && item.done -> colors.accent
                            else -> colors.card
                        }
                    )
                    .border(
                        1.5.dp,
                        if ((selectMode && picked) || (!selectMode && item.done)) colors.accent else colors.tick,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if ((selectMode && picked) || (!selectMode && item.done)) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = colors.card,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomButton(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, stroke: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    val colors = moColors()
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(
                if (stroke != null) Modifier.border(1.dp, stroke, RoundedCornerShape(50)) else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Text(text, fontSize = 13.sp, color = fg)
    }
}

// MARK: 工具

private fun timeText(item: TodoItem): String {
    val d = item.due ?: return "—"
    return if (item.hasTime) d.format(DateTimeFormatter.ofPattern("HH:mm"))
    else "全天"
}

private fun subText(item: TodoItem, colors: MoColors): String {
    var s = when {
        item.deadline -> "截止"
        item.due == null -> "时间待定"
        !item.hasTime -> "全天"
        item.remindMinutes > 0 -> "提前${item.remindMinutes}分钟"
        else -> "不提醒"
    }
    if (item.priority > 0 && !item.done) s += " · " + colors.priorityName(item.priority)
    return s
}
