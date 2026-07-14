package com.journalog.app.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journalog.app.BuildConfig
import com.journalog.app.core.common.TokenManager
import com.journalog.app.core.common.UpdateChecker
import com.journalog.app.core.common.UpdateResult
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit = {},
    onWallet: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val updateChecker = remember { UpdateChecker(context) }

    var updateResult by remember { mutableStateOf<UpdateResult>(UpdateResult.UpToDate) }
    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadId by remember { mutableStateOf(-1L) }
    val isAdmin by tokenManager.isAdminFlow.collectAsState(initial = false)
    var showDebug by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tokenManager.showDebugFlow.collect { showDebug = it }
    }

    val installPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    // Register download completion receiver
    var receiver by remember { mutableStateOf<android.content.BroadcastReceiver?>(null) }
    DisposableEffect(updateChecker) {
        try {
            val r = updateChecker.registerDownloadReceiver { id ->
                isDownloading = false
                downloadId = id
                try { updateChecker.installApk(id) } catch (_: Exception) {}
            }
            receiver = r
            onDispose {
                try { context.unregisterReceiver(r) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            onDispose {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SettingsItem(
                icon = Icons.Outlined.Person,
                title = "Edit Profile",
                onClick = onEditProfile
            )
            SettingsItem(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                onClick = { }
            )
            SettingsItem(
                icon = Icons.Outlined.Lock,
                title = "Privacy & Security",
                onClick = { }
            )
            SettingsItem(
                icon = Icons.Outlined.Payments,
                title = "Wallet",
                onClick = onWallet
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // App Update section
            Text(
                "App Updates",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                onClick = { }
            )

            when (val result = updateResult) {
                is UpdateResult.Checking -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking for updates...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is UpdateResult.Available -> {
                    SettingsItem(
                        icon = Icons.Outlined.SystemUpdate,
                        title = "Update available: ${result.version.versionName}",
                        onClick = { }
                    )
                    if (result.version.changelog != null) {
                        Text(
                            result.version.changelog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                    if (isDownloading) {
                        LinearProgressIndicator(modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp))
                    } else {
                        Button(
                            onClick = {
                                // Request install unknown apps permission for Android 8+
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    if (!context.packageManager.canRequestPackageInstalls()) {
                                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                        installPermLauncher.launch(intent)
                                        return@Button
                                    }
                                }
                                isDownloading = true
                                downloadId = updateChecker.download(result.version)
                                Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Update")
                        }
                    }
                }
                is UpdateResult.UpToDate -> {
                    SettingsItem(
                        icon = Icons.Outlined.CheckCircle,
                        title = "App is up to date",
                        onClick = { }
                    )
                }
                is UpdateResult.Error -> {
                    SettingsItem(
                        icon = Icons.Outlined.ErrorOutline,
                        title = result.message,
                        onClick = { }
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isChecking = true
                        updateResult = UpdateResult.Checking
                        updateResult = updateChecker.check()
                        isChecking = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                enabled = !isChecking
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Check for Updates")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { tokenManager.setShowDebug(!showDebug) } }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Debug Overlay",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = showDebug,
                        onCheckedChange = { scope.launch { tokenManager.setShowDebug(it) } }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            SettingsItem(
                icon = Icons.Outlined.Logout,
                title = "Log Out",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = {
                    scope.launch {
                        try { ApiClient.create(ApiService::class.java).logout() } catch (_: Exception) {}
                        onLogout()
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = titleColor)
    }
}
