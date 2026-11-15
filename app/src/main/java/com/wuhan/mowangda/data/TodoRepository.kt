package com.wuhan.mowangda.data

import android.content.Context
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/// 本地 JSON 持久化（org.json 是系统自带，零依赖）。
/// 权威数据就是这一份文件；安卓版不同步系统提醒事项。
object TodoRepository {

    private const val KEY = "items.v1"

    fun load(context: Context): MutableList<TodoItem> {
        val raw = context.getSharedPreferences("mowangda", Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return mutableListOf()
        return runCatching {
            val arr = JSONArray(raw)
            val out = mutableListOf<TodoItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val ms = o.optLong("due", 0L)
                out.add(
                    TodoItem(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        due = if (ms == 0L) null
                        else LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()),
                        hasTime = o.getBoolean("hasTime"),
                        deadline = o.getBoolean("deadline"),
                        remindMinutes = o.optInt("remindMinutes", 0),
                        note = o.optString("note"),
                        priority = o.optInt("priority", 0),
                        done = o.getBoolean("done")
                    )
                )
            }
            out
        }.getOrDefault(mutableListOf())
    }

    fun save(context: Context, items: List<TodoItem>) {
        val arr = JSONArray()
        for (it in items) {
            arr.put(
                org.json.JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("due", it.due?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0L)
                    .put("hasTime", it.hasTime)
                    .put("deadline", it.deadline)
                    .put("remindMinutes", it.remindMinutes)
                    .put("note", it.note)
                    .put("priority", it.priority)
                    .put("done", it.done)
            )
        }
        context.getSharedPreferences("mowangda", Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
