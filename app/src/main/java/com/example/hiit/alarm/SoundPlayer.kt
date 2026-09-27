package com.example.hiit.alarm

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import com.example.hiit.util.AudioFocusDucker
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
    // Volumen relativo de los tonos agudos con el modo interior activado (0-100)
    private const val INDOOR_VOLUME = 30

    /** Un tono: frecuencia, duración y si es agudo (se atenúa en modo interior). */
    private data class Step(val tone: Int, val durationMs: Int, val sharp: Boolean = false)

    /** 3 pitidos cortos, uno por segundo: la cuenta regresiva 3, 2, 1. */
    fun playCountdownBeeps(context: Context) {
        play(
            context,
            listOf(
                Step(ToneGenerator.TONE_PROP_BEEP, 120),
                Step(ToneGenerator.TONE_PROP_BEEP, 120),
                Step(ToneGenerator.TONE_PROP_BEEP, 120),
            ),
            stepMs = 1_000L,
        )
    }

    /** Tono del cambio de fase: caminar es calmado, correr es alto y enérgico. */
    fun playPhaseTone(context: Context, phase: HiitPhase, indoor: Boolean = false) {
        when (phase) {
            // Tres pitidos ascendentes: prepárate
            HiitPhase.PREP -> play(
                context,
                listOf(
                    Step(ToneGenerator.TONE_PROP_BEEP, 100),
                    Step(ToneGenerator.TONE_PROP_BEEP, 100),
                    Step(ToneGenerator.TONE_PROP_BEEP2, 250),
                ),
            )
            // Tono medio largo: empieza a caminar
            HiitPhase.WALK -> play(context, listOf(Step(ToneGenerator.TONE_PROP_BEEP2, 400)))
            // Tono medio con ligero ascenso: sube a trote
            HiitPhase.JOG -> play(
                context,
                listOf(
                    Step(ToneGenerator.TONE_PROP_BEEP2, 200),
                    Step(ToneGenerator.TONE_CDMA_HIGH_L, 250),
                ),
            )
            // Dos pitidos agudos rápidos: ¡a correr!
            HiitPhase.RUN -> play(
                context,
                listOf(
                    Step(ToneGenerator.TONE_CDMA_HIGH_L, 150, sharp = true),
                    Step(ToneGenerator.TONE_CDMA_HIGH_L, 250, sharp = true),
                ),
                indoor = indoor,
            )
            // Tono suave y calmado: reduce el ritmo poco a poco
            HiitPhase.COOLDOWN -> play(
                context,
                listOf(
                    Step(ToneGenerator.TONE_PROP_BEEP2, 350),
                    Step(ToneGenerator.TONE_PROP_BEEP, 350),
                ),
            )
        }
    }
    /** Ascendente doble: sesión terminada. */
    fun playFinishTone(context: Context, indoor: Boolean = false) {
        play(
            context,
            listOf(
                Step(ToneGenerator.TONE_PROP_ACK, 250),
                Step(ToneGenerator.TONE_CDMA_HIGH_L, 450, sharp = true),
            ),
            indoor = indoor,
        )
    }

    private fun play(context: Context, steps: List<Step>, stepMs: Long? = null, indoor: Boolean = false) {
        CoroutineScope(Dispatchers.Default).launch {
            // Ducking: cubre toda la secuencia más un margen de 500 ms para que
            // la música ajena no suba justo al cortar el último tono
            val totalMs = steps.sumOf { stepMs ?: (it.durationMs + BEEP_GAP_MS) } + 300 + 500
            AudioFocusDucker.duck(context, totalMs)
            var generator: ToneGenerator? = null
            var generatorVolume = -1
            try {
                for ((tone, durationMs, sharp) in steps) {
                    val volume = if (indoor && sharp) INDOOR_VOLUME else 100
                    // El volumen se fija al crear el generator, así que se
                    // recrea solo cuando cambia entre tonos agudos y normales
                    if (volume != generatorVolume) {
                        generator?.release()
                        generator = ToneGenerator(AudioManager.STREAM_ALARM, volume)
                        generatorVolume = volume
                    }
                    generator?.startTone(tone, durationMs)
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
