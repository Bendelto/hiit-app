package com.example.hiit.alarm

import android.content.Context
import com.example.hiit.R
import com.example.hiit.data.SettingsRepository
import com.example.hiit.tts.TtsSpeaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Motor del modo HIIT: inicia y detiene la sesión y construye los mensajes
 * hablados de cada fase. La alternancia preparación → camina/corre se logra
 * encadenando alarmas (o los ticks del servicio en primer plano): cada aviso
 * programa el siguiente y actualiza el estado en vivo (fase, ronda y fin de
 * fase) que lee la pantalla del cronómetro.
 */
object HiitSession {

    /**
     * Zancada media estimada (m) para convertir pasos a distancia. Corresponde
     * a una persona de ~1,80 m (zancada ≈ altura × 0,415): la referencia más
     * usada al no conocer la altura del usuario. En sesiones de prueba con
     * pocos pasos la percepción suele inflar la distancia real.
     */
    private const val STRIDE_METERS = 0.762f

    /** Mensaje hablado y notificado al terminar la sesión. */
    fun finishMessage(context: Context): String =
        context.getString(R.string.tts_hiit_finished)

    /** Distancia estimada en metros a partir de los pasos de la sesión. */
    fun distanceMeters(steps: Int): Int = (steps * STRIDE_METERS).toInt()

    /**
     * Pasos caminados desde el inicio de la sesión: contador del sensor menos
     * la línea de base anotada al arrancar. 0 si no hay sensor o lectura.
     */
    suspend fun measureSteps(context: Context, baseline: Float): Int {
        if (baseline < 0) return 0
        val current = StepsValidator.readCounterOnce(context) ?: return 0
        return (current - baseline).toInt().coerceAtLeast(0)
    }

    /**
     * Arranca la sesión. Con calentamiento (> 0 s) empieza con la cuenta de
     * preparación y programa la primera caminata; sin calentamiento arranca
     * directamente en la caminata de la ronda 1.
     */
    suspend fun start(
        context: Context,
        walkSeconds: Int,
        runSeconds: Int,
        rounds: Int,
        warmupSeconds: Int,
        cooldownSeconds: Int,
    ) {
        val repo = SettingsRepository(context)
        val settings = repo.settings.first()
        repo.setHiitActive(true)
        // Línea de base de pasos y hora de inicio: alimentan el contador en
        // vivo y el resumen de la sesión (solo con sensor y permiso)
        val stepBaseline = if (StepsValidator.hasSensor(context) && StepsValidator.hasPermission(context)) {
            StepsValidator.readCounterOnce(context) ?: -1f
        } else {
            -1f
        }
        repo.setHiitSessionStart(System.currentTimeMillis(), stepBaseline)
        val scheduler = AlarmScheduler(context)
        if (warmupSeconds > 0) {
            repo.setHiitState(
                HiitPhase.PREP.name,
                1,
                System.currentTimeMillis() + warmupSeconds * 1_000L,
            )
            repo.setHiitPending(HiitPhase.WALK.name, rounds)
            if (settings.hiitSounds) SoundPlayer.playPhaseTone(HiitPhase.PREP)
            if (settings.hiitVoice) {
                speak(
                    context,
                    context.getString(
                        R.string.tts_hiit_start,
                        speakDuration(context, warmupSeconds),
                    ),
                )
            }
            scheduler.scheduleHiitPhase(HiitPhase.WALK, warmupSeconds, rounds)
            if (settings.hiitSounds && warmupSeconds > AlarmReceiver.COUNTDOWN_LEAD_SECONDS + 2) {
                scheduler.scheduleHiitCountdown(warmupSeconds - AlarmReceiver.COUNTDOWN_LEAD_SECONDS)
            }
        } else {
            repo.setHiitState(
                HiitPhase.WALK.name,
                1,
                System.currentTimeMillis() + walkSeconds * 1_000L,
            )
            repo.setHiitPending(HiitPhase.RUN.name, rounds)
            val cue = walkCue(context, walkSeconds)
            Notifier.show(context, cue)
            if (settings.hiitSounds) SoundPlayer.playPhaseTone(HiitPhase.WALK)
            if (settings.hiitVoice) speak(context, cue)
            scheduler.scheduleHiitPhase(HiitPhase.RUN, walkSeconds, rounds)
            if (settings.hiitSounds && walkSeconds > AlarmReceiver.COUNTDOWN_LEAD_SECONDS + 2) {
                scheduler.scheduleHiitCountdown(walkSeconds - AlarmReceiver.COUNTDOWN_LEAD_SECONDS)
            }
        }
        HiitNotificationService.start(context)
    }

