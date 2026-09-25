package com.example.hiit.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "hiit_settings")

data class AppSettings(
    val hiitWalkSeconds: Int = 120,
    val hiitRunSeconds: Int = 30,
    // Rondas de la sesión; 0 = infinitas (la detiene el usuario al agotarse)
    val hiitRounds: Int = 8,
    // Calentamiento antes de la primera caminata (0 = empezar directo)
    val hiitWarmupSeconds: Int = 0,
    // Enfriamiento tras la última ronda (0 = terminar sin enfriamiento)
    val hiitCooldownSeconds: Int = 120,
    val hiitActive: Boolean = false,
    // Sonidos de cuenta regresiva (pitidos) y guía de voz en HIIT
    val hiitSounds: Boolean = true,
    // Modo interior: suaviza el tono agudo de "correr" en espacios cerrados (gimnasio)
    val hiitIndoorMode: Boolean = false,
    val hiitVoice: Boolean = true,
    // Modo caminadora: el celular queda fijo, así que el sensor no cuenta pasos;
    // la distancia real se anota a mano al terminar la sesión
    val hiitTreadmillMode: Boolean = false,
    // Estado en vivo de la sesión HIIT: fase actual, ronda y fin de la fase (epoch ms).
    // Es la fuente de verdad que sincroniza la pantalla del cronómetro con las alarmas.
    val hiitPhase: String = "",
    val hiitRound: Int = 0,
    val hiitPhaseEnd: Long = 0L,
    // Fase y rondas de la alarma pendiente (lo que sonará al terminar la fase actual)
    val hiitPendingPhase: String = "",
    val hiitPendingRounds: Int = 0,
    // Segundos restantes cuando la sesión está pausada; 0 = no pausada
    val hiitPausedSeconds: Int = 0,
    val hiitTotalSessions: Int = 0,
    // Marca de tiempo de la última sesión HIIT terminada; sirve para mostrar
    // la pantalla de celebración justo al completarla.
    val hiitLastCompleted: Long = 0L,
    // Momento de inicio de la sesión en curso (epoch ms); 0 = sin sesión.
    // Sirve para calcular la duración real, también en rondas infinitas.
    val hiitStartedAt: Long = 0L,
    // Contador del sensor anotado al arrancar la sesión HIIT; los pasos de la
    // sesión = contador actual − esta línea de base. -1 = sensor no disponible
    val hiitStepBaseline: Float = -1f,
    // Resumen de la última sesión para la pantalla de celebración
    val hiitLastRounds: Int = 0,
    val hiitLastSeconds: Int = 0,
    val hiitLastSteps: Int = 0,
    val hiitLastDistanceM: Int = 0,
    // Días con al menos una sesión HIIT (para la racha)
    val activeDays: Set<String> = emptySet(),
    // Sesiones HIIT por día ("2026-09-12=2") para el gráfico semanal
    val hiitSessionsHistory: Map<String, Int> = emptyMap(),
    // Onboarding de primer uso ya mostrado
    val onboardingSeen: Boolean = false,
    // Función Pro desbloqueada (billing real pendiente; hoy es un candado visual)
    val hiitProUnlocked: Boolean = false,
    // Perfiles de intervalos personalizados (JSON, ver CustomProfiles.kt)
    val hiitCustomProfilesJson: String = "",
    // Plan en vivo de una sesión personalizada: pasos expandidos (modo × reps).
    // Vacío = la sesión es la clásica de caminata/carrera fijas.
    val hiitPlanJson: String = "",
    // Índice (0-based) del siguiente paso del plan; el paso en curso es el anterior.
    val hiitPlanIndex: Int = 0,
) {
    // Duración estimada de la sesión. Con rondas infinitas (0) solo cuenta
    // calentamiento y enfriamiento: la sesión dura lo que aguante el usuario.
    val hiitTotalSeconds: Int
        get() = hiitWarmupSeconds + hiitRounds * (hiitWalkSeconds + hiitRunSeconds) + hiitCooldownSeconds

    /** Pasos del plan personalizado en vivo; null cuando la sesión es la clásica. */
    val planSteps: List<PlanStep>?
        get() = parseSteps(hiitPlanJson).takeIf { it.isNotEmpty() }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val HIIT_WALK_SECONDS = intPreferencesKey("hiit_walk_seconds")
        val HIIT_RUN_SECONDS = intPreferencesKey("hiit_run_seconds")
        val HIIT_ROUNDS = intPreferencesKey("hiit_rounds")
        val HIIT_WARMUP_SECONDS = intPreferencesKey("hiit_warmup_seconds")
        val HIIT_COOLDOWN_SECONDS = intPreferencesKey("hiit_cooldown_seconds")
        val HIIT_ACTIVE = booleanPreferencesKey("hiit_active")
        val HIIT_SOUNDS = booleanPreferencesKey("hiit_sounds")
        val HIIT_INDOOR_MODE = booleanPreferencesKey("hiit_indoor_mode")
        val HIIT_VOICE = booleanPreferencesKey("hiit_voice")
        val HIIT_TREADMILL_MODE = booleanPreferencesKey("hiit_treadmill_mode")
        val HIIT_PHASE = stringPreferencesKey("hiit_phase")
        val HIIT_ROUND = intPreferencesKey("hiit_round")
        val HIIT_PHASE_END = longPreferencesKey("hiit_phase_end")
        val HIIT_PENDING_PHASE = stringPreferencesKey("hiit_pending_phase")
        val HIIT_PENDING_ROUNDS = intPreferencesKey("hiit_pending_rounds")
        val HIIT_PAUSED_SECONDS = intPreferencesKey("hiit_paused_seconds")
        val HIIT_TOTAL_SESSIONS = intPreferencesKey("hiit_total_sessions")
        val HIIT_LAST_COMPLETED = longPreferencesKey("hiit_last_completed")
        val HIIT_STARTED_AT = longPreferencesKey("hiit_started_at")
        val HIIT_STEP_BASELINE = floatPreferencesKey("hiit_step_baseline")
        val HIIT_LAST_ROUNDS = intPreferencesKey("hiit_last_rounds")
        val HIIT_LAST_SECONDS = intPreferencesKey("hiit_last_seconds")
        val HIIT_LAST_STEPS = intPreferencesKey("hiit_last_steps")
        val HIIT_LAST_DISTANCE_M = intPreferencesKey("hiit_last_distance_m")
        val ACTIVE_DAYS = stringPreferencesKey("active_days")
        val HIIT_SESSIONS_HISTORY = stringPreferencesKey("hiit_sessions_history")
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        val HIIT_PRO_UNLOCKED = booleanPreferencesKey("hiit_pro_unlocked")
        val HIIT_CUSTOM_PROFILES = stringPreferencesKey("hiit_custom_profiles")
        val HIIT_PLAN_JSON = stringPreferencesKey("hiit_plan_json")
        val HIIT_PLAN_INDEX = intPreferencesKey("hiit_plan_index")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            hiitWalkSeconds = prefs[Keys.HIIT_WALK_SECONDS] ?: 120,
            hiitRunSeconds = prefs[Keys.HIIT_RUN_SECONDS] ?: 30,
            hiitRounds = prefs[Keys.HIIT_ROUNDS] ?: 8,
            hiitWarmupSeconds = prefs[Keys.HIIT_WARMUP_SECONDS] ?: 0,
            hiitCooldownSeconds = prefs[Keys.HIIT_COOLDOWN_SECONDS] ?: 120,
            hiitActive = prefs[Keys.HIIT_ACTIVE] ?: false,
            hiitSounds = prefs[Keys.HIIT_SOUNDS] ?: true,
            hiitIndoorMode = prefs[Keys.HIIT_INDOOR_MODE] ?: false,
            hiitVoice = prefs[Keys.HIIT_VOICE] ?: true,
            hiitTreadmillMode = prefs[Keys.HIIT_TREADMILL_MODE] ?: false,
            hiitPhase = prefs[Keys.HIIT_PHASE] ?: "",
            hiitRound = prefs[Keys.HIIT_ROUND] ?: 0,
            hiitPhaseEnd = prefs[Keys.HIIT_PHASE_END] ?: 0L,
            hiitPendingPhase = prefs[Keys.HIIT_PENDING_PHASE] ?: "",
            hiitPendingRounds = prefs[Keys.HIIT_PENDING_ROUNDS] ?: 0,
            hiitPausedSeconds = prefs[Keys.HIIT_PAUSED_SECONDS] ?: 0,
            hiitTotalSessions = prefs[Keys.HIIT_TOTAL_SESSIONS] ?: 0,
            hiitLastCompleted = prefs[Keys.HIIT_LAST_COMPLETED] ?: 0L,
            hiitStartedAt = prefs[Keys.HIIT_STARTED_AT] ?: 0L,
            hiitStepBaseline = prefs[Keys.HIIT_STEP_BASELINE] ?: -1f,
            hiitLastRounds = prefs[Keys.HIIT_LAST_ROUNDS] ?: 0,
            hiitLastSeconds = prefs[Keys.HIIT_LAST_SECONDS] ?: 0,
            hiitLastSteps = prefs[Keys.HIIT_LAST_STEPS] ?: 0,
            hiitLastDistanceM = prefs[Keys.HIIT_LAST_DISTANCE_M] ?: 0,
            activeDays = (prefs[Keys.ACTIVE_DAYS] ?: "")
                .split(";").filter { it.isNotBlank() }.toSet(),
            hiitSessionsHistory = parseSessionsHistory(prefs[Keys.HIIT_SESSIONS_HISTORY] ?: ""),
            onboardingSeen = prefs[Keys.ONBOARDING_SEEN] ?: false,
            hiitProUnlocked = prefs[Keys.HIIT_PRO_UNLOCKED] ?: false,
            hiitCustomProfilesJson = prefs[Keys.HIIT_CUSTOM_PROFILES] ?: "",
            hiitPlanJson = prefs[Keys.HIIT_PLAN_JSON] ?: "",
            hiitPlanIndex = prefs[Keys.HIIT_PLAN_INDEX] ?: 0,
        )
    }

    suspend fun setHiitParams(
        walkSeconds: Int,
        runSeconds: Int,
        rounds: Int,
        warmupSeconds: Int,
        cooldownSeconds: Int,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_WALK_SECONDS] = walkSeconds.coerceIn(10, 3_600)
            prefs[Keys.HIIT_RUN_SECONDS] = runSeconds.coerceIn(5, 3_600)
            prefs[Keys.HIIT_ROUNDS] = rounds.coerceIn(0, 50)
            prefs[Keys.HIIT_WARMUP_SECONDS] = warmupSeconds.coerceIn(0, 3_600)
            prefs[Keys.HIIT_COOLDOWN_SECONDS] = cooldownSeconds.coerceIn(0, 3_600)
        }
    }

    suspend fun setHiitActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_ACTIVE] = active
        }
    }

    suspend fun setHiitSounds(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_SOUNDS] = enabled
        }
    }

    suspend fun setHiitIndoorMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_INDOOR_MODE] = enabled
        }
    }

    suspend fun setHiitVoice(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_VOICE] = enabled
        }
    }

    suspend fun setHiitTreadmillMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_TREADMILL_MODE] = enabled
        }
    }

    /** Corrige pasos y distancia de la última sesión (distancia anotada a mano en caminadora). */
    suspend fun updateLastSessionStats(steps: Int, distanceMeters: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_LAST_STEPS] = steps
            prefs[Keys.HIIT_LAST_DISTANCE_M] = distanceMeters
        }
    }

    suspend fun setHiitState(phase: String, round: Int, phaseEnd: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_PHASE] = phase
            prefs[Keys.HIIT_ROUND] = round
            prefs[Keys.HIIT_PHASE_END] = phaseEnd
        }
    }

    suspend fun clearHiitState() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.HIIT_PHASE)
            prefs.remove(Keys.HIIT_ROUND)
            prefs.remove(Keys.HIIT_PHASE_END)
            prefs.remove(Keys.HIIT_PENDING_PHASE)
            prefs.remove(Keys.HIIT_PENDING_ROUNDS)
            prefs.remove(Keys.HIIT_PAUSED_SECONDS)
            prefs.remove(Keys.HIIT_STARTED_AT)
            prefs.remove(Keys.HIIT_STEP_BASELINE)
            prefs.remove(Keys.HIIT_PLAN_JSON)
            prefs.remove(Keys.HIIT_PLAN_INDEX)
        }
    }

    /** Anota el momento de inicio y el contador de pasos al arrancar la sesión. */
    suspend fun setHiitSessionStart(startedAt: Long, stepBaseline: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_STARTED_AT] = startedAt
            prefs[Keys.HIIT_STEP_BASELINE] = stepBaseline
        }
    }

    /** Guarda qué fase sonará al terminar la actual y con cuántas rondas restan. */
    suspend fun setHiitPending(phase: String, roundsRemaining: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_PENDING_PHASE] = phase
            prefs[Keys.HIIT_PENDING_ROUNDS] = roundsRemaining
        }
    }

    /** Pausa la sesión guardando los segundos que quedaban en la fase actual. */
    suspend fun setHiitPaused(secondsRemaining: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_PAUSED_SECONDS] = secondsRemaining
        }
    }

    suspend fun clearHiitLastCompleted() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.HIIT_LAST_COMPLETED)
        }
    }

    /**
     * Registra una sesión HIIT terminada (completada o detenida a mano) y
     * marca el día como activo. Guarda el resumen (rondas alcanzadas,
     * duración real, pasos y distancia) para la pantalla de celebración.
     */
    suspend fun recordHiitCompleted(
        rounds: Int,
        seconds: Int,
        steps: Int,
        distanceMeters: Int,
    ) {
        val today = todayKey()
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_TOTAL_SESSIONS] = (prefs[Keys.HIIT_TOTAL_SESSIONS] ?: 0) + 1
            prefs[Keys.HIIT_LAST_COMPLETED] = System.currentTimeMillis()
            prefs[Keys.HIIT_LAST_ROUNDS] = rounds
            prefs[Keys.HIIT_LAST_SECONDS] = seconds
            prefs[Keys.HIIT_LAST_STEPS] = steps
            prefs[Keys.HIIT_LAST_DISTANCE_M] = distanceMeters
            val history = parseSessionsHistory(prefs[Keys.HIIT_SESSIONS_HISTORY] ?: "")
                .toMutableMap()
            history[today] = (history[today] ?: 0) + 1
            prefs[Keys.HIIT_SESSIONS_HISTORY] = serializeSessionsHistory(history)
            addActiveDay(prefs, today)
        }
    }

    /** Marca el onboarding de primer uso como visto (o lo resetea para reverlo). */
    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.ONBOARDING_SEEN] = seen }
    }

    suspend fun setProUnlocked(unlocked: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.HIIT_PRO_UNLOCKED] = unlocked }
    }

    /** Guarda el listado completo de perfiles de intervalos personalizados. */
    suspend fun saveCustomProfiles(profiles: List<IntervalProfile>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_CUSTOM_PROFILES] = serializeProfiles(profiles)
        }
    }

    /** Instala el plan de una sesión personalizada y rebobina el índice a 0. */
    suspend fun setSessionPlan(steps: List<PlanStep>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_PLAN_JSON] = serializeSteps(steps)
            prefs[Keys.HIIT_PLAN_INDEX] = 0
        }
    }

    /** Avanza el puntero del plan personalizado (siguiente paso a ejecutar). */
    suspend fun setPlanIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIIT_PLAN_INDEX] = index
        }
    }

    private fun addActiveDay(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        day: String,
    ) {
        val days = (prefs[Keys.ACTIVE_DAYS] ?: "")
            .split(";").filter { it.isNotBlank() }.toMutableSet()
        days.add(day)
        prefs[Keys.ACTIVE_DAYS] = days.sorted().joinToString(";")
    }

    private fun parseSessionsHistory(raw: String): Map<String, Int> =
        raw.split(";").filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: return@mapNotNull null)
                else null
            }.toMap()

    private fun serializeSessionsHistory(history: Map<String, Int>): String =
        history.toSortedMap().entries.joinToString(";") { "${it.key}=${it.value}" }
}
