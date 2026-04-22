package com.campus.todo.data.parser

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.regex.Pattern

/**
 * 中文通知本地规则解析：标题、类型、日期、时间、地点、备注、优先级。
 */
object NoticeParser {

    private data class TimeParse(
        val startMinuteOfDay: Int?,
        val endMinuteOfDay: Int?,
        val isDeadline: Boolean
    )

    private val actionPattern = Pattern.compile(
        "(提交|完成|参加|上交|领取|前往|签到|开会|开班会|班会|答辩|汇报|考试|测验|训练|上课|听取|观看)([^，,。；;\\n]{1,24})"
    )
    private val courseNamePattern = Pattern.compile("《([^》]+)》|【([^】]+)】")
    private val periodPattern = Pattern.compile("第\\s*(\\d{1,2})\\s*[-到至~]\\s*(\\d{1,2})\\s*节")
    private val ymdPattern = Pattern.compile("(\\d{4})[\\-/.年](\\d{1,2})[\\-/.月](\\d{1,2})")
    private val mdPattern = Pattern.compile("(?<!\\d)(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日")
    private val slashMdPattern = Pattern.compile("(?<!\\d)(\\d{1,2})/(\\d{1,2})(?!\\d)")
    private val isoPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})")
    private val timeRangePattern = Pattern.compile("(\\d{1,2})[:：](\\d{2})\\s*[-到至~]\\s*(\\d{1,2})[:：](\\d{2})")
    private val hmPattern = Pattern.compile("(?<!\\d)(\\d{1,2})[:：](\\d{2})(?!\\d)")
    private val locKeywordPattern = Pattern.compile(
        "(?:地点|地址|教室|在|于|到|前往)[:：]?\\s*([^，,。；;\\n]{2,32})"
    )
    private val buildingRoomPattern = Pattern.compile(
        "([\\u4e00-\\u9fa5]{1,6}(?:楼|苑|馆|区|教|主楼|综合楼)[\\s\\-]*[A-Za-z0-9\\u4e00-\\u9fa5\\-]{1,16})"
    )

    fun parse(raw: String, today: LocalDate, zone: ZoneId): NoticeParseResult {
        if (raw.trim().isEmpty()) {
            return fallback("待确认事项", today, zone, raw)
        }
        val text = raw.trim()

        val type = parseType(text)
        val dateDay = parseDateEpochDay(text, today)
        val timePair = parseTimeRange(text)
        val location = parseLocation(text)
        val title = parseTitle(text, type)
        val courseHint = parseCourseNameHint(text, type, title)
        val priorityTag = parsePriority(text)
        val note = buildNote(text, title, location, dateDay, timePair, type)

        val startMin = timePair?.startMinuteOfDay
        val endMin = timePair?.endMinuteOfDay
        val dateForInstant = dateDay?.let { LocalDate.ofEpochDay(it) } ?: today

        val startAt = startMin?.let { combineToEpoch(dateForInstant, it, zone) }
        val endAt = endMin?.let { combineToEpoch(dateForInstant, it, zone) }
        val dueAt = when {
            type == "meeting" && startAt != null -> startAt
            timePair?.isDeadline == true && startMin != null ->
                combineToEpoch(dateForInstant, startMin, zone)
            type == "deadline" || type == "task" || type == "exam" ->
                endAt ?: startAt ?: dateDay?.let {
                    LocalDate.ofEpochDay(it).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
                }
            else -> endAt ?: startAt
        }

        val conf = buildString {
            append("本地规则：类型=").append(type)
            if (dateDay != null) append("·日期")
            if (startMin != null) append("·时间")
            if (location != null) append("·地点")
        }

        return NoticeParseResult(
            title = title,
            noticeType = type,
            dateEpochDay = dateDay,
            startMinute = startMin,
            endMinute = endMin,
            location = location,
            note = note?.trim()?.ifBlank { null },
            priorityTag = priorityTag,
            dueAtEpoch = dueAt,
            startAtEpoch = if (timePair?.isDeadline == true) null else startAt,
            endAtEpoch = endAt,
            courseNameHint = courseHint,
            confidenceNote = conf
        )
    }

    private fun combineToEpoch(day: LocalDate, minuteOfDay: Int, zone: ZoneId): Long {
        val h = (minuteOfDay / 60).coerceIn(0, 23)
        val m = (minuteOfDay % 60).coerceIn(0, 59)
        return LocalDateTime.of(day, LocalTime.of(h, m)).atZone(zone).toInstant().toEpochMilli()
    }

    private fun fallback(title: String, today: LocalDate, zone: ZoneId, raw: String) = NoticeParseResult(
        title = title,
        noticeType = "task",
        dateEpochDay = today.toEpochDay(),
        startMinute = null,
        endMinute = null,
        location = null,
        note = raw.trim().ifBlank { null },
        priorityTag = "normal",
        dueAtEpoch = null,
        startAtEpoch = null,
        endAtEpoch = null,
        courseNameHint = null,
        confidenceNote = "本地规则：原文保留，请手动补全"
    )

    fun parseTitle(text: String, type: String): String {
        val am = actionPattern.matcher(text)
        if (am.find()) {
            val verb = am.group(1) ?: ""
            var rest = (am.group(2) ?: "").trim()
            if (verb == "开" && rest.startsWith("班会")) {
                return "开班会"
            }
            if (verb.endsWith("班会")) {
                return (verb + rest).take(24)
            }
            val t = (verb + rest).trim()
            if (t.length in 2..30) return t.trimEnd('，', ',', '。', ' ')
        }
        if (text.contains("班会")) {
            val idx = text.indexOf("班会")
            val start = (idx - 4).coerceAtLeast(0)
            return text.substring(start, (idx + 2).coerceAtMost(text.length)).trim().take(20)
        }
        val cm = courseNamePattern.matcher(text)
        if (cm.find()) {
            val n = cm.group(1) ?: cm.group(2)
            if (!n.isNullOrBlank()) {
                val suffix = when (type) {
                    "course" -> "上课"
                    "exam" -> "考试"
                    else -> ""
                }
                return (n + suffix).take(28)
            }
        }
        val courseLike = Pattern.compile("([\\u4e00-\\u9fa5]{2,10})(?=在教|在.*楼|第\\d)").matcher(text)
        if (type == "course" && courseLike.find()) {
            val name = courseLike.group(1) ?: ""
            if (name.isNotBlank()) return "${name}上课".take(28)
        }
        val first = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val clipped = first.take(20).trim()
        return if (clipped.isNotBlank()) clipped else "待确认事项"
    }

    fun parseType(text: String): String {
        var exam = 0
        var deadline = 0
        var meeting = 0
        var course = 0
        var task = 0
        var activity = 0

        if (Regex("考试|测验|期中|期末|笔试").containsMatchIn(text)) exam += 10
        if (Regex("作业|报告|实验|提交|截止|上交|论文|ddl", RegexOption.IGNORE_CASE).containsMatchIn(text)) deadline += 8
        if (Regex("逾期|最晚|前提交|不候").containsMatchIn(text)) deadline += 4
        if (Regex("开会|会议|班会|答辩|汇报|座谈").containsMatchIn(text)) meeting += 9
        if (Regex("上课|第\\s*\\d{1,2}\\s*[-到至~]?\\s*\\d{0,2}\\s*节|课程").containsMatchIn(text)) course += 7
        if (Regex("讲座|活动|比赛|训练|运动会|宣讲").containsMatchIn(text)) activity += 6
        if (Regex("领取|前往|签到|完成|参加").containsMatchIn(text)) task += 5

        val scores = listOf(
            "exam" to exam,
            "deadline" to deadline,
            "meeting" to meeting,
            "course" to course,
            "task" to task,
            "activity" to activity
        )
        val maxScore = scores.maxOf { it.second }
        if (maxScore == 0) return "task"
        val order = listOf("exam", "deadline", "meeting", "course", "task", "activity")
        return order.first { t -> scores.find { it.first == t }!!.second == maxScore }
    }

    fun parseDateEpochDay(text: String, today: LocalDate): Long? {
        when {
            Regex("后天").containsMatchIn(text) -> return today.plusDays(2).toEpochDay()
            Regex("明天|明日").containsMatchIn(text) -> return today.plusDays(1).toEpochDay()
            Regex("今天|今日").containsMatchIn(text) -> return today.toEpochDay()
            Regex("今晚|明晚").containsMatchIn(text) -> {
                return if (text.contains("明晚")) today.plusDays(1).toEpochDay() else today.toEpochDay()
            }
        }

        parseWeekDate(text, "本", today)?.let { return it.toEpochDay() }
        parseWeekDate(text, "下", today)?.let { return it.toEpochDay() }
        parseWeekdayToken(text, today)?.let { return it.toEpochDay() }

        val iso = isoPattern.matcher(text)
        if (iso.find()) {
            val d = LocalDate.parse(iso.group(1))
            return d.toEpochDay()
        }
        val ymd = ymdPattern.matcher(text)
        if (ymd.find()) {
            val y = ymd.group(1).toInt()
            val mo = ymd.group(2).toInt()
            val day = ymd.group(3).toInt()
            return LocalDate.of(y, mo, day).toEpochDay()
        }
        val md = mdPattern.matcher(text)
        if (md.find()) {
            val mo = md.group(1).toInt()
            val day = md.group(2).toInt()
            var y = today.year
            var candidate = LocalDate.of(y, mo, day)
            if (candidate.isBefore(today.minusDays(60))) {
                y += 1
                candidate = LocalDate.of(y, mo, day)
            }
            return candidate.toEpochDay()
        }
        val sl = slashMdPattern.matcher(text)
        if (sl.find()) {
            val mo = sl.group(1).toInt()
            val day = sl.group(2).toInt()
            var y = today.year
            var candidate = LocalDate.of(y, mo, day)
            if (candidate.isBefore(today.minusDays(60))) {
                y += 1
                candidate = LocalDate.of(y, mo, day)
            }
            return candidate.toEpochDay()
        }
        return null
    }

    private fun parseWeekDate(text: String, prefix: String, today: LocalDate): LocalDate? {
        val p = Pattern.compile(
            prefix + "周([一二三四五六日天])"
        )
        val m = p.matcher(text)
        if (!m.find()) return null
        val d = chineseWeekdayToValue(m.group(1) ?: return null) ?: return null
        val anchor = if (prefix == "下") today.plusWeeks(1) else today
        return anchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(d)))
    }

    private fun parseWeekdayToken(text: String, today: LocalDate): LocalDate? {
        val p = Pattern.compile("周([一二三四五六日天])|星期([一二三四五六日天])")
        val m = p.matcher(text)
        if (!m.find()) return null
        val g = m.group(1) ?: m.group(2) ?: return null
        val d = chineseWeekdayToValue(g) ?: return null
        return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(d)))
    }

    private fun chineseWeekdayToValue(c: String): Int? = when (c) {
        "一" -> 1
        "二" -> 2
        "三" -> 3
        "四" -> 4
        "五" -> 5
        "六" -> 6
        "日", "天" -> 7
        else -> null
    }

    private fun parseTimeRange(text: String): TimeParse? {
        val isBefore = Regex("(前|之前)").containsMatchIn(text)
        val tr = timeRangePattern.matcher(text)
        if (tr.find()) {
            val h1 = tr.group(1).toInt()
            val m1 = tr.group(2).toInt()
            val h2 = tr.group(3).toInt()
            val m2 = tr.group(4).toInt()
            return TimeParse(h1 * 60 + m1, h2 * 60 + m2, false)
        }
        val hm = hmPattern.matcher(text)
        if (hm.find()) {
            val h = hm.group(1).toInt()
            val m = hm.group(2).toInt()
            val minute = h * 60 + m
            return TimeParse(minute, null, isBefore)
        }
        parseChineseColloquialTime(text)?.let { (h, m) ->
            return TimeParse(h * 60 + m, null, isBefore)
        }
        val pm = periodPattern.matcher(text)
        if (pm.find()) {
            val a = pm.group(1).toInt()
            val b = pm.group(2).toInt()
            val (s, e) = periodsToMinutes(a, b)
            return TimeParse(s, e, false)
        }
        return null
    }

    /** 解析「下午三点」「晚上7点」「上午8点20」等 */
    private fun parseChineseColloquialTime(text: String): Pair<Int, Int>? {
        val day = when {
            text.contains("凌晨") || text.contains("清晨") -> "am"
            text.contains("上午") || text.contains("早") -> "am"
            text.contains("中午") -> "noon"
            text.contains("下午") -> "pm"
            text.contains("晚上") || text.contains("晚间") -> "pm"
            else -> ""
        }
        val hourWord = listOf(
            "十二" to 12, "十一" to 11, "十" to 10, "九" to 9, "八" to 8,
            "七" to 7, "六" to 6, "五" to 5, "四" to 4, "三" to 3, "两" to 2, "二" to 2, "一" to 1
        )
        var hour: Int? = null
        var minute = 0
        val digitHour = Pattern.compile("(\\d{1,2})\\s*点(?:钟)?").matcher(text)
        if (digitHour.find()) {
            hour = digitHour.group(1).toInt()
            val afterDot = text.substring(digitHour.end()).trimStart()
            val mm = Pattern.compile("^(\\d{1,2})\\s*分").matcher(afterDot)
            if (mm.find()) minute = mm.group(1).toInt()
            if (afterDot.startsWith("半")) minute = 30
        } else {
            for ((w, v) in hourWord) {
                if (text.contains(w + "点") || text.contains(w + "点钟")) {
                    hour = v
                    break
                }
            }
            if (text.contains("点半")) minute = 30
        }
        if (hour == null) return null
        var h = hour!!
        when (day) {
            "pm" -> if (h in 1..11) h += 12
            "noon" -> if (h in 1..6) h += 12
            "am" -> { }
            else -> {
                if (Regex("下午|晚上|晚间").containsMatchIn(text) && h in 1..11) h += 12
            }
        }
        if (h == 12 && day == "pm" && text.contains("下午")) { }
        if (h == 24) h = 12
        return h.coerceIn(0, 23) to minute.coerceIn(0, 59)
    }

    /** 粗略：从第 1 节 8:00 起每节 95 分钟 */
    fun periodsToMinutes(startP: Int, endP: Int): Pair<Int, Int> {
        val base = 8 * 60
        val slot = 95
        val s = base + (startP - 1).coerceAtLeast(0) * slot
        val e = base + endP.coerceAtLeast(startP) * slot - 10
        return s.coerceIn(0, 1439) to e.coerceIn(0, 1439)
    }

    fun parseLocation(text: String): String? {
        val lk = locKeywordPattern.matcher(text)
        if (lk.find()) {
            val s = lk.group(1)?.trim()?.take(32)
            if (!s.isNullOrBlank()) return cleanLoc(s)
        }
        val teach = Pattern.compile("(教[一二三四五六七八九十0-9]{0,4}[\\-–·][A-Za-z0-9\\u4e00-\\u9fa5\\-]{1,12})").matcher(text)
        if (teach.find()) return cleanLoc(teach.group(1) ?: return null)
        val br = buildingRoomPattern.matcher(text)
        if (br.find()) {
            return cleanLoc(br.group(1)?.trim() ?: return null)
        }
        val roomOnly = Pattern.compile("([A-Za-z]\\d{2,4})").matcher(text)
        if (roomOnly.find()) return roomOnly.group(1)
        return null
    }

    private fun cleanLoc(s: String): String = s.trimEnd('。', '，', ',')

    fun parsePriority(text: String): String {
        if (Regex("立即|今天内|最晚|逾期|紧急").containsMatchIn(text)) return "urgent"
        if (Regex("截止|尽快|必须|重要|务必").containsMatchIn(text)) return "high"
        return "normal"
    }

    fun parseCourseNameHint(text: String, type: String, title: String): String? {
        val cm = courseNamePattern.matcher(text)
        if (cm.find()) return cm.group(1) ?: cm.group(2)
        if (type != "course") return null
        val m = Pattern.compile("([\\u4e00-\\u9fa5]{2,8})(?=在教|在.*楼|第\\d)").matcher(text)
        if (m.find()) return m.group(1)
        return title.removeSuffix("上课").removeSuffix("课程").takeIf { it.length in 2..10 }
    }

    private fun buildNote(
        text: String,
        title: String,
        location: String?,
        dateEpochDay: Long?,
        timePair: TimeParse?,
        type: String
    ): String? {
        val chunks = mutableListOf<String>()
        val used = mutableSetOf<String>()
        if (title.isNotBlank() && text.contains(title)) used.add(title)
        location?.let { if (text.contains(it)) used.add(it) }

        val periodM = periodPattern.matcher(text)
        if (periodM.find()) {
            periodM.group()?.let { chunks.add(it) }
        }
        Regex("下周[^，,。；;\\n]{1,16}").findAll(text).forEach { chunks.add(it.value.trim()) }
        if (Regex("所有人[^。]{1,16}").containsMatchIn(text)) {
            val m = Regex("所有人[^。]{1,16}").find(text)
            m?.value?.let { chunks.add(it.trim()) }
        }
        if (Regex("逾期[^，,。]{0,8}").containsMatchIn(text)) {
            val m = Regex("逾期[^，,。]{0,8}").find(text)
            m?.value?.let { chunks.add(it.trim()) }
        }

        val manual = chunks.distinct().joinToString("；").ifBlank { null }
        if (manual != null) return manual

        var remainder = text
        title.takeIf { it.length > 1 }?.let { remainder = remainder.replace(it, "") }
        location?.let { remainder = remainder.replace(it, "") }
        dateEpochDay?.let {
            val d = LocalDate.ofEpochDay(it)
            remainder = remainder.replace("明天", "").replace("今天", "").replace("后天", "")
            remainder = remainder.replace("${d.monthValue}月${d.dayOfMonth}日", "")
        }
        remainder = remainder.replace(Regex("\\s+"), " ").trim()
        if (remainder.length > 80) remainder = remainder.take(80) + "…"
        return remainder.trim().ifBlank { null }
    }
}
