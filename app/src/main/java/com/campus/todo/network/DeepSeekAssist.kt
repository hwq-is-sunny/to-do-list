package com.campus.todo.network

import android.content.Context
import com.campus.todo.R
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class AiParseSuggestion(
    val title: String?,
    val description: String? = null,
    val courseHint: String?,
    val location: String? = null,
    val dueAtEpoch: Long?,
    val startAtEpoch: Long? = null,
    val endAtEpoch: Long? = null,
    val taskType: TaskType?,
    val urgency: UrgencyLevel?,
    val confidenceScore: Float = 0.7f
)

class DeepSeekAssist(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val fallbackApiKey: String
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun parseNotification(raw: String): Result<AiParseSuggestion> = withContext(Dispatchers.IO) {
        val settings = settingsStore.currentSettings()
        if (!settings.aiParseEnabled) {
            return@withContext Result.failure(IllegalStateException("AI parsing disabled by settings"))
        }
        val apiKey = settings.aiApiKey.ifBlank { fallbackApiKey }
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(context.getString(R.string.ai_missing_api_key))
            )
        }
        val system = "你是校园通知解析助手。只输出一个 JSON 对象，不要代码块或多余文字。键：title,description,courseHint,location,dueDateISO(yyyy-MM-dd),startTime(HH:mm),endTime(HH:mm),taskType(HOMEWORK/EXAM/SIGN_IN/CLASS/ANNOUNCEMENT/PERSONAL/OTHER),urgency(NORMAL/IMPORTANT/URGENT),confidence(0~1)."
        val user = "从以下文本提取信息：\n\n${raw.take(8000)}"
        val bodyJson = JSONObject().apply {
            put("model", settings.normalizedAiModel)
            put(
                "messages",
                org.json.JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", system) })
                    put(JSONObject().apply { put("role", "user"); put("content", user) })
                }
            )
            put("temperature", 0.2)
        }
        val request = Request.Builder()
            .url("${settings.normalizedAiBaseUrl}/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMedia))
            .build()
        runCatching {
            client.newBuilder()
                .callTimeout(settings.normalizedTimeoutSeconds.toLong(), TimeUnit.SECONDS)
                .build()
                .newCall(request)
                .execute()
                .use { resp ->
                val str = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: ${str.take(200)}")
                }
                val root = JSONObject(str)
                val content = root.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
                val jsonStr = extractJsonObject(content)
                val o = JSONObject(jsonStr)
                val title = o.optString("title", "").takeIf { it.isNotBlank() }
                val courseHint = o.optString("courseHint", "").takeIf { it.isNotBlank() }
                val dueIso = o.optString("dueDateISO", "").takeIf { it.isNotBlank() }
                val dueEpoch = dueIso?.let { parseIsoToEndOfDayEpoch(it) }
                val startAt = parseDateTime(dueIso, o.optString("startTime", ""))
                val endAt = parseDateTime(dueIso, o.optString("endTime", ""))
                val taskType = o.optString("taskType", "OTHER").let { parseTaskType(it) }
                val urgency = o.optString("urgency", "NORMAL").let { parseUrgency(it) }
                AiParseSuggestion(
                    title = title,
                    description = o.optString("description", "").takeIf { it.isNotBlank() },
                    courseHint = courseHint,
                    location = o.optString("location", "").takeIf { it.isNotBlank() },
                    dueAtEpoch = dueEpoch,
                    startAtEpoch = startAt,
                    endAtEpoch = endAt,
                    taskType = taskType,
                    urgency = urgency,
                    confidenceScore = o.optDouble("confidence", 0.7).toFloat().coerceIn(0f, 1f)
                )
            }
        }
    }

    /**
     * 将课表截图 OCR 后的杂乱文本整理为可导入课表的行文本（竖线分隔，供课表导入页解析）：
     * 课程名|周几|HH:mm-HH:mm|地点|教师
     */
    suspend fun structureTimetableFromOcr(ocrText: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = ocrText.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException(context.getString(R.string.timetable_import_ai_empty_ocr))
            )
        }
        val settings = settingsStore.currentSettings()
        if (!settings.aiParseEnabled) {
            return@withContext Result.failure(
                IllegalStateException(context.getString(R.string.timetable_import_ai_need_enable))
            )
        }
        val apiKey = settings.aiApiKey.ifBlank { fallbackApiKey }
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(context.getString(R.string.ai_missing_api_key))
            )
        }
        val system = (
            "你是课表 OCR 文本结构化助手。用户粘贴的是中文课表截图识别结果，可能错乱、缺行。" +
                "请只输出多行纯文本，每行一门课，严格 5 列用英文竖线 | 分隔，不要表头、不要序号、不要 Markdown、不要解释。\n" +
                "列顺序：课程名|周几|开始时间-结束时间|上课地点|教师姓名\n" +
                "周几必须是：周一、周二、周三、周四、周五、周六、周日 之一。\n" +
                "时间用 24 小时制 HH:mm-HH:mm。地点或教师未知时用单个减号 - 占位。\n" +
                "同一课程多个时间段请拆成多行。"
            )
        val user = "以下为 OCR 文本：\n\n${trimmed.take(12000)}"
        val bodyJson = JSONObject().apply {
            put("model", settings.normalizedAiModel)
            put(
                "messages",
                org.json.JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", system) })
                    put(JSONObject().apply { put("role", "user"); put("content", user) })
                }
            )
            put("temperature", 0.1)
        }
        val request = Request.Builder()
            .url("${settings.normalizedAiBaseUrl}/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMedia))
            .build()
        runCatching {
            client.newBuilder()
                .callTimeout(settings.normalizedTimeoutSeconds.toLong(), TimeUnit.SECONDS)
                .build()
                .newCall(request)
                .execute()
                .use { resp ->
                    val str = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        error("HTTP ${resp.code}: ${str.take(200)}")
                    }
                    val root = JSONObject(str)
                    val content = root.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                    stripCodeFences(content)
                }
        }
    }

    private fun stripCodeFences(s: String): String {
        var t = s.trim()
        if (t.startsWith("```")) {
            t = t.removePrefix("```").trim()
            val firstNl = t.indexOf('\n')
            if (firstNl >= 0 && t.substring(0, firstNl).matches(Regex("[a-zA-Z0-9+]+"))) {
                t = t.substring(firstNl + 1).trim()
            }
            val endFence = t.lastIndexOf("```")
            if (endFence >= 0) t = t.substring(0, endFence).trim()
        }
        return t
    }

    private fun extractJsonObject(s: String): String {
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start >= 0 && end > start) return s.substring(start, end + 1)
        return s
    }

    private fun parseIsoToEndOfDayEpoch(iso: String): Long? = runCatching {
        val d = LocalDate.parse(iso.trim().take(10))
        val z = ZoneId.systemDefault()
        d.atTime(23, 59, 59).atZone(z).toInstant().toEpochMilli()
    }.getOrNull()

    private fun parseTaskType(s: String): TaskType? =
        runCatching { TaskType.valueOf(s.trim()) }.getOrNull()

    private fun parseUrgency(s: String): UrgencyLevel? =
        runCatching { UrgencyLevel.valueOf(s.trim()) }.getOrNull()

    private fun parseDateTime(dateIso: String?, hhmm: String): Long? = runCatching {
        if (dateIso.isNullOrBlank() || hhmm.isBlank()) return null
        val date = LocalDate.parse(dateIso.trim().take(10))
        val parts = hhmm.trim().replace('：', ':').split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toInt().coerceIn(0, 23)
        val minute = parts[1].toInt().coerceIn(0, 59)
        date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
}
