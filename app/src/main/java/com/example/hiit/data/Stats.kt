package com.example.hiit.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun todayKey(): String = dayKey(0)

fun dayKey(daysAgo: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
}

/**
 * Distancia legible a partir de metros: "850 m" por debajo del kilómetro y
 * "1,25 km" (según el idioma del dispositivo) a partir de ahí.
 */
fun formatDistance(meters: Int): String =
    if (meters >= 1_000) String.format(Locale.getDefault(), "%.2f km", meters / 1_000f)
    else "$meters m"

/**
 * Días consecutivos con actividad (sesión HIIT), contando hacia atrás
 * desde hoy; si hoy aún no hay actividad, la racha empieza desde ayer.
 */
fun AppSettings.streakDays(): Int {
    var daysAgo = if (todayKey() in activeDays) 0 else 1
    var streak = 0
    while (dayKey(daysAgo) in activeDays) {
        streak++
        daysAgo++
    }
    return streak
}
