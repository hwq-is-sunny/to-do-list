package com.campus.todo.domain.timetable

import com.campus.todo.ui.screens.courses.TimetableDraft
import com.campus.todo.ui.screens.courses.WeekMode
import com.campus.todo.util.MinuteParse
import java.util.Locale

/**
 * 课表截图 OCR 后的文本解析：时间范围、周次、地点、课程名、星期、课程类型。
 * 采用「按行（可继承上一行的星期）」+「按时间锚点」+「分隔符行」组合策略。
 */
object TimetableOcrTextParser {

    private val timeRangeRegex = Regex(
        "(\\d{1,2})[:：](\\d{2})\\s*[-~—至到－]\\s*(\\d{1,2})[:：](\\d{2})"
    )

    /** 无冒号分隔的节次 08 30 - 09 15（少见，容错） */
    private val timeRangeLooseRegex = Regex(
        "(\\d{1,2})\\s+(\\d{2})\\s*[-~—至到－]\\s*(\\d{1,2})\\s+(\\d{2})"
    )

    private val periodRegex = Regex("(\\d{1,2})\\s*[-~—至到]\\s*(\\d{1,2})\\s*节")

    private val weekRangeRegex = Regex(
        "(?:第\\s*)?(\\d{1,2})\\s*[-~—至到]\\s*(\\d{1,2})\\s*周"
    )

    private val singleWeekRegex = Regex("第\\s*(\\d{1,2})\\s*周")

    private val dayRegex = Regex(
        "(星期?[一二三四五六日天]|周\\s*[一二三四五六日天])"
    )

    fun parse(ocrText: String): List<TimetableDraft> {
        val text = normalize(ocrText)
        if (text.isBlank()) return emptyList()

        val fromLines = parseLineByLine(text)
        val fromAnchors = parseByTimeAnchors(text)
        val delimited = parseDelimitedLines(text)
        return mergeDrafts(fromLines + fromAnchors + delimited)
    }

    private fun mergeDrafts(all: List<TimetableDraft>): List<TimetableDraft> {
        if (all.isEmpty()) return emptyList()
        return all.distinctBy { d ->
            "${d.dayOfWeek}-${d.startMinute}-${d.endMinute}-${d.courseName}-${d.location}"
        }
    }

    private fun normalize(raw: String): String {
        var s = raw
        val full = "０１２３４５６７８９"
        val half = "0123456789"
        s = s.map { c ->
            val i = full.indexOf(c)
            if (i >= 0) half[i] else c
        }.joinToString("")
        s = s
            .replace('\uFF1A', ':')
            .replace('\uFF0D', '-')
            .replace('－', '-')
            .replace('—', '-')
            .replace('–', '-')
            .replace('～', '-')
            .replace('〜', '-')
        s = s.replace(Regex("[\t\r]+"), " ")
        s = s.replace(Regex(" +"), " ")
        return s.trim()
    }

