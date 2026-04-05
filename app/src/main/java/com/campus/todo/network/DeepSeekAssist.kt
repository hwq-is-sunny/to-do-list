package com.campus.todo.network

import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
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
    val courseHint: String?,
    val dueAtEpoch: Long?,
    val taskType: TaskType?,
    val urgency: UrgencyLevel?
)

class DeepSeekAssist(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun parseNotification(raw: String): Result<AiParseSuggestion> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("未配置 DeepSeek 密钥：在项目根目录 local.properties 中添加 DEEPSEEK_API_KEY=… 后重新编译。"))
        }
        val system = "你是校园通知解析助手。只输出一个 JSON 对象，不要代码块或多余文字。键：title(string), courseHint(string 或 null), dueDateISO(string yyyy-MM-dd 或 null), taskType(string: HOMEWORK,EXAM,SIGN_IN,CLASS,ANNOUNCEMENT,PERSONAL,OTHER), urgency(string: NORMAL,IMPORTANT,URGENT)。"
        val user = "从以下文本提取信息：\n\n${raw.take(8000)}"
        val bodyJson = JSONObject().apply {
            put("model", "deepseek-chat")
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
            .url("https://api.deepseek.com/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMedia))
            .build()
        runCatching {
            client.newCall(request).execute().use { resp ->
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
                val taskType = o.optString("taskType", "OTHER").let { parseTaskType(it) }
                val urgency = o.optString("urgency", "NORMAL").let { parseUrgency(it) }
                AiParseSuggestion(title, courseHint, dueEpoch, taskType, urgency)
            }
        }
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
}
