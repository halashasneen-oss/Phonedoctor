package com.phonedoctor.app.domain.util

import java.text.DateFormat
import java.util.Date
import kotlin.math.ln
import kotlin.math.pow

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) "${bytes} B" else String.format("%.1f %s", value, units[digitGroups])
    }

    fun formatDateTime(millis: Long): String {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
    }

    fun formatDate(millis: Long): String {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
    }

    fun formatRelativeTime(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = nowMillis - millis
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days < 7 -> "$days d ago"
            else -> formatDate(millis)
        }
    }
}
