package com.wuhan.mowangda.data

import java.time.LocalDateTime

/// 一条待办。与 iOS 版字段一一对应；差异是没有 listName——安卓版不做系统提醒事项同步，
/// 全部数据存在本机。
data class TodoItem(
    val id: String,
    var title: String,
    var due: LocalDateTime?,
    var hasTime: Boolean,
    var deadline: Boolean,
    var remindMinutes: Int,      // 0 = 不提醒
    var note: String,
    var priority: Int,           // 0 无 1 低 2 中 3 高
    var done: Boolean
)

/// 录入页实时拆条的结果
data class ParsedItem(
    var title: String,
    var due: LocalDateTime?,
    var hasTime: Boolean,
    var deadline: Boolean,
    var listName: String = "工作"
)
