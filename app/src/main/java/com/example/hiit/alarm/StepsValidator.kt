package com.example.hiit.alarm

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Utilidades de acceso al sensor de pasos para la sesión HIIT: consultar la
 * línea de base al arrancar y leer el contador en vivo durante la sesión.
 *
 * Usa TYPE_STEP_COUNTER (contador acumulado desde el arranque), así que basta
 * leer el valor dos veces: no hay que mantener un listener activo entre medias.
 */
object StepsValidator {

    private const val SENSOR_TIMEOUT_MS = 3_000L

    fun hasSensor(context: Context): Boolean =
        sensorManager(context).getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    /** En Android 10+ hace falta el permiso de reconocimiento de actividad. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_GRANTED

    private fun sensorManager(context: Context): SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /**
     * Lee el contador una vez: registra el listener, espera el primer evento
     * y se desregistra. Público para que la sesión HIIT lo consulte en vivo.
     */
    suspend fun readCounterOnce(context: Context): Float? =
        withContext(Dispatchers.Default) {
            val manager = sensorManager(context)
            val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                ?: return@withContext null

            withTimeoutOrNull(SENSOR_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            manager.unregisterListener(this)
                            if (cont.isActive) cont.resume(event.values[0])
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                    }
                    cont.invokeOnCancellation { manager.unregisterListener(listener) }
                    manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
        }
}