    /**
     * Pausa la sesión: cancela las alarmas programadas y congela el tiempo
     * que quedaba de la fase actual, para retomarlo con [resume].
     */
    suspend fun pause(context: Context) {
        val repo = SettingsRepository(context)
        val settings = repo.settings.first()
        val remaining = ((settings.hiitPhaseEnd - System.currentTimeMillis()) / 1_000L)
            .coerceAtLeast(1L)
            .toInt()
        AlarmScheduler(context).cancelHiit()
        // Se congela el fin de fase para que la interfaz muestre el tiempo detenido
        repo.setHiitState(
            settings.hiitPhase,
            settings.hiitRound,
            System.currentTimeMillis() + remaining * 1_000L,
        )
        repo.setHiitPaused(remaining)
    }

    /** Reanuda una sesión pausada: programa la fase pendiente con el tiempo congelado. */
    suspend fun resume(context: Context) {
        val repo = SettingsRepository(context)
        val settings = repo.settings.first()
        val pendingPhase = runCatching {
            HiitPhase.valueOf(settings.hiitPendingPhase)
        }.getOrNull() ?: HiitPhase.RUN
        val seconds = settings.hiitPausedSeconds.coerceAtLeast(1)
        // 0 = rondas infinitas: se respeta el marcador, no se sube a 1
        val rounds = settings.hiitPendingRounds.coerceAtLeast(0)

        repo.setHiitState(
            settings.hiitPhase,
            settings.hiitRound,
            System.currentTimeMillis() + seconds * 1_000L,
        )
        val scheduler = AlarmScheduler(context)
        scheduler.scheduleHiitPhase(pendingPhase, seconds, rounds)
        if (settings.hiitSounds && seconds > AlarmReceiver.COUNTDOWN_LEAD_SECONDS + 2) {
            scheduler.scheduleHiitCountdown(seconds - AlarmReceiver.COUNTDOWN_LEAD_SECONDS)
        }
        repo.setHiitPaused(0)
        HiitNotificationService.start(context)
    }

    /**
     * Detiene la sesión: cancela las alarmas y limpia el estado en vivo. Con
     * [recordCompletion] (terminar a mano desde la pantalla de la sesión)
     * además mide pasos/distancia y guarda el resumen, que dispara la
     * pantalla de celebración.
     */
    suspend fun stop(context: Context, recordCompletion: Boolean = true) {
        AlarmScheduler(context).cancelHiit()
        val repo = SettingsRepository(context)
        if (recordCompletion) {
            val settings = repo.settings.first()
            val steps = measureSteps(context, settings.hiitStepBaseline)
            val seconds = if (settings.hiitStartedAt > 0) {
                ((System.currentTimeMillis() - settings.hiitStartedAt) / 1_000L).toInt()
            } else {
                settings.hiitTotalSeconds
            }
            repo.recordHiitCompleted(
                rounds = settings.hiitRound,
                seconds = seconds,
                steps = steps,
                distanceMeters = distanceMeters(steps),
            )
        }
        repo.setHiitActive(false)
        repo.clearHiitState()
    }

    fun walkCue(context: Context, walkSeconds: Int): String =
        context.getString(R.string.tts_hiit_cue_walk, speakDuration(context, walkSeconds))

    fun runCue(context: Context, runSeconds: Int): String =
        context.getString(R.string.tts_hiit_cue_run, speakDuration(context, runSeconds))

    fun speak(context: Context, text: String) {
        // TtsSpeaker encola el texto hasta que el motor esté listo y se apaga
        // solo al terminar de hablar; esto solo es un red de seguridad
        val speaker = TtsSpeaker(context)
        speaker.speak(text)
        CoroutineScope(Dispatchers.Default).launch {
            delay(30_000)
            speaker.shutdown()
        }
    }
}
