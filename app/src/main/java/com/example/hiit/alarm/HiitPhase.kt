package com.example.hiit.alarm

import android.content.Context
import com.example.hiit.R

/** Fase del entrenamiento HIIT: preparación, caminata, carrera o enfriamiento. */
enum class HiitPhase { PREP, WALK, RUN, COOLDOWN }

/** "2 min 30 s" para mostrar en pantalla. */
fun formatDuration(context: Context, seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return when {
        m > 0 && s > 0 -> context.getString(R.string.common_duration_min_sec, m, s)
        m > 0 -> context.getString(R.string.common_duration_min, m)
        else -> context.getString(R.string.common_duration_sec, s)
    }
}

/** "2 minutos con 30 segundos" para que suene natural al hablar. */
fun speakDuration(context: Context, seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    val parts = mutableListOf<String>()
    if (m > 0) {
        parts += if (m == 1) {
            context.getString(R.string.tts_duration_minute_one)
        } else {
            context.getString(R.string.tts_duration_minutes, m)
        }
    }
    if (s > 0) {
        parts += if (s == 1) {
            context.getString(R.string.tts_duration_second_one)
        } else {
            context.getString(R.string.tts_duration_seconds, s)
        }
    }
    return if (parts.isEmpty()) {
        context.getString(R.string.tts_duration_zero)
    } else {
        parts.joinToString(context.getString(R.string.tts_duration_joiner))
    }
}
