package com.campus.todo.util

object MinuteParse {
    fun parseMinuteOfDay(input: String): Int? {
        val parts = input.trim().split(":", limit = 3)
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    /** Returns a user-visible error message, or null if the range is valid. */
    fun timeRangeValidationError(startInput: String, endInput: String): String? {
        val start = parseMinuteOfDay(startInput)
        val end = parseMinuteOfDay(endInput)
        if (start == null) {
            return "开始时间格式不正确，请使用 24 小时制，例如 08:00（小时 0–23，分钟 0–59）。"
        }
        if (end == null) {
            return "结束时间格式不正确，请使用 24 小时制，例如 09:40。"
        }
        if (end <= start) {
            return "结束时间必须晚于开始时间。"
        }
        return null
    }

    fun formatMinuteOfDay(min: Int): String {
        val h = min / 60
        val m = min % 60
        return "%02d:%02d".format(h, m)
    }
}
