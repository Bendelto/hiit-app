package com.example.hiit.tts

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TtsSpeaker(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    init {
        val preferredLocale = speechLocale(context)
        tts = TextToSpeech(context.applicationContext) { status ->
            val engine = tts ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                Log.w(TAG, "TextToSpeech no disponible (status=$status)")
                return@TextToSpeech
            }
            applyLanguage(engine, preferredLocale)
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    shutdown()
                }

                @Deprecated("Reemplazado por onError(id, errorCode) en API 21; minSdk es 26")
                override fun onError(utteranceId: String?) {
                    shutdown()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    shutdown()
                }
            })
            ready = true
            // El texto pedido antes de que el motor estuviera listo se habla ahora
            pendingText?.let { text ->
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
            }
            pendingText = null
        }
    }

    fun speak(text: String) {
        val engine = tts
        if (ready && engine != null) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        } else {
            pendingText = text
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        pendingText = null
    }

    /** Idioma de la voz según el idioma de la app: español, portugués o inglés. */
    private fun speechLocale(context: Context): Locale =
        when (context.resources.configuration.locales[0]?.language) {
            "es" -> Locale("es", "ES")
            "pt" -> Locale("pt", "BR")
            else -> Locale.ENGLISH
        }

    /** Fija el idioma elegido; si el motor no lo tiene, prueba con el del sistema. */
    private fun applyLanguage(engine: TextToSpeech, preferred: Locale) {
        if (engine.isLanguageAvailable(preferred) >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = preferred
            return
        }
        Log.w(TAG, "Voz no disponible para $preferred, se prueba con la del sistema")
        val fallback = Locale.getDefault()
        if (engine.isLanguageAvailable(fallback) >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = fallback
        } else {
            Log.w(TAG, "Voz no disponible para $fallback; se usa el idioma por defecto del motor")
        }
    }

    private companion object {
        const val TAG = "TtsSpeaker"
        const val UTTERANCE_ID = "hiit_utterance"
    }
}
