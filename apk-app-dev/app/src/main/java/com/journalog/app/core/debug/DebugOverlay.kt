package com.journalog.app.core.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DebugOverlay(isAdmin: Boolean) {
    if (!isAdmin) return

    var showPanel by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Floating bug button — draggable
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = (-16).dp)
                .size(40.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .clickable { showPanel = !showPanel }
                .background(
                    color = if (DebugLogStore.entries.any { it.responseCode >= 400 || it.error.isNotBlank() })
                        Color(0xFFE53935) else Color(0xFF1E88E5),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.BugReport,
                contentDescription = "Debug",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Panel
        AnimatedVisibility(
            visible = showPanel,
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xE6000000)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🐞 Debug Log (${DebugLogStore.entries.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = {
                                try {
                                    val appContext = context.applicationContext
                                    val text = DebugLogStore.copyAll()
                                    val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Debug Log", text))
                                    Toast.makeText(appContext, "Copied ${text.length} chars", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {}
                            }) {
                                Icon(Icons.Outlined.ContentCopy, "Copy", tint = Color.White)
                            }
                            IconButton(onClick = { DebugLogStore.clear() }) {
                                Icon(Icons.Filled.Delete, "Clear", tint = Color.White)
                            }
                            IconButton(onClick = { showPanel = false }) {
                                Icon(Icons.Filled.Close, "Close", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF444444))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Log list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(DebugLogStore.entries, key = { it.hashCode() }) { entry ->
                            LogEntryCard(entry, context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry, context: Context) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when {
        entry.error.isNotBlank() -> Color(0xFFE53935)
        entry.responseCode in 200..299 -> Color(0xFF43A047)
        entry.responseCode in 400..499 -> Color(0xFFFB8C00)
        entry.responseCode >= 500 -> Color(0xFFE53935)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.timestamp,
                    color = Color(0xFF888888),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    entry.method,
                    color = Color(0xFF64B5F6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    entry.url.substringAfterLast("/api/v1/").take(50),
                    color = Color(0xFFCCCCCC),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (entry.error.isNotBlank()) "ERR" else entry.responseCode.toString(),
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "URL: ${entry.url}",
                    color = Color(0xFFBBBBBB),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (entry.requestBody.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Req: ${entry.requestBody.take(500)}",
                        color = Color(0xFF66BB6A),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (entry.responseBody.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Res: ${entry.responseBody.take(500)}",
                        color = Color(0xFF42A5F5),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (entry.error.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Err: ${entry.error}",
                        color = Color(0xFFE53935),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = {
                        try {
                            val appContext = context.applicationContext
                            val text = "[${entry.timestamp}] ${entry.method} ${entry.url}\n" +
                                "Status: ${entry.responseCode}\n" +
                                "Req: ${entry.requestBody}\n" +
                                "Res: ${entry.responseBody}\n" +
                                "Err: ${entry.error}"
                            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Log Entry", text))
                            Toast.makeText(appContext, "Copied entry", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {}
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, "Copy entry", tint = Color(0xFF888888), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", color = Color(0xFF888888), fontSize = 10.sp)
                }
            }
        }
    }
}
