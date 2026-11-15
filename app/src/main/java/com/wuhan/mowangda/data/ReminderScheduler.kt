package com.wuhan.mowangda.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wuhan.mowangda.R
import java.time.ZoneId

/// 到点提醒：AlarmManager 精确闹钟 + 通知渠道。
///
/// 这是安卓版比 iOS 版强的地方——iOS 的 EventKit 不给第三方 App 设置铃声/振动的接口，
/// 这里的三个选项（提示音+振动三声 / 静音 / 仅振动）是真的生效的：
/// 通知渠道创建时就写死了 vibrationPattern = 三下，声音按用户选择。
/// 渠道的声音/振动创建后不可改，所以三种方式各建一个渠道，切换时换渠道 id。
object ReminderScheduler {
    const val EXTRA_ID = "id"
    const val EXTRA_TITLE = "title"

    fun channelId(tone: Int) = "reminders_t$tone"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val vibrate3 = longArrayOf(0, 350, 250, 350, 250, 350)   // 振三下：震-停-震-停-震

        for (tone in 0..2) {
            val ch = NotificationChannel(
                channelId(tone), channelName(tone), NotificationManager.IMPORTANCE_HIGH
            )
            when (tone) {
                Prefs.TONE_DEFAULT -> {
                    ch.setSound(defaultSound, attrs)
                    ch.enableVibration(true)
                    ch.vibrationPattern = vibrate3
                }
                Prefs.TONE_SILENT -> {
                    ch.setSound(null, null)
                    ch.enableVibration(false)
                }
                else -> {
                    ch.setSound(null, null)
                    ch.enableVibration(true)
                    ch.vibrationPattern = vibrate3
                }
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun channelName(tone: Int) = when (tone) {
        Prefs.TONE_DEFAULT -> "到点提醒 · 提示音+振动三声"
        Prefs.TONE_SILENT -> "到点提醒 · 静音"
        else -> "到点提醒 · 仅振动"
    }

    fun schedule(context: Context, item: TodoItem) {
        cancel(context, item.id)
        val due = item.due ?: return
        if (!item.hasTime || item.remindMinutes <= 0 || item.done) return
        val trigger = due.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                item.remindMinutes * 60_000L
        if (trigger <= System.currentTimeMillis()) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context, item.id, item.title)
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
    }

    fun cancel(context: Context, id: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, id, null))
    }

    private fun pending(context: Context, id: String, title: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, id)
        if (title != null) intent.putExtra(EXTRA_TITLE, title)
        return PendingIntent.getBroadcast(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /// 冷启动 / 重启后把所有未完成条目的闹钟重新排上
    fun rescheduleAll(context: Context) {
        ensureChannels(context)
        TodoRepository.load(context).filter { !it.done }.forEach { schedule(context, it) }
    }
}

/// 闹钟到点：弹通知，走当前偏好对应的渠道
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "到点的提醒"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val tone = Prefs.tone(context)
        val notification = NotificationCompat.Builder(context, ReminderScheduler.channelId(tone))
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("莫忘哒")
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        nm.notify(id.hashCode(), notification)
    }
}

/// 重启手机后 AlarmManager 的闹钟全部失效，开机广播里重排
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.rescheduleAll(context)
        }
    }
}
