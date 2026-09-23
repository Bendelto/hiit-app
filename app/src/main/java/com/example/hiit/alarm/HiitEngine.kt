package com.example.hiit.alarm

import android.content.Context
import com.example.hiit.R
import com.example.hiit.data.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Motor de transiciones del HIIT, compartido por AlarmReceiver (alarmas) y
 * HiitNotificationService (ticks del servicio en primer plano). Cualquiera de
 * las dos vías puede avanzar la sesión; la guarda de duplicados evita que una
 * transición se aplique dos veces cuando ambas coinciden.
 */
object HiitEngine {

    /**
     * Aplica la transición [targetPhase] con [roundsRemaining] rondas por
     * delante. Es seguro llamarlo varias veces con los mismos datos: tras la
     * primera, el fin de fase queda en el futuro y las llamadas siguientes
     * no hacen nada.
     */
    suspend fun advance(
        context: Context,
        targetPhase: HiitPhase,
        roundsRemaining: Int,
    ) {
        val repo = SettingsRepository(context)
        val settings = repo.settings.first()

        // La sesión pudo cancelarse mientras la alarma estaba programada
        if (!settings.hiitActive) return
        // Sesión pausada: no se avanza nada; el resume reprogramará la alarma
        if (settings.hiitPausedSeconds > 0) return

        // Guarda de duplicados: si la fase actual aún no vence, la transición
        // ya ocurrió (p. ej. el servicio avanzó antes que la alarma).
        if (settings.hiitPhaseEnd > System.currentTimeMillis() + 2_000) return

        // Rondas infinitas (0): la sesión no termina sola, la detiene el usuario
        val infinite = settings.hiitRounds <= 0

        HiitNotificationService.start(context)

        val scheduler = AlarmScheduler(context)
        when (targetPhase) {
            // Se agotó el tiempo de caminata: toca correr
            HiitPhase.RUN -> {
                repo.setHiitState(
                    HiitPhase.RUN.name,
                    settings.hiitRound,
                    System.currentTimeMillis() + settings.hiitRunSeconds * 1_000L,
                )
                announcePhase(context, settings, HiitPhase.RUN)
                scheduler.scheduleHiitPhase(HiitPhase.WALK, settings.hiitRunSeconds, roundsRemaining)
                repo.setHiitPending(HiitPhase.WALK.name, roundsRemaining)
                scheduleCountdownIfFits(scheduler, settings, settings.hiitRunSeconds)
            }
            // Se agotó el tiempo de carrera (o de preparación): primera
            // caminata, siguiente ronda o fin de sesión
            HiitPhase.WALK -> {
                if (settings.hiitPhase == HiitPhase.PREP.name) {
                    // Fin de la preparación: comienza la caminata de la ronda 1
                    repo.setHiitState(
                        HiitPhase.WALK.name,
                        1,
                        System.currentTimeMillis() + settings.hiitWalkSeconds * 1_000L,
                    )
                    announcePhase(context, settings, HiitPhase.WALK)
                    scheduler.scheduleHiitPhase(
                        HiitPhase.RUN,
                        settings.hiitWalkSeconds,
                        roundsRemaining,
                    )
                    repo.setHiitPending(HiitPhase.RUN.name, roundsRemaining)
                    scheduleCountdownIfFits(scheduler, settings, settings.hiitWalkSeconds)
                } else if (!infinite && roundsRemaining <= 1) {
                    if (settings.hiitCooldownSeconds > 0) {
                        // Última ronda completada: fase de enfriamiento antes de terminar
                        repo.setHiitState(
                            HiitPhase.COOLDOWN.name,
                            settings.hiitRounds,
                            System.currentTimeMillis() + settings.hiitCooldownSeconds * 1_000L,
                        )
                        announcePhase(context, settings, HiitPhase.COOLDOWN)
                        scheduler.scheduleHiitPhase(
                            HiitPhase.COOLDOWN,
                            settings.hiitCooldownSeconds,
                            1,
                        )
                        repo.setHiitPending(HiitPhase.COOLDOWN.name, 1)
                        scheduleCountdownIfFits(scheduler, settings, settings.hiitCooldownSeconds)
                    } else {
                        finishSession(context, settings)
                    }
                } else {
                    // Siguiente ronda (con rondas fijas se descuenta una;
                    // con infinitas el pendiente se mantiene en 0)
                    val pendingRounds = if (infinite) 0 else roundsRemaining - 1
                    repo.setHiitState(
                        HiitPhase.WALK.name,
                        settings.hiitRound + 1,
                        System.currentTimeMillis() + settings.hiitWalkSeconds * 1_000L,
                    )
                    announcePhase(context, settings, HiitPhase.WALK)
                    scheduler.scheduleHiitPhase(
                        HiitPhase.RUN,
                        settings.hiitWalkSeconds,
                        pendingRounds,
                    )
                    repo.setHiitPending(HiitPhase.RUN.name, pendingRounds)
                    scheduleCountdownIfFits(scheduler, settings, settings.hiitWalkSeconds)
                }
            }
            // La preparación nunca se programa como transición: arranca la sesión
            HiitPhase.PREP -> Unit
            // Fin del enfriamiento: la sesión termina
            HiitPhase.COOLDOWN -> finishSession(context, settings)
        }
    }

