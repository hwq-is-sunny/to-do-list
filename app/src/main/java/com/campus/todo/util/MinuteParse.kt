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

    fun formatMinuteOfDay(min: Int): String {
        val h = min / 60
        val m = min % 60
        return "%02d:%02d".format(h, m)
    }
}
