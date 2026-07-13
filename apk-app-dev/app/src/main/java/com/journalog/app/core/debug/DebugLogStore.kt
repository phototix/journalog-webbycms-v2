package com.journalog.app.core.debug

import androidx.compose.runtime.mutableStateListOf
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val timestamp: String,
    val method: String,
    val url: String,
    val requestHeaders: String = "",
    val requestBody: String = "",
    val responseCode: Int = 0,
    val responseBody: String = "",
    val error: String = ""
) {
    val summary: String get() = "$method $url → $responseCode"
}

object DebugLogStore {
    val entries = mutableStateListOf<LogEntry>()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun add(
        method: String,
        url: String,
        requestHeaders: String = "",
        requestBody: String = "",
        responseCode: Int = 0,
        responseBody: String = "",
        error: String = ""
    ) {
        val entry = LogEntry(
            timestamp = timeFormat.format(Date()),
            method = method,
            url = url,
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            responseCode = responseCode,
            responseBody = responseBody,
            error = error
        )
        entries.add(0, entry)
        if (entries.size > 200) {
            entries.removeAt(entries.lastIndex)
        }
    }

    fun clear() {
        entries.clear()
    }

    fun copyAll(): String {
        return entries.joinToString("\n---\n") { e ->
            buildString {
                appendLine("[${e.timestamp}] ${e.method} ${e.url}")
                appendLine("Status: ${e.responseCode}")
                if (e.requestBody.isNotBlank()) {
                    appendLine("Request: ${e.requestBody}")
                }
                if (e.responseBody.isNotBlank()) {
                    appendLine("Response: ${e.responseBody}")
                }
                if (e.error.isNotBlank()) {
                    appendLine("Error: ${e.error}")
                }
            }
        }
    }

    fun requestBodyToString(body: okhttp3.RequestBody?): String {
        if (body == null) return ""
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        } catch (_: Exception) { "" }
    }

    fun responseBodyToString(body: ResponseBody?): String {
        if (body == null) return ""
        return try {
            val source = body.source()
            source.request(Long.MAX_VALUE)
            source.buffer.clone().readUtf8()
        } catch (_: Exception) { "" }
    }
}
