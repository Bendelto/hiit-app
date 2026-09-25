package com.example.hiit.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.hiit.MainActivity
import com.example.hiit.R
import com.example.hiit.data.AppSettings
import com.example.hiit.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que mantiene una notificación persistente mientras
 * dura la sesión HIIT: muestra la fase, la ronda y el tiempo restante, y
 * ofrece acciones de Pausar/Reanudar y Terminar sin abrir la app.
 *
 * El servicio no controla la sesión (eso lo hacen las alarmas); solo observa
 * el estado en vivo y se autodestruye cuando la sesión deja de estar activa.
 */
class HiitNotificationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundWithType(buildNotification(null))
        watchSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> scope.launch { HiitSession.pause(applicationContext) }
            ACTION_RESUME -> scope.launch { HiitSession.resume(applicationContext) }
            ACTION_STOP -> scope.launch { HiitSession.stop(applicationContext) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Redibuja la notificación cada segundo, avanza la sesión cuando una fase
     *  vence y se cierra solo al terminar. */
    private fun watchSession() {
        val repo = SettingsRepository(applicationContext)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        scope.launch {
            repo.settings.collectLatest { settings ->
                if (!settings.hiitActive) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collectLatest
                }
                // collectLatest cancela este bucle cuando cambian los ajustes
                // (cambio de fase, pausa), y se reinicia con el estado fresco.
                while (currentCoroutineContext().isActive) {
                    manager.notify(NOTIFICATION_ID, buildNotification(settings))
                    advanceIfPhaseEnded(settings)
                    delay(1_000)
                }
            }
        }
    }

    /**
     * El servicio avanza la sesión cuando una fase vence, igual que haría la
     * alarma. HiitEngine tiene una guarda de duplicados: si la alarma también
     * dispara, la segunda llamada no hace nada.
     */
    private suspend fun advanceIfPhaseEnded(settings: AppSettings) {
        if (settings.hiitPausedSeconds > 0) return
        if (settings.hiitPhaseEnd <= 0 ||
            settings.hiitPhaseEnd > System.currentTimeMillis()
        ) {
            return
        }
        val pending = runCatching { HiitPhase.valueOf(settings.hiitPendingPhase) }
            .getOrNull() ?: return
        HiitEngine.advance(applicationContext, pending, settings.hiitPendingRounds.coerceAtLeast(1))
    }

    private fun buildNotification(settings: AppSettings?): Notification {
        val isPaused = settings != null && settings.hiitPausedSeconds > 0
        val remainingSec = when {
            settings == null -> 0L
            isPaused -> settings.hiitPausedSeconds.toLong()
            else -> ((settings.hiitPhaseEnd - System.currentTimeMillis()) / 1_000L)
                .coerceAtLeast(0L)
        }
        val timeText = "%02d:%02d".format(remainingSec / 60, remainingSec % 60)

        val title = when {
            settings == null -> getString(R.string.notif_hiit_ongoing)
            isPaused -> getString(R.string.notif_hiit_paused)
            settings.hiitPhase == HiitPhase.PREP.name -> getString(R.string.notif_hiit_prep)
            settings.hiitPhase == HiitPhase.JOG.name -> getString(R.string.notif_hiit_jog)
            settings.hiitPhase == HiitPhase.RUN.name -> getString(R.string.notif_hiit_run)
            settings.hiitPhase == HiitPhase.COOLDOWN.name -> getString(R.string.notif_hiit_cooldown)
            else -> getString(R.string.notif_hiit_walk)
        }
        val text = if (settings == null) {
            getString(R.string.notif_hiit_session_running)
        } else {
            getString(
                R.string.notif_hiit_round_line,
                settings.hiitRound,
                settings.hiitRounds,
                timeText,
            )
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(contentIntent)

        // Acción principal: pausar o reanudar según el estado
        val (toggleLabel, toggleAction) = if (isPaused) {
            getString(R.string.notif_action_resume) to ACTION_RESUME
        } else {
            getString(R.string.notif_action_pause) to ACTION_PAUSE
        }
        builder.addAction(0, toggleLabel, serviceIntent(RC_TOGGLE, toggleAction))
        builder.addAction(
            0,
            getString(R.string.notif_action_stop),
            serviceIntent(RC_STOP, ACTION_STOP),
        )

        return builder.build()
    }

    private fun serviceIntent(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, HiitNotificationService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun startForegroundWithType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_hiit_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_hiit_channel_description)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "HiitNotificationSvc"
        private const val CHANNEL_ID = "hiit_session"
        private const val NOTIFICATION_ID = 1005
        private const val RC_TOGGLE = 2001
        private const val RC_STOP = 2002

        const val ACTION_START = "com.example.hiit.action.HIIT_NOTIF_START"
        const val ACTION_PAUSE = "com.example.hiit.action.HIIT_NOTIF_PAUSE"
        const val ACTION_RESUME = "com.example.hiit.action.HIIT_NOTIF_RESUME"
        const val ACTION_STOP = "com.example.hiit.action.HIIT_NOTIF_STOP"

        /** Arranca el servicio si no está corriendo; seguro llamarlo varias veces. */
        fun start(context: Context) {
            val intent = Intent(context, HiitNotificationService::class.java)
                .setAction(ACTION_START)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // En segundo plano puede estar restringido; la sesión sigue por alarmas
                Log.w(TAG, "No se pudo iniciar la notificación persistente", e)
            }
        }
    }
}
