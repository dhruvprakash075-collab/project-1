package com.openfiles.core.common

import java.text.DateFormat
import java.util.Date
import kotlin.math.ln
import kotlin.math.pow

/** Human-readable file size, e.g. "12.4 MB". Shared by browser, gallery, and viewer screens. */
fun Long.toHumanReadableSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (ln(this.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = this / 1024.0.pow(digitGroups.toDouble())
    return "%.1f %s".format(value, units[digitGroups])
}

fun Long.toDisplayDate(): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(this))
