package com.example.hiit.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.hiit.data.SettingsRepository
import kotlinx.coroutines.flow.first

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val repo = SettingsRepository(context)

    /**
     * Programa la siguiente fase del HIIT. Cada aviso lleva la fase que le
     * sigue al usuario y las rondas restantes, para que la sesión avance sola.
     */
    fun scheduleHiitPhase(phase: HiitPhase, delaySeconds: Int, roundsRemaining: Int) {
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1_000L
        val intent = Intent(context, AlarmReceiver::class.java)
            .setData(Uri.parse("hiit://hiit"))
            .putExtra(AlarmReceiver.EXTRA_PHASE, phase.name)
            .putExtra(AlarmReceiver.EXTRA_ROUNDS_REMAINING, roundsRemaining)
        setExact(
            triggerAt,
            PendingIntent.getBroadcast(
                context,
                RC_HIIT,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        Log.i(TAG, "Fase HIIT $phase en $triggerAt (delay=${delaySeconds}s, rondas=$roundsRemaining)")
    }

    /**
     * Programa los pitidos de cuenta regresiva (3, 2, 1) unos segundos antes
     * del cambio de fase. Usa otra identidad para no reemplazar la alarma
     * de la fase.
     */
    fun scheduleHiitCountdown(delaySeconds: Int) {
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1_000L
        setExact(
            triggerAt,
            PendingIntent.getBroadcast(
                context,
                RC_HIIT_COUNTDOWN,
                Intent(context, AlarmReceiver::class.java)
                    .setData(Uri.parse("hiit://hiit-countdown"))
                    .putExtra(AlarmReceiver.EXTRA_COUNTDOWN, true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    fun cancelHiitCountdown() {
        alarmManager.cancel(hiitCountdownIntent())
    }

    fun cancelHiit() {
        alarmManager.cancel(hiitIntent())
        alarmManager.cancel(hiitCountdownIntent())
        Log.i(TAG, "Alarma HIIT cancelada")
    }

    fun canScheduleExactAlarms(): Boolean =
        alarmManager.canScheduleExactAlarms()

    private fun setExact(triggerAt: Long, intent: PendingIntent) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } catch (e: SecurityException) {
            Log.w(TAG, "Sin permiso de alarma exacta; se omite", e)
        }
    }

    // Las alarmas se distinguen por su URI de datos: si fueran idénticas,
    // cancelar una cancelaría también la otra.
    private fun hiitIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            RC_HIIT,
            Intent(context, AlarmReceiver::class.java).setData(Uri.parse("hiit://hiit")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun hiitCountdownIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            RC_HIIT_COUNTDOWN,
            Intent(context, AlarmReceiver::class.java).setData(Uri.parse("hiit://hiit-countdown")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val RC_HIIT = 1002
        private const val RC_HIIT_COUNTDOWN = 1004

        suspend fun rescheduleFromSettings(context: Context) {
            val repo = SettingsRepository(context)
            val settings = repo.settings.first()
            val scheduler = AlarmScheduler(context)

            if (settings.hiitActive) {
                if (settings.hiitPausedSeconds > 0) {
                    // Sesión pausada: se conserva congelada y no se reprograma nada
                    scheduler.cancelHiit()
                } else if (settings.planSteps != null) {
                    // Sesión personalizada: se rearma el paso en curso (el
                    // puntero apunta al siguiente, así que el en curso es el
                    // anterior) con su propia duración.
                    val steps = settings.planSteps ?: emptyList()
                    if (steps.isEmpty()) {
                        scheduler.cancelHiit()
                    } else {
                        val currentIndex = (settings.hiitPlanIndex - 1).coerceIn(0, steps.lastIndex)
                        val step = steps[currentIndex]
                        repo.setHiitState(
                            step.phase.name,
                            currentIndex + 1,
                            System.currentTimeMillis() + step.seconds * 1_000L,
                        )
                        val nextPhase = steps.getOrNull(currentIndex + 1)?.phase ?: HiitPhase.COOLDOWN
                        scheduler.scheduleHiitPhase(nextPhase, step.seconds, 0)
                        repo.setHiitPending(nextPhase.name, 0)
                    }
                } else {
                    // Tras un reinicio se retoma la sesión: la fase de carrera
                    // sonará al terminar el tiempo de caminata actual.
                    repo.setHiitState(
                        HiitPhase.WALK.name,
                        1,
                        System.currentTimeMillis() + settings.hiitWalkSeconds * 1_000L,
                    )
                    scheduler.scheduleHiitPhase(
                        HiitPhase.RUN,
                        settings.hiitWalkSeconds,
                        settings.hiitRounds,
                    )
                    // El pendiente guardado es de antes del reinicio y ya no
                    // coincide con la alarma reprogramada: sin actualizarlo, el
                    // servicio avanzaría la sesión con una fase/rondas erróneas.
                    repo.setHiitPending(HiitPhase.RUN.name, settings.hiitRounds)
                }
            } else {
                scheduler.cancelHiit()
            }
        }
    }
}
