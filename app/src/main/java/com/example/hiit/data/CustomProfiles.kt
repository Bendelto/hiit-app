package com.example.hiit.data

import com.example.hiit.alarm.HiitPhase
import com.example.hiit.alarm.asPhase
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** Nivel de intensidad de un intervalo personalizado, de menor a mayor. */
enum class IntervalIntensity { WALK, JOG, RUN }

/** Cómo se ordenan los intervalos de un perfil al ejecutarlo. */
enum class ProfileMode { SEQUENCE, LADDER, RANDOM }

/** Un intervalo personalizado: duración propia y nivel de intensidad propio. */
data class CustomInterval(
    val intensity: IntervalIntensity,
    val seconds: Int,
)

/**
 * Perfil guardado por el usuario: una secuencia de intervalos con duraciones
 * e intensidades propias, más calentamiento, enfriamiento y cuántas veces se
 * repite la secuencia al ejecutarla.
 */
data class IntervalProfile(
    val id: String,
    val name: String,
    val intervals: List<CustomInterval>,
    val mode: ProfileMode,
    val repetitions: Int,
    val warmupSeconds: Int,
    val cooldownSeconds: Int,
) {
    val totalSeconds: Int
        get() = warmupSeconds +
            repetitions * intervals.sumOf { it.seconds } +
            cooldownSeconds

    val totalIntervals: Int get() = intervals.size * repetitions
}

/**
 * Paso plano de la sesión en vivo: qué fase toca (PREP, un nivel de
 * intensidad o COOLDOWN) y por cuánto tiempo. La sesión entera es una lista
 * de pasos que el motor recorre secuencialmente.
 */
data class PlanStep(val phase: HiitPhase, val seconds: Int)

/**
 * Plan completo que se ejecutará: calentamiento, los intervalos ordenados
 * según el modo repetidos [IntervalProfile.repetitions] veces y enfriamiento.
 * Escalera ordena de la intensidad más baja a la más alta (a igual intensidad
 * conserva el orden en que se creó); aleatorio baraja con la semilla dada.
 */
fun IntervalProfile.buildPlan(seed: Long = System.currentTimeMillis()): List<PlanStep> {
    val ordered = when (mode) {
        ProfileMode.SEQUENCE -> intervals
        ProfileMode.LADDER -> intervals.sortedBy { it.intensity.ordinal }
        ProfileMode.RANDOM -> intervals.shuffled(Random(seed))
    }
    return buildList {
        if (warmupSeconds > 0) add(PlanStep(HiitPhase.PREP, warmupSeconds))
        repeat(repetitions) {
            ordered.forEach { add(PlanStep(it.intensity.asPhase(), it.seconds)) }
        }
        if (cooldownSeconds > 0) add(PlanStep(HiitPhase.COOLDOWN, cooldownSeconds))
    }
}

// ── Serialización (JSON en DataStore, mismo patrón que el historial) ────────

private const val KEY_ID = "id"
private const val KEY_NAME = "name"
private const val KEY_MODE = "mode"
private const val KEY_REPS = "reps"
private const val KEY_WARMUP = "warmup"
private const val KEY_COOLDOWN = "cooldown"
private const val KEY_INTERVALS = "intervals"
private const val KEY_INTENSITY = "i"
private const val KEY_SECONDS = "s"

fun serializeProfiles(profiles: List<IntervalProfile>): String {
    val array = JSONArray()
    profiles.forEach { profile ->
        val item = JSONObject()
            .put(KEY_ID, profile.id)
            .put(KEY_NAME, profile.name)
            .put(KEY_MODE, profile.mode.name)
            .put(KEY_REPS, profile.repetitions)
            .put(KEY_WARMUP, profile.warmupSeconds)
            .put(KEY_COOLDOWN, profile.cooldownSeconds)
            .put(
                KEY_INTERVALS,
                JSONArray().apply {
                    profile.intervals.forEach { interval ->
                        put(
                            JSONObject()
                                .put(KEY_INTENSITY, interval.intensity.name)
                                .put(KEY_SECONDS, interval.seconds),
                        )
                    }
                },
            )
        array.put(item)
    }
    return array.toString()
}

fun parseProfiles(raw: String): List<IntervalProfile> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val intervals = mutableListOf<CustomInterval>()
                val intervalsArray = item.optJSONArray(KEY_INTERVALS) ?: JSONArray()
                for (j in 0 until intervalsArray.length()) {
                    val interval = intervalsArray.getJSONObject(j)
                    // Perfiles guardados con niveles ya eliminados (p. ej.
                    // SPRINT) se reinterpretan como carrera, no se pierden.
                    val intensity = runCatching {
                        IntervalIntensity.valueOf(interval.getString(KEY_INTENSITY))
                    }.getOrDefault(IntervalIntensity.RUN)
                    val seconds = interval.optInt(KEY_SECONDS, 0)
                    if (seconds > 0) intervals += CustomInterval(intensity, seconds)
                }
                if (intervals.isEmpty()) continue
                add(
                    IntervalProfile(
                        id = item.optString(KEY_ID),
                        name = item.optString(KEY_NAME),
                        intervals = intervals,
                        mode = runCatching {
                            ProfileMode.valueOf(item.getString(KEY_MODE))
                        }.getOrDefault(ProfileMode.SEQUENCE),
                        repetitions = item.optInt(KEY_REPS, 1).coerceIn(1, 50),
                        warmupSeconds = item.optInt(KEY_WARMUP, 0).coerceIn(0, 3_600),
                        cooldownSeconds = item.optInt(KEY_COOLDOWN, 0).coerceIn(0, 3_600),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())
}

/** Formato compacto para el plan en vivo: "JOG:90;RUN:60;WALK:120". */
fun serializeSteps(steps: List<PlanStep>): String =
    steps.joinToString(";") { "${it.phase.name}:${it.seconds}" }

fun parseSteps(raw: String): List<PlanStep> {
    if (raw.isBlank()) return emptyList()
    return raw.split(";").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size != 2) return@mapNotNull null
        // Planes en vivo guardados con niveles ya eliminados (p. ej. SPRINT)
        // se reinterpretan como carrera para no truncar la sesión.
        val phase = when (parts[0]) {
            "SPRINT" -> HiitPhase.RUN
            else -> runCatching { HiitPhase.valueOf(parts[0]) }.getOrNull()
        } ?: return@mapNotNull null
        val seconds = parts[1].toIntOrNull() ?: return@mapNotNull null
        if (seconds <= 0) return@mapNotNull null
        PlanStep(phase, seconds)
    }
}
