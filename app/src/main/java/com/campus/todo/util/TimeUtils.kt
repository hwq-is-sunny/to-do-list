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

    /** 将 [newDate] 与 [referenceEpoch] 的时分（及秒）合并；无参考则用当日 23:59 */
    fun atDateKeepingTime(newDate: LocalDate, referenceEpoch: Long?): Long {
        if (referenceEpoch == null || referenceEpoch <= 0L) {
            return newDate.atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
        }
        val t = Instant.ofEpochMilli(referenceEpoch).atZone(zone).toLocalTime()
        return newDate.atTime(t).atZone(zone).toInstant().toEpochMilli()
    }
}