    /** 管道/逗号分隔：课程名|周X|时间|地点 */
    private fun parseDelimitedLines(text: String): List<TimetableDraft> {
        val out = mutableListOf<TimetableDraft>()
        for (line in text.lines().map { it.trim() }.filter { it.isNotBlank() }) {
            val parts = line.split("|", "｜", ",").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size < 3) continue
            val courseName = parts[0]
            val day = parseDayToken(parts[1]) ?: continue
            val timePart = parts[2]
            val range = parseTimeRangeToken(timePart)
                ?: parsePeriodToken(timePart)
                ?: continue
            val loc = parts.getOrNull(3)?.takeIf { it.isNotBlank() && it != "-" }.orEmpty()
            val teacher = parts.getOrNull(4)?.takeIf { it.isNotBlank() && it != "-" }.orEmpty()
            val (sw, ew) = extractWeekRange(line) ?: (1 to 20)
            val wm = extractWeekMode(line)
            val ct = extractCourseType(line) ?: "必修"
            if (courseName.isNotBlank()) {
                out += TimetableDraft(
                    courseName = courseName,
                    teacher = teacher,
                    location = loc,
                    dayOfWeek = day,
                    startMinute = range.first,
                    endMinute = range.second,
                    startWeek = sw,
                    endWeek = ew,
                    weekMode = wm,
                    courseType = ct
                )
            }
        }
        return out
    }

    private fun parseLineByLine(text: String): List<TimetableDraft> {
        val out = mutableListOf<TimetableDraft>()
        var lastDay: Int? = null
        for (rawLine in text.lines().map { it.trim() }.filter { it.isNotBlank() }) {
            val line = rawLine
            if (line.count { it == '|' } >= 2) continue
            parseDayInLine(line)?.let { lastDay = it }
            val range = timeRangeRegex.find(line)?.let { m ->
                parsePairFromStrings(
                    "${m.groupValues[1]}:${m.groupValues[2]}",
                    "${m.groupValues[3]}:${m.groupValues[4]}"
                )
            } ?: timeRangeLooseRegex.find(line)?.let { m ->
                parsePairFromStrings(
                    "${m.groupValues[1]}:${m.groupValues[2]}",
                    "${m.groupValues[3]}:${m.groupValues[4]}"
                )
            } ?: extractTimeRange(line)
                ?: extractPeriodRange(line) ?: continue

            val day = parseDayInLine(line) ?: lastDay ?: continue
            val loc = extractLocation(line).orEmpty()
            val name = extractCourseName(line)
            if (name.length < 2) continue
            val (sw, ew) = extractWeekRange(line) ?: (1 to 20)
            val wm = extractWeekMode(line)
            val ct = extractCourseType(line) ?: "必修"
            val teacher = extractTeacher(line).orEmpty()
            out += TimetableDraft(
                courseName = name,
                teacher = teacher,
                location = loc,
                dayOfWeek = day,
                startMinute = range.first,
                endMinute = range.second,
                startWeek = sw,
                endWeek = ew,
                weekMode = wm,
                courseType = ct
            )
        }
        return out
    }

    private fun parseByTimeAnchors(text: String): List<TimetableDraft> {
        val out = mutableListOf<TimetableDraft>()
        for (match in timeRangeRegex.findAll(text)) {
            val a = "${match.groupValues[1]}:${match.groupValues[2]}"
            val b = "${match.groupValues[3]}:${match.groupValues[4]}"
            val range = parsePairFromStrings(a, b) ?: continue
            val lineStart = text.lastIndexOf('\n', match.range.first).let { if (it < 0) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', match.range.last).let { if (it < 0) text.length else it }
            val line = text.substring(lineStart, lineEnd).trim()
            if (line.length < 4) continue
            val day = parseDayInLine(line) ?: scanNearbyDay(text, lineStart, lineEnd) ?: continue
            val loc = extractLocation(line).orEmpty()
            val name = extractCourseName(line)
            if (name.length < 2) continue
            val (sw, ew) = extractWeekRange(line) ?: extractWeekRange(text) ?: (1 to 20)
            val wm = extractWeekMode(line)
            val ct = extractCourseType(line) ?: extractCourseType(text) ?: "必修"
            val teacher = extractTeacher(line).orEmpty()
            out += TimetableDraft(
                courseName = name,
                teacher = teacher,
                location = loc,
                dayOfWeek = day,
                startMinute = range.first,
                endMinute = range.second,
                startWeek = sw,
                endWeek = ew,
                weekMode = wm,
                courseType = ct
            )
        }
        return out.distinctBy { "${it.dayOfWeek}-${it.startMinute}-${it.endMinute}-${it.courseName}" }
    }

    private fun scanNearbyDay(text: String, lineStart: Int, lineEnd: Int): Int? {
        val windowStart = (lineStart - 120).coerceAtLeast(0)
        val windowEnd = (lineEnd + 120).coerceAtMost(text.length)
        return parseDayInLine(text.substring(windowStart, windowEnd))
    }

    private fun parsePairFromStrings(start: String, end: String): Pair<Int, Int>? {
        val s = MinuteParse.parseMinuteOfDay(start.replace('：', ':')) ?: return null
        val e = MinuteParse.parseMinuteOfDay(end.replace('：', ':')) ?: return null
        if (s >= e) return null
        return s to e
    }

    private fun parseTimeRangeToken(part: String): Pair<Int, Int>? {
        val normalized = part.replace("~", "-").replace("—", "-").replace("至", "-")
        val dash = normalized.indexOf('-')
        if (dash > 0) {
            val left = normalized.substring(0, dash).trim()
            val right = normalized.substring(dash + 1).trim()
            parsePairFromStrings(left, right)?.let { return it }
        }
        return timeRangeRegex.find(part)?.let { m ->
            parsePairFromStrings(
                "${m.groupValues[1]}:${m.groupValues[2]}",
                "${m.groupValues[3]}:${m.groupValues[4]}"
            )
        }
    }

    private fun parsePeriodToken(part: String): Pair<Int, Int>? {
        val m = periodRegex.find(part) ?: return null
        return periodRangeToMinute(
            m.groupValues[1].toIntOrNull() ?: return null,
            m.groupValues[2].toIntOrNull() ?: return null
        )
    }

    private fun parseDayToken(part: String): Int? = parseDayInLine(part)

    private fun extractTimeRange(line: String): Pair<Int, Int>? {
        val m = timeRangeRegex.find(line) ?: return null
        return parsePairFromStrings(
            "${m.groupValues[1]}:${m.groupValues[2]}",
            "${m.groupValues[3]}:${m.groupValues[4]}"
        )
    }

    private fun extractPeriodRange(line: String): Pair<Int, Int>? {
        val m = periodRegex.find(line) ?: return null
        return periodRangeToMinute(
            m.groupValues[1].toIntOrNull() ?: return null,
            m.groupValues[2].toIntOrNull() ?: return null
        )
    }

    private fun periodRangeToMinute(startPeriod: Int, endPeriod: Int): Pair<Int, Int>? {
        if (startPeriod <= 0 || endPeriod < startPeriod) return null
        val periodStartMinute = mapOf(
            1 to 8 * 60,
            2 to 8 * 60 + 55,
            3 to 10 * 60 + 10,
            4 to 11 * 60 + 5,
            5 to 14 * 60,
            6 to 14 * 60 + 55,
            7 to 16 * 60 + 10,
            8 to 17 * 60 + 5,
            9 to 19 * 60,
            10 to 19 * 60 + 55,
            11 to 21 * 60,
            12 to 21 * 60 + 55
        )
        val startMinute = periodStartMinute[startPeriod] ?: return null
        val endMinute = (periodStartMinute[endPeriod] ?: return null) + 45
        if (startMinute >= endMinute) return null
        return startMinute to endMinute
    }

    private fun extractWeekRange(raw: String): Pair<Int, Int>? {
        weekRangeRegex.find(raw)?.let { m ->
            val a = m.groupValues[1].toIntOrNull() ?: return@let
            val b = m.groupValues[2].toIntOrNull() ?: return@let
            if (a > 0 && b >= a) return a to b
        }
        singleWeekRegex.find(raw)?.let { m ->
            val w = m.groupValues[1].toIntOrNull() ?: return@let
            if (w > 0) return w to w
        }
        return null
    }

    private fun extractWeekMode(raw: String): WeekMode = when {
        raw.contains("单周") -> WeekMode.ODD
        raw.contains("双周") -> WeekMode.EVEN
        else -> WeekMode.EVERY
    }

    private fun extractTeacher(raw: String): String? {
        val r = Regex("(?:教师|老师|主讲)[:：]?\\s*([\\u4e00-\\u9fa5A-Za-z·•. ]{2,16})")
        return r.find(raw)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null }
    }

    /**
     * 地点：B310、教学楼A101、艺术楼305、教三-204、教室：xxx
     */
    fun extractLocation(raw: String): String? {
        val candidates = mutableListOf<String>()
        Regex("(?:教室|地点|教学楼|上课地点)[:：]\\s*([A-Za-z0-9\\u4e00-\\u9fa5#\\-]{2,24})")
            .find(raw)?.groupValues?.getOrNull(1)?.trim()?.let { candidates.add(it) }
        Regex("(教[一二三四五六七八九十0-9]{0,4}[-－][A-Za-z0-9\\u4e00-\\u9fa5]{1,12})")
            .find(raw)?.value?.let { candidates.add(it.trim()) }
        Regex("([\\u4e00-\\u9fa5]{2,8}楼\\s*[A-Za-z0-9\\-]{1,10})")
            .find(raw)?.value?.let { candidates.add(it.replace(" ", "").trim()) }
        Regex("(?<![0-9年月日:])([A-Za-z]\\d{2,4})(?![0-9:：])")
            .findAll(raw).forEach { candidates.add(it.groupValues[1]) }
        Regex("([A-Za-z]{1,3}\\d{3,4})")
            .findAll(raw).forEach { candidates.add(it.value) }
        return candidates
            .map { it.trim() }
            .filter { it.length in 2..24 }
            .distinct()
            .maxByOrNull { it.length }
            ?.ifBlank { null }
    }

    private fun extractCourseType(raw: String): String? = when {
        raw.contains("必修") || raw.contains("学位课") -> "必修"
        raw.contains("选修") || raw.contains("任选") || raw.contains("限选") ||
            raw.contains("公选") || raw.contains("通识") || raw.contains("通修") -> "选修"
        else -> null
    }

    private fun parseDayInLine(raw: String): Int? {
        val onlyDigit = raw.trim()
        onlyDigit.toIntOrNull()?.let { n -> if (n in 1..7) return n }
        val compact = raw.replace(" ", "")
        dayRegex.find(compact)?.let { m ->
            dayFromChinese(m.value)?.let { return it }
        }
        val lower = raw.lowercase(Locale.getDefault())
        return when {
            lower.contains("周一") || lower.contains("星期一") -> 1
            lower.contains("周二") || lower.contains("星期二") -> 2
            lower.contains("周三") || lower.contains("星期三") -> 3
            lower.contains("周四") || lower.contains("星期四") -> 4
            lower.contains("周五") || lower.contains("星期五") -> 5
            lower.contains("周六") || lower.contains("星期六") -> 6
            lower.contains("周日") || lower.contains("星期天") || lower.contains("星期日") || lower.contains("周天") -> 7
            else -> null
        }
    }

    private fun dayFromChinese(token: String): Int? {
        val t = token.replace("星期", "周")
        return when {
            t.contains("周一") -> 1
            t.contains("周二") -> 2
            t.contains("周三") -> 3
            t.contains("周四") -> 4
            t.contains("周五") -> 5
            t.contains("周六") -> 6
            t.contains("周日") || t.contains("周天") -> 7
            else -> null
        }
    }

    /**
     * 从行内去掉时间、星期、周次、地点、教师、类型后取课程名。
     */
    private fun extractCourseName(line: String): String {
        var s = line
        timeRangeRegex.replace(s, " ").let { s = it }
        timeRangeLooseRegex.replace(s, " ").let { s = it }
        periodRegex.replace(s, " ").let { s = it }
        s = dayRegex.replace(s, " ")
        s = weekRangeRegex.replace(s, " ")
        s = singleWeekRegex.replace(s, " ")
        extractLocation(line)?.let { loc ->
            if (loc.length >= 2) s = s.replace(loc, " ")
        }
        extractTeacher(line)?.let { t -> s = s.replace(t, " ") }
        s = s.replace(Regex("(?:必修|选修|单周|双周|教师|老师|主讲)[:：]?"), " ")
        s = s.replace(Regex("[,，|｜]"), " ")
        s = s.replace(Regex("\\s+"), " ").trim()
        val words = s.split(" ").map { it.trim() }.filter { it.length >= 2 }
        val chinese = words.filter { it.any { ch -> ch in '\u4e00'..'\u9fff' } }
        val best = chinese.maxByOrNull { it.length } ?: words.maxByOrNull { it.length } ?: ""
        return best.take(40).trim()
    }
}
