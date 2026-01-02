package net.turtton.ytalarm.ui.compose.screens

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.compose.theme.AppTheme

/**
 * 権限の種類
 */
enum class PermissionType {
    SCHEDULE_EXACT_ALARM,
    FULL_SCREEN_INTENT,
    OVERLAY,
    NOTIFICATIONS
}

/**
 * 権限の状態
 */
data class PermissionStatus(
    val type: PermissionType,
    val granted: Boolean,
    val title: String,
    val description: String
)

/**
 * 権限チェック画面
 *
 * アプリ起動時に必要な権限を確認し、不足している場合は設定画面への誘導を行う。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 権限状態を管理
    var permissions by remember { mutableStateOf(checkAllPermissions(context)) }

    // ライフサイクルイベントで権限状態を更新（設定画面から戻った時）
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = checkAllPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 通知権限用のランチャー
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissions = checkAllPermissions(context)
    }

    // 設定画面用のランチャー
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissions = checkAllPermissions(context)
    }

    val allGranted = permissions.all { it.granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permission_screen_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.permission_screen_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(permissions) { permission ->
                    PermissionCard(
                        permission = permission,
                        onRequestPermission = {
                            when (permission.type) {
                                PermissionType.NOTIFICATIONS -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                }

                                PermissionType.SCHEDULE_EXACT_ALARM -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val intent = Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            "package:${context.packageName}".toUri()
                                        )
                                        settingsLauncher.launch(intent)
                                    }
                                }

                                PermissionType.FULL_SCREEN_INTENT -> {
                                    if (Build.VERSION.SDK_INT >=
                                        Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                                    ) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                            "package:${context.packageName}".toUri()
                                        )
                                        settingsLauncher.launch(intent)
                                    }
                                }

                                PermissionType.OVERLAY -> {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        "package:${context.packageName}".toUri()
                                    )
                                    settingsLauncher.launch(intent)
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = allGranted
            ) {
                Text(stringResource(R.string.permission_screen_continue))
            }

            if (!allGranted) {
                Text(
                    text = stringResource(R.string.permission_screen_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * 権限カード
 */
@Composable
private fun PermissionCard(
    permission: PermissionStatus,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (permission.granted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (permission.granted) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Close
                        },
                        contentDescription = null,
                        tint = if (permission.granted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = permission.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (!permission.granted) {
                OutlinedButton(onClick = onRequestPermission) {
                    Text(stringResource(R.string.permission_screen_grant))
                }
            }
        }
    }
}

/**
 * 全ての権限をチェック
 */
private fun checkAllPermissions(context: Context): List<PermissionStatus> {
    val permissions = mutableListOf<PermissionStatus>()

    // SCHEDULE_EXACT_ALARM (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        permissions.add(
            PermissionStatus(
                type = PermissionType.SCHEDULE_EXACT_ALARM,
                granted = alarmManager.canScheduleExactAlarms(),
                title = context.getString(R.string.permission_exact_alarm_title),
                description = context.getString(R.string.permission_exact_alarm_description)
            )
        )
    }

    // USE_FULL_SCREEN_INTENT (Android 14+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        permissions.add(
            PermissionStatus(
                type = PermissionType.FULL_SCREEN_INTENT,
                granted = notificationManager.canUseFullScreenIntent(),
                title = context.getString(R.string.permission_full_screen_title),
                description = context.getString(R.string.permission_full_screen_description)
            )
        )
    }

    // SYSTEM_ALERT_WINDOW
    permissions.add(
        PermissionStatus(
            type = PermissionType.OVERLAY,
            granted = Settings.canDrawOverlays(context),
            title = context.getString(R.string.permission_overlay_title),
            description = context.getString(R.string.permission_overlay_description)
        )
    )

    // POST_NOTIFICATIONS (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(
            PermissionStatus(
                type = PermissionType.NOTIFICATIONS,
                granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED,
                title = context.getString(R.string.permission_notification_title),
                description = context.getString(R.string.permission_notification_description)
            )
        )
    }

    return permissions
}

/**
 * 権限が不足しているかチェック
 */
fun hasMissingPermissions(context: Context): Boolean =
    checkAllPermissions(context).any { !it.granted }

@Preview(showBackground = true)
@Composable
private fun PermissionScreenPreview() {
    AppTheme {
        PermissionScreen(onContinue = {})
    }
}