    /** Aviso final de la sesión: notificación, tono, voz y registro del logro. */
    private suspend fun finishSession(
        context: Context,
        settings: com.example.hiit.data.AppSettings,
    ) {
        val finishMessage = HiitSession.finishMessage(context)
        // El aviso final se queda un poco más: el usuario puede haber dejado
        // el teléfono apartado al terminar la sesión
        Notifier.show(context, finishMessage, timeoutMs = 8_000)
        if (settings.hiitSounds) SoundPlayer.playFinishTone()
        if (settings.hiitVoice) HiitSession.speak(context, finishMessage)
        val repo = SettingsRepository(context)
        val steps = HiitSession.measureSteps(context, settings.hiitStepBaseline)
        val seconds = if (settings.hiitStartedAt > 0) {
            ((System.currentTimeMillis() - settings.hiitStartedAt) / 1_000L).toInt()
        } else {
            settings.hiitTotalSeconds
        }
        repo.recordHiitCompleted(
            rounds = settings.hiitRound,
            seconds = seconds,
            steps = steps,
            distanceMeters = HiitSession.distanceMeters(steps),
        )
        repo.setHiitActive(false)
        repo.clearHiitState()
    }

    /** Tono distintivo del cambio de fase + notificación + voz, según los ajustes. */
    private fun announcePhase(context: Context, settings: com.example.hiit.data.AppSettings, phase: HiitPhase) {
        val cue = when (phase) {
            HiitPhase.RUN -> HiitSession.runCue(context, settings.hiitRunSeconds)
            HiitPhase.WALK -> HiitSession.walkCue(context, settings.hiitWalkSeconds)
            HiitPhase.PREP -> context.getString(R.string.tts_hiit_prep)
            HiitPhase.COOLDOWN -> context.getString(R.string.tts_hiit_cooldown_cue)
        }
        Notifier.show(context, cue)
        if (settings.hiitSounds) SoundPlayer.playPhaseTone(phase)
        if (settings.hiitVoice) HiitSession.speak(context, cue)
    }

    /** Los pitidos 3-2-1 solo tienen sentido si la fase dura más que la cuenta regresiva. */
    private fun scheduleCountdownIfFits(
        scheduler: AlarmScheduler,
        settings: com.example.hiit.data.AppSettings,
        phaseSeconds: Int,
    ) {
        if (!settings.hiitSounds) return
        if (phaseSeconds <= AlarmReceiver.COUNTDOWN_LEAD_SECONDS + 2) return
        scheduler.scheduleHiitCountdown(phaseSeconds - AlarmReceiver.COUNTDOWN_LEAD_SECONDS)
    }
}
