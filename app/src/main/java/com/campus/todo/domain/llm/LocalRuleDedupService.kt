package com.campus.todo.domain.llm

import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.data.db.entity.Task
import com.campus.todo.domain.ParsedCandidateDraft
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class LocalRuleDedupService : LlmDedupService {
    override suspend fun evaluate(
        draft: ParsedCandidateDraft,
        existingTasks: List<Task>,
        existingCandidates: List<CandidateItem>
    ): DedupDecision {
        var bestScore = 0f
        var bestTaskId: Long? = null
        var bestCandidateId: Long? = null

        existingTasks.forEach { task ->
            val score = scoreCandidate(draft.title, draft.courseHint, draft.dueAtEpoch, task.title, task.courseName, task.dueAtEpoch)
            if (score > bestScore) {
                bestScore = score
                bestTaskId = task.id
                bestCandidateId = null
            }
        }
        existingCandidates.forEach { candidate ->
            val score = scoreCandidate(
                draft.title,
                draft.courseHint,
                draft.dueAtEpoch,
                candidate.parsedTitle ?: candidate.rawText.take(48),
                candidate.parsedCourseHint,
                candidate.parsedDueAtEpoch
            )
            if (score > bestScore) {
                bestScore = score
                bestTaskId = null
                bestCandidateId = candidate.id
            }
        }

        val suggestion = when {
            bestScore >= 0.86f -> "high-duplicate"
            bestScore >= 0.64f -> "possible-merge"
            else -> "new-item"
        }
        return DedupDecision(
            duplicateScore = bestScore.coerceIn(0f, 1f),
            mergeSuggestion = suggestion,
            existingTaskId = bestTaskId,
            existingCandidateId = bestCandidateId
        )
    }

    private fun scoreCandidate(
        newTitle: String,
        newCourse: String?,
        newDue: Long?,
        oldTitle: String,
        oldCourse: String?,
        oldDue: Long?
    ): Float {
        val titleScore = tokenSimilarity(newTitle, oldTitle)
        val courseScore = when {
            newCourse.isNullOrBlank() || oldCourse.isNullOrBlank() -> 0.4f
            newCourse.contains(oldCourse, true) || oldCourse.contains(newCourse, true) -> 1f
            else -> tokenSimilarity(newCourse, oldCourse)
        }
        val dueScore = when {
            newDue == null || oldDue == null -> 0.35f
            else -> {
                val diff = abs(newDue - oldDue)
                when {
                    diff <= 60 * 60 * 1000L -> 1f
                    diff <= 24 * 60 * 60 * 1000L -> 0.7f
                    diff <= 3 * 24 * 60 * 60 * 1000L -> 0.45f
                    else -> 0.12f
                }
            }
        }
        return (titleScore * 0.58f) + (courseScore * 0.22f) + (dueScore * 0.2f)
    }

    private fun tokenSimilarity(a: String, b: String): Float {
        val left = normalize(a).split(' ').filter { it.isNotBlank() }.toSet()
        val right = normalize(b).split(' ').filter { it.isNotBlank() }.toSet()
        if (left.isEmpty() || right.isEmpty()) return 0f
        val inter = left.intersect(right).size.toFloat()
        val denom = max(left.size, right.size).toFloat()
        val overlap = inter / denom
        val edit = 1f - (levenshtein(normalize(a), normalize(b)).toFloat() / max(1, max(a.length, b.length)))
        return ((overlap * 0.7f) + (edit * 0.3f)).coerceIn(0f, 1f)
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val m = a.length
        val n = b.length
        val dp = IntArray(n + 1) { it }
        for (i in 1..m) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..n) {
                val tmp = dp[j]
                dp[j] = min(
                    min(dp[j] + 1, dp[j - 1] + 1),
                    prev + if (a[i - 1] == b[j - 1]) 0 else 1
                )
                prev = tmp
            }
        }
        return dp[n]
    }
}
