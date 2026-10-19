package com.campus.todo.network

import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Lightweight client-side validation for untrusted text and model responses.
 *
 * These checks are defense-in-depth only. Pattern matching is not a complete
 * prompt-injection defense; production systems should enforce policy server-side.
 */
internal object AiSecurityGuard {
    const val MAX_NOTICE_CHARS = 8_000
    const val MAX_OCR_CHARS = 12_000

    private val injectionPatterns = listOf(
        Regex(
            """(?i)\b(ignore|disregard|override)\s+(all\s+)?(previous|above|system|developer)\s+(instructions?|prompts?|rules?)\b"""
        ),
        Regex(
            """(?i)\b(reveal|print|expose|leak)\b.{0,50}\b(system prompt|api[\s_-]*key|secret|credential|token)\b"""
        ),
        Regex("""忽略.{0,10}(之前|上面|系统|开发者).{0,10}(指令|提示词|规则)"""),
        Regex("""(泄露|输出|打印|告诉我).{0,20}(系统提示词|API[\s_-]*Key|密钥|密码|令牌)"""),
        Regex("""(绕过|解除).{0,8}(安全|限制|规则)""")
    )

    private val timePattern = Regex("""(?:[01]\d|2[0-3]):[0-5]\d""")
    private val timeRangePattern =
        Regex("""((?:[01]\d|2[0-3]):[0-5]\d)-((?:[01]\d|2[0-3]):[0-5]\d)""")
    private val datePattern = Regex("""\d{4}-\d{2}-\d{2}""")

    data class ValidatedTaskSuggestion(
        val title: String,
        val description: String?,
        val courseHint: String?,
        val location: String?,
        val dueDateIso: String?,
        val startTime: String?,
        val endTime: String?,
        val taskType: TaskType,
        val urgency: UrgencyLevel,
        val confidence: Float
    )

    /**
     * Trims external text, enforces a size limit, and blocks a small set of
     * high-confidence instruction-override patterns. This is not a classifier.
     */
    fun validateExternalText(raw: String, maxChars: Int, label: String): String {
        val text = raw.trim()
        require(text.isNotEmpty()) { "$label 不能为空" }
        require(text.length <= maxChars) { "$label 超过长度限制（最多 $maxChars 个字符）" }
        require('\u0000' !in text) { "$label 包含非法控制字符" }
        require(injectionPatterns.none { it.containsMatchIn(text) }) {
            "$label 命中了可疑的提示注入规则，请检查原文后再试"
        }
        return text
    }


    /**
     * Accept only HTTPS provider endpoints. API keys must never be sent over
     * cleartext HTTP or embedded in a URL.
     */
    fun validateAiBaseUrl(raw: String): String {
        val value = raw.trim()
        val url = value.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("AI 接口地址格式无效")
        require(url.scheme == "https") { "AI 接口必须使用 HTTPS" }
        require(url.username.isEmpty() && url.password.isEmpty()) {
            "AI 接口地址不能包含用户名或密码"
        }
        require(url.query == null && url.fragment == null) {
            "AI 接口地址不能包含查询参数或片段"
        }
        require(url.host.isNotBlank()) { "AI 接口地址缺少主机名" }
        return url.toString().trimEnd('/')
    }

