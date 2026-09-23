package com.example.hiit.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.hiit.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.Default)

        scope.launch {
            try {
                val repo = SettingsRepository(appContext)
                val settings = repo.settings.first()

                when {
                    // Pitidos de cuenta regresiva antes del cambio de fase HIIT
                    intent.getBooleanExtra(EXTRA_COUNTDOWN, false) -> {
                        if (settings.hiitActive && settings.hiitSounds) {
                            SoundPlayer.playCountdownBeeps()
                        }
                    }

                    // Aviso de una fase del HIIT: el motor avanza la sesión
                    phaseFrom(intent) != null -> {
                        HiitEngine.advance(
                            appContext,
                            phaseFrom(intent)!!,
                            intent.getIntExtra(EXTRA_ROUNDS_REMAINING, 1),
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun phaseFrom(intent: Intent): HiitPhase? =
        intent.getStringExtra(EXTRA_PHASE)
            ?.let { runCatching { HiitPhase.valueOf(it) }.getOrNull() }

    companion object {
        const val EXTRA_COUNTDOWN = "com.example.hiit.EXTRA_COUNTDOWN"
        const val EXTRA_PHASE = "com.example.hiit.EXTRA_PHASE"
        const val EXTRA_ROUNDS_REMAINING = "com.example.hiit.EXTRA_ROUNDS_REMAINING"
        const val COUNTDOWN_LEAD_SECONDS = 3
    }
}
