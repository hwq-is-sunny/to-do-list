package com.campus.todo.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
}
