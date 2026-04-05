package com.campus.todo.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object TimeUtils {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun startOfDayEpoch(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDayEpoch(date: LocalDate): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    fun formatEpoch(epoch: Long): String {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), zone)
        return DateTimeFormatter.ofPattern("M/d HH:mm").format(zdt)
    }

    fun formatDate(epoch: Long): String {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), zone)
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(zdt)
    }

    /**
     * Format today's date as "星期X 月/日"
     */
    fun formatDateToday(): String {
        val today = LocalDate.now()
        val dow = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
        return "$dow ${today.monthValue}/${today.dayOfMonth}"
    }

    /**
     * Get relative date string (今天/明天/后天 or M/d)
     */
    fun formatRelativeDate(epoch: Long): String {
        val date = Instant.ofEpochMilli(epoch).atZone(zone).toLocalDate()
        val today = LocalDate.now()
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, date)
        return when (daysUntil) {
            0L -> "今天"
            1L -> "明天"
            2L -> "后天"
            in 3..7 -> "${daysUntil}天后"
            else -> DateTimeFormatter.ofPattern("M/d").format(date)
        }
    }
}
