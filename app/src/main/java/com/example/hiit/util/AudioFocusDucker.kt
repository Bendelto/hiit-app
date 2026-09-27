package com.example.hiit.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ducking de audio: pide el foco de audio en modo transitorio "may duck" para
 * que la música de otras apps (Spotify, etc.) baje su volumen mientras suena
 * una señal de la sesión (pitido o voz) y vuelva a subir al soltarlo. Cada
 * aviso aplaza la liberación: si dos señales se encadenan, el foco se
 * mantiene hasta que termina la última. Si el sistema niega el foco, la
 * señal suena igual; solo se pierde el efecto de bajar la música ajena.
 */
object AudioFocusDucker {

    private val lock = Any()
    private var focusRequest: AudioFocusRequest? = null
    private var releaseJob: Job? = null

    /** Baja la música ajena durante [durationMs]. Reentrante: cada llamada aplaza la liberación. */
    fun duck(context: Context, durationMs: Long) {
        val audioManager =
            context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        synchronized(lock) {
            val request = focusRequest ?: AudioFocusRequest.Builder(
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .build()
                .also { focusRequest = it }
            audioManager.requestAudioFocus(request)
            releaseJob?.cancel()
            releaseJob = CoroutineScope(Dispatchers.Default).launch {
                delay(durationMs)
                synchronized(lock) {
                    audioManager.abandonAudioFocusRequest(request)
                    releaseJob = null
                }
            }
        }
    }
}
