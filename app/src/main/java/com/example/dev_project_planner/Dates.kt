package com.example.dev_project_planner

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val DAY_MS = 86_400_000L

fun formatDate(ms: Long?): String {
    if (ms == null) return "—"
    val f = SimpleDateFormat("MMM d", Locale.getDefault())
    f.timeZone = TimeZone.getTimeZone("UTC")
    return f.format(Date(ms))
}
