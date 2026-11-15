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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wuhan.mowangda.data.Prefs
import com.wuhan.mowangda.data.ReminderScheduler

/// 设置页：默认提前量 + 到点提醒方式。
/// 安卓上提醒声音/振动是 App 自己的通知渠道，三项都真实生效（iOS 做不到的这里做得到）。
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val colors = moColors()
    var lead by remember { mutableStateOf(Prefs.leadMinutes(context)) }
    var tone by remember { mutableStateOf(Prefs.tone(context)) }

    fun saveTone(v: Int) {
        tone = v
        Prefs.setTone(context, v)
        ReminderScheduler.ensureChannels(context)
    }

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)) {
                Text("‹ 返回", fontSize = 13.sp, color = colors.accent, modifier = Modifier.clickable { onClose() })
                Spacer(Modifier.weight(1f))
                Text("设置", fontSize = 12.sp, color = colors.muted)
                Spacer(Modifier.weight(1f))
                Text("完成", fontSize = 13.sp, color = colors.accent, modifier = Modifier.clickable { onClose() })
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                SectionLabel("默认提醒", colors)
                Column(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .background(colors.card, RoundedCornerShape(16.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("新记的条目", fontSize = 13.5.sp, color = colors.ink)
                            Text("到点前多久提醒你，可在详情页逐条改", fontSize = 10.5.sp, color = colors.muted)
                        }
                        var leadOpen by remember { mutableStateOf(false) }
                        Box {
                            Text(
                                if (lead == 0) "不提醒" else "提前 $lead 分钟",
                                fontSize = 12.5.sp, color = colors.accent,
                                modifier = Modifier.clickable { leadOpen = true }
                            )
                            androidx.compose.material3.DropdownMenu(expanded = leadOpen, onDismissRequest = { leadOpen = false }) {
                                listOf(0, 5, 15, 30, 60).forEach { m ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(if (m == 0) "不提醒" else "提前 $m 分钟") },
                                        onClick = {
                                            lead = m
                                            Prefs.setLeadMinutes(context, m)
                                            leadOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                SectionLabel("到点提醒方式", colors)
                Column(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .background(colors.card, RoundedCornerShape(16.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                ) {
                    ToneRow("提示音 + 振动三声", "系统提示音，伴随震-停-震-停-震三下", tone == Prefs.TONE_DEFAULT) {
                        saveTone(Prefs.TONE_DEFAULT)
                    }
                    Box(Modifier.fillMaxWidth().padding(start = 15.dp).height(1.dp).background(colors.line))
                    ToneRow("静音", "到点只弹通知，不响不震", tone == Prefs.TONE_SILENT) {
                        saveTone(Prefs.TONE_SILENT)
                    }
                    Box(Modifier.fillMaxWidth().padding(start = 15.dp).height(1.dp).background(colors.line))
                    ToneRow("仅振动", "不出声，到点振动三下", tone == Prefs.TONE_VIBRATE_ONLY) {
                        saveTone(Prefs.TONE_VIBRATE_ONLY)
                    }
                }

                Text(
                    "提示音用的是系统通知音；振动三下是莫忘哒自己的节奏。改完对新提醒立即生效，不用逐条重存。",
                    fontSize = 10.5.sp, color = colors.faint, lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(s: String, colors: MoColors) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(s, fontSize = 11.5.sp, color = colors.muted)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(colors.line))
    }
}

@Composable
private fun ToneRow(title: String, detail: String, on: Boolean, onClick: () -> Unit) {
    val colors = moColors()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, color = colors.ink)
            Text(detail, fontSize = 10.5.sp, color = colors.muted)
        }
        Box(
            Modifier
                .size(20.dp)
                .border(1.5.dp, if (on) colors.accent else colors.tick, CircleShape)
                .padding(3.dp)
                .background(if (on) colors.accent else colors.card, CircleShape)
        )
    }
}
