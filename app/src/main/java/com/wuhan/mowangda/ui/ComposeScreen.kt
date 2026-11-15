package com.wuhan.mowangda.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.wuhan.mowangda.data.ParsedItem
import com.wuhan.mowangda.data.TimeParser
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/// 录入页：一句话实时拆条，逐卡可删，确认后写入本地。
/// 布局与 iOS 版一致：顶部导航 → 输入框 + 识别卡片 → 底部固定条（麦克风在上、写入按钮在下）。
@Composable
fun ComposeScreen(onAdd: (ParsedItem) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val colors = moColors()
    var text by remember { mutableStateOf("") }
    val parsed = remember { mutableStateListOf<ParsedItem>() }
    var transcribing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun reparse(v: String) {
        parsed.clear()
        parsed.addAll(TimeParser.parseAll(v))
    }

    val sr = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context))
            SpeechRecognizer.createSpeechRecognizer(context)
        else null
    }
    DisposableEffect(Unit) {
        onDispose { sr?.destroy() }
    }
    sr?.setRecognitionListener(object : RecognitionListener {
        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.let { text = it; reparse(it) }
        }

        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.let { text = it; reparse(it) }
            transcribing = false
        }

        override fun onError(error: Int) {
            transcribing = false
            errorMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没听清，再试一次"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "请在设置里允许麦克风和语音识别"
                else -> "语音识别出错（$error）"
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    fun startSpeech() {
        if (transcribing) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMsg = "此设备没有可用的中文语音识别"
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        errorMsg = null
        transcribing = true
        sr?.startListening(intent)
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSpeech() else errorMsg = "请在设置里允许麦克风"
    }


    fun toggleSpeech() {
        if (transcribing) {
            sr?.stopListening()
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) startSpeech() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun write() {
        val list = parsed.toList()
        list.forEach { onAdd(it) }
        onClose()
    }

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        // 顶部导航
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text("取消", fontSize = 13.sp, color = colors.accent, modifier = Modifier.clickable { onClose() })
            Spacer(Modifier.weight(1f))
            Text(
                if (transcribing) "正在听…" else "说一句，或打一行",
                fontSize = 12.sp, color = colors.muted
            )
            Spacer(Modifier.weight(1f))
            Text("清空", fontSize = 13.sp, color = colors.muted,
                modifier = Modifier.clickable { text = ""; reparse("") })
        }

        // 中间可滚动区
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it; reparse(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                textStyle = TextStyle(fontSize = 15.sp, color = colors.ink, lineHeight = 22.sp),
                cursorBrush = SolidColor(colors.accent),
                decorationBox = { inner ->
                    Column(
                        Modifier
                            .background(colors.card, RoundedCornerShape(18.dp))
                            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                            .heightIn(min = 88.dp)
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                "明天下午3点跟张总碰方案，周三前把预算表发财务",
                                fontSize = 15.sp, color = colors.faint, lineHeight = 22.sp
                            )
                        }
                        inner()
                    }
                }
            )

            if (parsed.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("识别到 ${parsed.size} 项", fontSize = 11.5.sp, color = colors.muted)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f).height(1.dp).background(colors.line))
                    Spacer(Modifier.width(8.dp))
                    Text("可逐条删", fontSize = 10.5.sp, color = colors.faint)
                }
            }
            parsed.toList().forEachIndexed { i, p ->
                ParseCard(p, colors) { parsed.removeAt(i) }
            }
            if (parsed.isEmpty()) {
                Text(
                    "说完或打完，这里会即时拆成带时间的一条条",
                    fontSize = 12.sp, color = colors.faint,
                    modifier = Modifier.fillMaxWidth().padding(top = 44.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // 底部固定操作条：麦克风在上、写入按钮在下
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (transcribing) colors.danger else colors.accent)
                    .clickable { toggleSpeech() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (transcribing) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (transcribing) "停止听写" else "语音输入",
                    tint = colors.bg,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (parsed.isEmpty()) colors.disabled else colors.accent,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = parsed.isNotEmpty()) { write() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (parsed.isEmpty()) "写入提醒事项" else "写入提醒事项 · ${parsed.size} 项",
                    fontSize = 13.5.sp, color = colors.bg
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                hintOf(transcribing, parsed.toList(), errorMsg),
                fontSize = 10.5.sp,
                color = if (errorMsg != null) colors.danger else colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ParseCard(p: ParsedItem, colors: MoColors, onRemove: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .background(colors.card, RoundedCornerShape(16.dp))
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row {
            Text(
                p.title.ifEmpty { "（未命名）" },
                fontSize = 13.5.sp, color = colors.ink,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text("×", fontSize = 15.sp, color = colors.faint, modifier = Modifier.clickable { onRemove() })
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                whenText(p),
                fontSize = 10.5.sp,
                color = if (p.deadline) colors.chipWarnInk else if (p.due != null) colors.accent else colors.muted,
                modifier = Modifier
                    .background(
                        when {
                            p.deadline -> colors.chipWarn
                            p.due != null -> colors.accentSoft
                            else -> colors.chip
                        },
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
            if (p.deadline) {
                Text(
                    "截止",
                    fontSize = 10.5.sp, color = colors.chipWarnInk,
                    modifier = Modifier
                        .background(colors.chipWarn, RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun hintOf(transcribing: Boolean, parsed: List<ParsedItem>, error: String?): String {
    if (error != null) return error
    if (transcribing) return "正在听… 说完点一下方块停止"
    if (parsed.isEmpty()) return "按麦克风说话，或直接在上面打字"
    return "将排入 " + parsed.joinToString("　·　") { whenText(it) }
}

private fun whenText(p: ParsedItem): String {
    val d = p.due ?: return "未定时间"
    val today = LocalDateTime.now().toLocalDate()
    val day = when (java.time.temporal.ChronoUnit.DAYS.between(today, d.toLocalDate())) {
        0L -> "今天"
        1L -> "明天"
        else -> d.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
    }
    return if (p.hasTime) day + " " + d.format(DateTimeFormatter.ofPattern("HH:mm"))
    else "$day 全天"
}
