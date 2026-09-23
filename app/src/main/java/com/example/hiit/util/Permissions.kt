package com.example.hiit.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Permisos que la app necesita y utilidades para comprobarlos y pedirlos.
 *
 * Solo [android.Manifest.permission.POST_NOTIFICATIONS] y
 * [android.Manifest.permission.ACTIVITY_RECOGNITION] requieren consentimiento
 * en tiempo de ejecución; el resto (alarmas exactas, arranque, vibración,
 * servicio en primer plano…) se concede al instalar la app.
 */
fun requiredRuntimePermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= 33) add(android.Manifest.permission.POST_NOTIFICATIONS)
    if (Build.VERSION.SDK_INT >= 29) add(android.Manifest.permission.ACTIVITY_RECOGNITION)
}

/** Permisos en tiempo de ejecución que aún no están concedidos. */
fun missingPermissions(context: Context): List<String> =
    requiredRuntimePermissions().filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

/** `true` si el usuario no ha concedido el permiso de alarmas exactas. */
fun isExactAlarmMissing(context: Context): Boolean {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return !am.canScheduleExactAlarms()
}

/**
 * `true` solo si TODOS los permisos están concedidos:
 * - Permisos en tiempo de ejecución (notificaciones, actividad)
 * - Alarmas exactas (permiso especial del sistema)
 */
fun hasAllPermissions(context: Context): Boolean =
    missingPermissions(context).isEmpty() && !isExactAlarmMissing(context)

/** Abre la ficha de la app en los ajustes del sistema para conceder permisos a mano. */
fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** Abre la pantalla de ajustes para conceder el permiso de alarmas exactas. */
fun openExactAlarmSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        },
    )
}

