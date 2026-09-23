package com.example.hiit.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hiit.R
import com.example.hiit.data.AppSettings
import com.example.hiit.data.SettingsRepository
import kotlinx.coroutines.launch

/**
 * Pestaña Ajustes: acceso a la configuración de la sesión HIIT y permisos
 * del sistema.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    repository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onHiitConfig: () -> Unit,
) {
    val context = LocalContext.current

    // Se incrementa cada vez que la pantalla vuelve a primer plano (o se concede
    // un permiso por diálogo), para recalcular el estado de los permisos.
    var refreshTrigger by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exactAlarmGranted = remember(refreshTrigger) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.canScheduleExactAlarms()
    }
    val notificationGranted = remember(refreshTrigger) { hasNotificationPermission(context) }
    val fullScreenGranted = remember(refreshTrigger) { hasFullScreenIntentPermission(context) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshTrigger++ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        AppHeader(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.settings_subtitle),
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── HIIT ─────────────────────────────────────────────────
            SectionTitle(stringResource(R.string.hiit_title))
            SettingRow(
                icon = Icons.Default.Speed,
                iconBackground = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.settings_hiit_row),
                onClick = onHiitConfig,
            )

            // ── Permisos ────────────────────────────────────────────
            PremiumCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(stringResource(R.string.settings_permissions))
                    Text(
                        stringResource(R.string.settings_permissions_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    PermissionRow(
                        name = stringResource(R.string.settings_perm_notifications),
                        description = stringResource(R.string.settings_perm_notifications_desc),
                        icon = Icons.Default.Notifications,
                        granted = notificationGranted,
                    ) {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    PermissionRow(
                        name = stringResource(R.string.settings_perm_exact_alarms),
                        description = stringResource(R.string.settings_perm_exact_alarms_desc),
                        icon = Icons.Default.CheckCircle,
                        granted = exactAlarmGranted,
                    ) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    }

                    PermissionRow(
                        name = stringResource(R.string.settings_perm_full_screen),
                        description = stringResource(R.string.settings_perm_full_screen_desc),
                        icon = Icons.Default.Refresh,
                        granted = fullScreenGranted,
                    ) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    }

                    if (notificationGranted && exactAlarmGranted && fullScreenGranted) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp),
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    stringResource(R.string.settings_all_granted),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            // ── Introducción ──────────────────────────────────────────
            PremiumCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(stringResource(R.string.settings_intro_section))
                    Text(
                        stringResource(R.string.settings_intro_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = { scope.launch { repository.setOnboardingSeen(false) } },
                        ) {
                            Text(stringResource(R.string.settings_show_intro))
                        }
                    }
                }
            }

            // Espacio para que el último elemento quede por encima de la barra
            // de navegación flotante al llegar al final del scroll
            Spacer(modifier = Modifier.navigationBarsPadding().height(104.dp))
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

private fun hasFullScreenIntentPermission(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    return if (Build.VERSION.SDK_INT >= 34) nm.canUseFullScreenIntent() else true
}
