package com.wuhan.mowangda.data

import java.time.LocalDate
import java.time.LocalDateTime

/// 中文自然语言时间解析。规则与 iOS 版 TimeParser.swift 逐条对应：
/// 月份日期 / 下周X / 周X / 周末 / 月底 / N天后 / 明天后天今晚 / 几号 / 时段 / 时刻 / 截止。
/// 注意「周X」的 weekday 语义与 Swift Calendar 一致（周日=1），转换见 swiftCur0()。
object TimeParser {

    private val weekMap = mapOf(
        "一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5,
        "六" to 6, "日" to 0, "天" to 0, "七" to 0
    )

    private val cnDigits = mapOf(
        "零" to 0, "一" to 1, "二" to 2, "两" to 2, "三" to 3, "四" to 4,
        "五" to 5, "六" to 6, "七" to 7, "八" to 8, "九" to 9
    )

    fun parseAll(text: String, base: LocalDateTime = LocalDateTime.now()): List<ParsedItem> {
        val prepared = text.replace("然后", "，").replace("接着", "，").replace("还有", "，")
        val parts = prepared.split('，', ',', '。', '；', ';', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val out = mutableListOf<ParsedItem>()
        var lastDue: LocalDateTime? = null
        var lastPeriod: Int? = null

        for (p in parts) {
            val r = parseOne(p, base, lastPeriod)
            var due = r.due
            if (r.inferred && lastDue != null) due = lastDue
            if (due != null && !r.inferred) lastDue = due
            if (r.explicitPeriod != null) lastPeriod = r.explicitPeriod
            out.add(
                ParsedItem(
                    title = r.title, due = due, hasTime = r.hasTime,
                    deadline = r.deadline, listName = "工作"
                )
            )
        }
        return out
    }

    private data class OneResult(
        val title: String,
        val due: LocalDateTime?,
        val hasTime: Boolean,
        val deadline: Boolean,
        val inferred: Boolean,
        val explicitPeriod: Int?
    )

    private fun parseOne(raw: String, base: LocalDateTime, inheritPeriod: Int?): OneResult {
        val today = base.toLocalDate()
        var s = " " + raw.trim() + " "
        var due: LocalDate? = null
        var period: Int? = null          // 0 早 1 午 2 傍晚 3 晚
        var explicitPeriod = false
        var hour: Int? = null
        var minute = 0
        var deadline = false
        val consumed = mutableListOf<String>()

        fun cut(m: String) {
            consumed.add(m)
            s = s.replace(m, " ")
        }

        // 期限：截止 / 之前 / 紧跟时间词后的「前」
        if (s.contains("截止") || s.contains("之前") || s.contains("以前") ||
            firstMatch("(?:[日号点周天末底午晨晚]|[一二三四五六七八九十]|\\d)\\s*之?前", s) != null
        ) deadline = true

        // 11月20号
        firstMatch("(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*[日号]", s)?.let { g ->
            val y = today.year
            var cand = runCatching {
                LocalDate.of(y, g[1].toIntOrNull() ?: 1, g[2].toIntOrNull() ?: 1)
            }.getOrNull() ?: today
            if (cand.isBefore(today)) cand = cand.plusYears(1)
            due = cand
            cut(g[0])
        }

        // 下周一 / 下下周五
        if (due == null) firstMatch("下(下)?(?:个)?(?:周|星期|礼拜)\\s*([一二三四五六日天七])", s)?.let { g ->
            val target = weekMap[g[2]] ?: 1
            val curMonday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
            val baseMonday = curMonday.plusDays(if (g[1] == "下") 14L else 7L)
            val off = if (target == 0) 6L else (target - 1).toLong()
            due = baseMonday.plusDays(off)
            cut(g[0])
        }

        // 周三
        if (due == null && !s.contains("周末")) {
            firstMatch("(?:本)?(?:周|星期|礼拜)\\s*([一二三四五六日天七])", s)?.let { g ->
                val target = weekMap[g[1]] ?: 1
                var diff = (target - swiftCur0(today) + 7) % 7
                if (diff == 0) diff = 7
                due = today.plusDays(diff.toLong())
                cut(g[0])
            }
        }

        // 周末
        if (due == null && s.contains("周末")) {
            val d = (6 - swiftCur0(today) + 7) % 7
            due = today.plusDays(if (d == 0) 6L else d.toLong())
            consumed.add("周末")
            s = s.replace("周末", " ")
        }

        // 月底
        if (due == null && (s.contains("月底") || s.contains("月末"))) {
            val w = if (s.contains("月底")) "月底" else "月末"
            due = today.withDayOfMonth(today.lengthOfMonth())
            consumed.add(w)
            s = s.replace(w, " ")
        }

        // 三天后
        firstMatch("(\\d{1,3}|[一二两三四五六七八九十]{1,3})\\s*天\\s*(?:以)?后", s)?.let { g ->
            if (due == null) due = today.plusDays((cnNum(g[1]) ?: 1).toLong())
            cut(g[0])
        }

        // 明天 / 后天 / 大后天 / 今晚 / 明早 …
        if (due == null) {
            val rel = listOf(
                Triple("大后天", 3, null as Int?), Triple("后天", 2, null),
                Triple("明天", 1, null), Triple("明日", 1, null),
                Triple("明早", 1, 0), Triple("明晚", 1, 3),
                Triple("今天", 0, null), Triple("今日", 0, null),
                Triple("今早", 0, 0), Triple("今晚", 0, 3)
            )
            for ((k, off, pe) in rel) {
                if (s.contains(k)) {
                    due = today.plusDays(off.toLong())
                    if (pe != null) {
                        period = pe
                        explicitPeriod = true
                    }
                    consumed.add(k)
                    s = s.replace(k, " ")
                    break
                }
            }
        }

        // 20号
        if (due == null) firstMatch("(\\d{1,2})\\s*[日号]", s)?.let { g ->
            var cand = runCatching {
                LocalDate.of(today.year, today.monthValue, g[1].toIntOrNull() ?: 1)
            }.getOrNull() ?: today
            if (cand.isBefore(today)) cand = cand.plusMonths(1)
            due = cand
            cut(g[0])
        }

        // 时段
        firstMatch("(凌晨|清晨|早上|早晨|上午|中午|下午|傍晚|晚上|夜里|晚间)", s)?.let { g ->
            period = when (g[1]) {
                "凌晨", "清晨", "早上", "早晨", "上午" -> 0
                "中午" -> 1
                "傍晚" -> 2
                else -> 3
            }
            explicitPeriod = true
            cut(g[0])
        }

        // 时刻 15:30
        firstMatch("(\\d{1,2})\\s*:\\s*(\\d{2})", s)?.let { g ->
            hour = g[1].toIntOrNull()
            minute = g[2].toIntOrNull() ?: 0
            cut(g[0])
        }

        // 时刻 三点半 / 下午4点 / 9点20
        if (hour == null) {
            firstMatch(
                "(\\d{1,2}|[一二两三四五六七八九十]{1,3})\\s*[点時时](?:\\s*(\\d{1,2}|[一二三四五六七八九十]{1,3})\\s*分?|(半))?",
                s
            )?.let { g ->
                hour = cnNum(g[1])
                if (g[3] == "半") minute = 30
                else if (g[2].isNotEmpty()) minute = cnNum(g[2]) ?: 0
                cut(g[0])
            }
        }

        // 小时按上午下午归位
        val h0 = hour
        if (h0 != null) {
            if (!explicitPeriod && inheritPeriod != null && h0 <= 12) period = inheritPeriod
            when (period) {
                1, 2, 3 -> hour = if (h0 < 12) h0 + 12 else h0
                0 -> hour = if (h0 == 12) 0 else h0
                else -> {}
            }
        }

        // 标题：去掉已经消费掉的时间词
        var title = Regex("\\s+").replace(raw, " ").trim()
        for (c in consumed) if (c.isNotEmpty()) title = title.replace(c, "")
        title = title
            .replace(Regex("^(然后|另外|再|顺便|记得|帮我)\\s*"), "")
            .replace(Regex("^之?前(?![台面边后])"), "")
            .trim { ch -> "，,、。；;：: ".contains(ch) }
            .replace(Regex("之?前$"), "")
            .replace(Regex("\\s{2,}"), " ")
        if (title.isEmpty()) title = raw.trim()

        // 时间落位
        var timeHM: Pair<Int, Int>? = hour?.let { it to minute }
        if (timeHM == null && deadline) timeHM = 18 to 0
        if (timeHM == null && period != null) {
            val p = period!!
            timeHM = (if (p == 0) 9 else if (p == 1) 12 else 14) to 0
        }

        var hasTime = false
        var finalDue: LocalDateTime? = null
        var inferred = false
        val d = due
        val t = timeHM
        if (d != null) {
            if (t != null) {
                hasTime = true
                finalDue = d.atTime(t.first, t.second)
            } else {
                finalDue = d.atStartOfDay()
            }
        } else if (t != null) {
            hasTime = true
            var fd = today.atTime(t.first, t.second)
            if (fd.isBefore(base)) fd = fd.plusDays(1)
            finalDue = fd
            inferred = true
        }
        return OneResult(title, finalDue, hasTime, deadline, inferred, if (explicitPeriod) period else null)
    }

    /// Swift Calendar 的 weekday：周日=1 … 周六=7；再 %7 得周一=2 … 周六=0、周日=1。
    /// java 的 dayOfWeek 是周一=1 … 周日=7，先换算成 Swift 语义再取模，和 iOS 版走完全同一条公式。
    private fun swiftCur0(today: LocalDate): Int {
        val swiftWeekday = today.dayOfWeek.value % 7 + 1
        return swiftWeekday % 7
    }

    private fun cnNum(s: String): Int? {
        s.toIntOrNull()?.let { return it }
        if (s == "十") return 10
        if (s.length == 1) return cnDigits[s]
        val idx = s.indexOf('十')
        if (idx >= 0) {
            val l = s.substring(0, idx)
            val r = s.substring(idx + 1)
            val lv = if (l.isEmpty()) 1 else cnDigits[l] ?: 0
            val rv = if (r.isEmpty()) 0 else cnDigits[r] ?: 0
            return lv * 10 + rv
        }
        return cnDigits[s]
    }

    private fun firstMatch(pattern: String, s: String): List<String>? {
        val m = Regex(pattern).find(s) ?: return null
        return m.groupValues
    }
}
