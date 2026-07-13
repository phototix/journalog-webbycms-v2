package com.journalog.app.core.common

import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {

    private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val isoParserWithMicros = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val mysqlParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.US)
    private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.US)

    fun formatRelativeTime(isoString: String?): String {
        if (isoString == null) return ""
        val date = try {
            isoParserWithMicros.parse(isoString) ?: isoParser.parse(isoString)
        } catch (_: Exception) {
            try { isoParser.parse(isoString) } catch (_: Exception) { return isoString }
        } ?: return isoString
        return formatRelative(date)
    }

    fun formatRelativeTimeMySql(mysqlString: String?): String {
        if (mysqlString == null) return ""
        val date = try { mysqlParser.parse(mysqlString) } catch (_: Exception) { return mysqlString }
        return formatRelative(date)
    }

    fun formatRelativeTime(epochSecs: Long): String {
        return formatRelative(Date(epochSecs * 1000))
    }

    private fun formatRelative(date: Date): String {
        val now = System.currentTimeMillis()
        val diff = now - date.time

        if (diff < 0) return dateTimeFormatter.format(date)

        val calendar = Calendar.getInstance()
        val dateCal = Calendar.getInstance().apply { time = date }

        if (diff < 60_000) return "Just now"
        if (diff < 3_600_000) return "${diff / 60_000} mins ago"
        if (diff < 86_400_000) return "${diff / 3_600_000} hrs ago"

        if (isSameDay(calendar, dateCal)) return "Today at ${timeFormatter.format(date)}"
        if (isYesterday(calendar, dateCal)) return "Yesterday at ${timeFormatter.format(date)}"

        val dayOfYearDiff = calendar.get(Calendar.DAY_OF_YEAR) - dateCal.get(Calendar.DAY_OF_YEAR)
        val yearDiff = calendar.get(Calendar.YEAR) - dateCal.get(Calendar.YEAR)
        if (yearDiff == 0 && dayOfYearDiff < 7) return "This week"
        if (yearDiff == 0 && dayOfYearDiff < 14) return "Last week"

        return dateTimeFormatter.format(date)
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, date: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(yesterday, date)
    }
}
