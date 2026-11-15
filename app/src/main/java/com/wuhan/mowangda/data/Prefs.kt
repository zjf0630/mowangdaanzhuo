package com.wuhan.mowangda.data

import android.content.Context

/// 用户偏好，都存 SharedPreferences。
object Prefs {
    private const val FILE = "mowangda"

    const val TONE_DEFAULT = 0      // 提示音 + 振动三声（默认）
    const val TONE_SILENT = 1       // 静音
    const val TONE_VIBRATE_ONLY = 2 // 仅振动

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /// 新记条目默认提前多少分钟提醒；0 = 不提醒
    fun leadMinutes(c: Context): Int =
        if (sp(c).contains("leadMinutes")) sp(c).getInt("leadMinutes", 15) else 15

    fun setLeadMinutes(c: Context, v: Int) {
        sp(c).edit().putInt("leadMinutes", v).apply()
    }

    /// 到点提醒的方式
    fun tone(c: Context): Int = sp(c).getInt("tone", TONE_DEFAULT)

    fun setTone(c: Context, v: Int) {
        sp(c).edit().putInt("tone", v).apply()
    }
}
