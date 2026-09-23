package com.example.hiit.alarm

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pitidos estilo arranque de competencia para el modo HIIT, generados con
 * ToneGenerator por el canal de alarma (suena aunque el volumen multimedia
 * esté bajo). No requiere archivos de audio.
 */
object SoundPlayer {

    private const val BEEP_GAP_MS = 150L

    /** 3 pitidos cortos, uno por segundo: la cuenta regresiva 3, 2, 1. */
    fun playCountdownBeeps() {
        play(
            listOf(
                ToneGenerator.TONE_PROP_BEEP to 120,
                ToneGenerator.TONE_PROP_BEEP to 120,
                ToneGenerator.TONE_PROP_BEEP to 120,
            ),
            stepMs = 1_000L,
        )
    }

    /** Tono del cambio de fase: caminar es calmado, correr es alto y enérgico. */
    fun playPhaseTone(phase: HiitPhase) {
        when (phase) {
            // Tres pitidos ascendentes: prepárate
            HiitPhase.PREP -> play(
                listOf(
                    ToneGenerator.TONE_PROP_BEEP to 100,
                    ToneGenerator.TONE_PROP_BEEP to 100,
                    ToneGenerator.TONE_PROP_BEEP2 to 250,
                ),
            )
            // Tono medio largo: empieza a caminar
            HiitPhase.WALK -> play(listOf(ToneGenerator.TONE_PROP_BEEP2 to 400))
            // Dos pitidos altos rápidos: ¡a correr!
            HiitPhase.RUN -> play(
                listOf(
                    ToneGenerator.TONE_CDMA_HIGH_L to 150,
                    ToneGenerator.TONE_CDMA_HIGH_L to 250,
                ),
            )
            // Tono suave y calmado: reduce el ritmo poco a poco
            HiitPhase.COOLDOWN -> play(
                listOf(
                    ToneGenerator.TONE_PROP_BEEP2 to 350,
                    ToneGenerator.TONE_PROP_BEEP to 350,
                ),
            )
        }
    }
    /** Ascendente doble: sesión terminada. */
    fun playFinishTone() {
        play(
            listOf(
                ToneGenerator.TONE_PROP_ACK to 250,
                ToneGenerator.TONE_CDMA_HIGH_L to 450,
            ),
        )
    }

    private fun play(steps: List<Pair<Int, Int>>, stepMs: Long? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            var generator: ToneGenerator? = null
            try {
                generator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                for ((tone, durationMs) in steps) {
                    generator.startTone(tone, durationMs)
                    delay(stepMs ?: (durationMs + BEEP_GAP_MS))
                }
                // Pequeño margen para que el último tono termine de sonar
                delay(300)
            } finally {
                generator?.release()
            }
        }
    }
}
