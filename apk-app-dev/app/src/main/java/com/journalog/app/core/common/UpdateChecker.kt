package com.journalog.app.core.common

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.journalog.app.BuildConfig
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.ApkVersionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class UpdateResult {
    data object Checking : UpdateResult()
    data class Available(val version: ApkVersionDto) : UpdateResult()
    data object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

class UpdateChecker(private val context: Context) {

    private var downloadId: Long = -1L
    private val dm by lazy { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.create(ApiService::class.java)
            val response = api.checkApkVersion()
            if (response.isSuccessful && response.body()?.ok == true) {
                val remote = response.body()!!.data ?: return@withContext UpdateResult.Error("Empty response")
                val localCode = BuildConfig.VERSION_CODE
                if (remote.versionCode > localCode) {
                    UpdateResult.Available(remote)
                } else {
                    UpdateResult.UpToDate
                }
            } else {
                UpdateResult.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Network error")
        }
    }

    fun download(version: ApkVersionDto): Long {
        val fileName = "journalog-${version.versionName}.apk"

        val request = DownloadManager.Request(Uri.parse(version.downloadUrl)).apply {
            setTitle("Journalog Update")
            setDescription("Downloading ${version.versionName}...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
        }

        downloadId = dm.enqueue(request)
        return downloadId
    }

    fun registerDownloadReceiver(onComplete: (Long) -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    onComplete(id)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        return receiver
    }

    fun installApk(downloadId: Long) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor = dm.query(query)
        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            cursor.close()

            if (status == DownloadManager.STATUS_SUCCESSFUL && uriString != null) {
                val file = File(Uri.parse(uriString).path ?: return)
                installApkFile(file)
            }
        }
    }

    private fun installApkFile(file: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No app found to open APK", Toast.LENGTH_SHORT).show()
        }
    }
}
