package com.journalog.app.feature.story

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.journalog.app.core.common.TokenManager
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCreateScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit,
    onStoryCreated: () -> Unit
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Story") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("✕")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Media picker area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { picker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri != null) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap to add photos or videos",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                placeholder = { Text("Write a caption...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            error?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedUri == null) {
                        error = "Please select a photo or video"
                        return@Button
                    }
                    scope.launch {
                        isUploading = true
                        error = null
                        try {
                            val uri = selectedUri!!
                            val contentResolver = context.contentResolver
                            var mimeType = contentResolver.getType(uri) ?: "image/*"
                            val cursor = contentResolver.query(uri, null, null, null, null)
                            var fileName = "upload"
                            cursor?.use {
                                if (it.moveToFirst()) {
                                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (nameIdx >= 0) fileName = it.getString(nameIdx)
                                }
                            }

                            val inputStream = contentResolver.openInputStream(uri)
                            val bytes = inputStream?.readBytes() ?: throw Exception("Could not read file")
                            inputStream?.close()

                            if (bytes.size > 20 * 1024 * 1024) {
                                error = "File too large (max 20MB)"
                                isUploading = false
                                return@launch
                            }

                            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                            val fileExt = fileName.substringAfterLast('.', "").ifBlank {
                                when {
                                    mimeType.startsWith("image/jpeg") -> "jpg"
                                    mimeType.startsWith("image/png") -> "png"
                                    mimeType.startsWith("image/gif") -> "gif"
                                    mimeType.startsWith("video/mp4") -> "mp4"
                                    mimeType.startsWith("video/quicktime") -> "mov"
                                    else -> "jpg"
                                }
                            }
                            val safeFileName = if (fileName.contains('.')) fileName else "upload.$fileExt"
                            val part = MultipartBody.Part.createFormData("file", safeFileName, requestBody)

                            val uploadResp = api.uploadAttachment("story", part)
                            if (!uploadResp.isSuccessful) {
                                val errBody = uploadResp.errorBody()?.string()
                                val parsed = try {
                                    org.json.JSONObject(errBody).optString("message", "Upload failed")
                                } catch (_: Exception) { errBody ?: "Upload failed" }
                                error = parsed
                                isUploading = false
                                return@launch
                            }

                            val attachmentId = uploadResp.body()?.data?.attachmentID ?: throw Exception("No attachment ID")

                            val storyResp = api.createStory(mapOf(
                                "attachment_id" to attachmentId,
                                "text" to caption
                            ))

                            if (storyResp.isSuccessful) {
                                onStoryCreated()
                            } else {
                                error = "Failed to create story"
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Error"
                        }
                        isUploading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isUploading && selectedUri != null,
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Share to Story")
                }
            }
        }
    }
}
