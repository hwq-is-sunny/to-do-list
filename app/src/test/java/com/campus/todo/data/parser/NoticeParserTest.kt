package com.campus.todo.data.parser

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class NoticeParserTest {

    private val today = LocalDate.of(2026, 10, 19)
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun parsesExplicitIsoDate() {
        assertEquals(
            LocalDate.of(2026, 12, 12).toEpochDay(),
            NoticeParser.parseDateEpochDay("请于2026-12-12前提交报告", today)
        )
    }

    @Test
    fun invalidCalendarDatesAreRejectedInsteadOfThrowing() {
        assertNull(NoticeParser.parseDateEpochDay("截止日期2026-02-30", today))
        assertNull(NoticeParser.parseDateEpochDay("请于2月30日提交", today))
        assertNull(NoticeParser.parseDateEpochDay("请于2026/13/01提交", today))
    }

    @Test
    fun rejectsInvalidTimeRanges() {
        val result = NoticeParser.parse(
            "请提交实验报告，截止日期2026-12-12，时间10:30-09:00",
            today,
            zone
        )
        assertNotNull(result)
        assertNull(result.startMinute)
        assertNull(result.endMinute)
    }

    @Test
    fun parsesRelativeDatesDeterministically() {
        assertEquals(
            today.plusDays(1).toEpochDay(),
            NoticeParser.parseDateEpochDay("明天提交报告", today)
        )
    }
}