    fun validateTaskSuggestion(json: JSONObject): ValidatedTaskSuggestion {
        val title = requiredString(json, "title", 120)
        val description = optionalString(json, "description", 1_200)
        val courseHint = optionalString(json, "courseHint", 100)
        val location = optionalString(json, "location", 160)
        val dueDate = optionalString(json, "dueDateISO", 10)
        val startTime = optionalString(json, "startTime", 5)
        val endTime = optionalString(json, "endTime", 5)

        dueDate?.let {
            require(datePattern.matches(it)) { "AI 返回了无效的日期格式" }
            LocalDate.parse(it)
        }
        startTime?.let {
            require(timePattern.matches(it)) { "AI 返回了无效的开始时间" }
            LocalTime.parse(it)
        }
        endTime?.let {
            require(timePattern.matches(it)) { "AI 返回了无效的结束时间" }
            LocalTime.parse(it)
        }
        require((startTime == null && endTime == null) || dueDate != null) {
            "AI 返回了时间，但没有返回对应日期"
        }
        if (startTime != null && endTime != null) {
            require(LocalTime.parse(endTime).isAfter(LocalTime.parse(startTime))) {
                "AI 返回的结束时间必须晚于开始时间"
            }
        }

        val typeName = requiredString(json, "taskType", 32).uppercase(Locale.ROOT)
        val urgencyName = requiredString(json, "urgency", 32).uppercase(Locale.ROOT)
        val taskType = runCatching { TaskType.valueOf(typeName) }
            .getOrElse { throw IllegalArgumentException("AI 返回了未知的任务类型") }
        val urgency = runCatching { UrgencyLevel.valueOf(urgencyName) }
            .getOrElse { throw IllegalArgumentException("AI 返回了未知的优先级") }

        val confidenceValue = json.opt("confidence")
        require(confidenceValue is Number) { "AI 返回的 confidence 必须是数字" }
        val confidence = confidenceValue.toDouble()
        require(confidence.isFinite() && confidence in 0.0..1.0) {
            "AI 返回的 confidence 必须在 0 到 1 之间"
        }

        return ValidatedTaskSuggestion(
            title = title,
            description = description,
            courseHint = courseHint,
            location = location,
            dueDateIso = dueDate,
            startTime = startTime,
            endTime = endTime,
            taskType = taskType,
            urgency = urgency,
            confidence = confidence.toFloat()
        )
    }

    /**
     * Checks the exact row shape expected by timetable import:
     * course|weekday|HH:mm-HH:mm|location|teacher
     */
    fun validateTimetableOutput(raw: String): String {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        require(lines.isNotEmpty()) { "AI 没有返回可导入的课表内容" }
        require(lines.size <= 100) { "AI 返回的课表行数过多" }

        val weekdays = setOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        lines.forEach { line ->
            val columns = line.split('|').map { it.trim() }
            require(columns.size == 5) { "课表行不是 5 列格式" }
            require(columns[0].isNotBlank() && columns[0].length <= 80) {
                "课表行的课程名无效"
            }
            require(columns[1] in weekdays) { "课表行的星期格式无效" }
            require(timeRangePattern.matches(columns[2])) {
                "课表行的时间格式无效"
            }
            val times = timeRangePattern.matchEntire(columns[2])!!
            val start = LocalTime.parse(times.groupValues[1])
            val end = LocalTime.parse(times.groupValues[2])
            require(end.isAfter(start)) { "课表行的结束时间必须晚于开始时间" }
            require(columns[3].isNotBlank() && columns[4].isNotBlank()) {
                "课表行的地点和教师未知时请使用 - 占位"
            }
            require(columns[3].length <= 100 && columns[4].length <= 100) {
                "课表行的地点或教师字段过长"
            }
        }
        return lines.joinToString("\n")
    }

    private fun requiredString(json: JSONObject, key: String, maxLength: Int): String {
        val value = optionalString(json, key, maxLength)
        require(!value.isNullOrBlank()) { "AI 返回缺少必需字段：$key" }
        return value
    }

    private fun optionalString(json: JSONObject, key: String, maxLength: Int): String? {
        val raw = json.opt(key) ?: return null
        if (raw === JSONObject.NULL) return null
        require(raw is String) { "AI 返回字段 $key 的类型无效" }
        val value = raw.trim()
        if (value.isEmpty()) return null
        require(value.length <= maxLength) { "AI 返回字段 $key 超过长度限制" }
        require('\u0000' !in value) { "AI 返回字段 $key 包含非法控制字符" }
        return value
    }
}
