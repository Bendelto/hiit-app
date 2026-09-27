package com.example.hiit.alarm

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibraciones de aviso del modo HIIT: cada fase tiene un patrón propio que
 * imita su tono en [SoundPlayer] para reconocer el cambio con el teléfono en
 * el bolsillo, donde ni la pantalla ni los pitidos se perciben bien. Usa el
 * motor de vibración del sistema (VIBRATE ya está declarado en el manifiesto).
 */
object VibrationCues {

    /** Cambio de fase: patrón distintivo según la fase que comienza. */
    fun phaseChange(context: Context, phase: HiitPhase) {
        val pattern = when (phase) {
            // Tres pulsos cortos: prepárate
            HiitPhase.PREP -> longArrayOf(0, 100, 150, 100, 150, 250)
            // Un pulso medio: empieza a caminar
            HiitPhase.WALK -> longArrayOf(0, 400)
            // Dos pulsos: sube a trote
            HiitPhase.JOG -> longArrayOf(0, 200, 150, 250)
            // Dos pulsos rápidos: ¡a correr!
            HiitPhase.RUN -> longArrayOf(0, 150, 120, 250)
            // Dos pulsos suaves: reduce el ritmo
            HiitPhase.COOLDOWN -> longArrayOf(0, 350, 200, 350)
        }
        vibrate(context, pattern)
    }

    /** Cuenta regresiva 3-2-1: un pulso corto por segundo. */
    fun countdown(context: Context) {
        vibrate(context, longArrayOf(0, 120, 880, 120, 880, 120))
    }

    /** Sesión terminada: patrón largo de celebración. */
    fun finish(context: Context) {
        vibrate(context, longArrayOf(0, 250, 200, 200, 200, 450))
    }

    /** Lanza un patrón una sola vez; no hace nada si el equipo no vibra. */
    private fun vibrate(context: Context, pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}
