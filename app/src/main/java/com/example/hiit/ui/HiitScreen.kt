package com.example.hiit.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hiit.R
import com.example.hiit.alarm.HiitPhase
import com.example.hiit.alarm.HiitSession
import com.example.hiit.alarm.StepsValidator
import com.example.hiit.alarm.formatDuration
import com.example.hiit.data.AppSettings
import com.example.hiit.data.formatDistance
import java.text.NumberFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Gradientes para el workout ─────────────────────────────────────────────

// Fuego para la alta intensidad: rojo profundo → naranja → ámbar
private val RunGradient = Brush.verticalGradient(
    listOf(Color(0xFFB71C1C), Color(0xFFE64A19), Color(0xFFFF8F00)),
)
private val WalkGradient = Brush.verticalGradient(
    listOf(Mint300, Blue500),
)
private val PrepGradient = Brush.verticalGradient(
    listOf(Aqua500, Blue700),
)
private val CooldownGradient = Brush.verticalGradient(
    listOf(Aqua500, Green700),
)

// ─── HIIT Workout Screen ────────────────────────────────────────────────────

/** Cronómetro en vivo durante la sesión HIIT con anillo Canvas personalizado. */
@Composable
fun HiitWorkoutScreen(
    settings: AppSettings,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val isPrep = settings.hiitPhase == HiitPhase.PREP.name
    val isRun = settings.hiitPhase == HiitPhase.RUN.name
    val isCooldown = settings.hiitPhase == HiitPhase.COOLDOWN.name
    val isPaused = settings.hiitPausedSeconds > 0
    val phaseTotalSec = when {
        isPrep -> settings.hiitWarmupSeconds
        isRun -> settings.hiitRunSeconds
        isCooldown -> settings.hiitCooldownSeconds
        else -> settings.hiitWalkSeconds
    }

    // Reloj de la interfaz: ticks frecuentes para que el anillo avance fluido
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(100)
        }
    }

    // Contador de pasos en vivo: consulta el sensor cada 2 s y lo compara con
    // la línea de base anotada al arrancar la sesión
    var liveSteps by remember { mutableIntStateOf(0) }
    LaunchedEffect(settings.hiitStepBaseline) {
        val baseline = settings.hiitStepBaseline
        if (baseline >= 0) {
            while (true) {
                StepsValidator.readCounterOnce(context)?.let { current ->
                    liveSteps = (current - baseline).toInt().coerceAtLeast(0)
                }
                delay(2_000)
            }
        }
    }

    // En pausa el tiempo queda congelado: se muestra el guardado, no el del reloj
    val remainingMs = if (isPaused) {
        settings.hiitPausedSeconds * 1_000L
    } else {
        (settings.hiitPhaseEnd - nowMs).coerceAtLeast(0L)
    }
    val remainingSec = remainingMs / 1_000L
    val rawProgress = if (phaseTotalSec > 0) {
        (1f - remainingMs / (phaseTotalSec * 1_000f)).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(120),
        label = "progress",
    )

    val background = when {
        isPrep -> PrepGradient
        isRun -> RunGradient
        isCooldown -> CooldownGradient
        else -> WalkGradient
    }

    // Colores del anillo
    val arcColorStart = when {
        isPrep -> Blue300
        isRun -> Color(0xFFFFCA28)
        isCooldown -> Blue300
        else -> Mint300
    }
    val arcColorEnd = when {
        isPrep -> Blue500
        isRun -> Color(0xFFFF6D00)
        isCooldown -> Green700
        else -> Mint500
    }
    val trackColor = Color.White.copy(alpha = 0.12f)
    val glowColor = when {
        isPrep -> Blue300.copy(alpha = 0.3f)
        isRun -> Color(0xFFFF8F00).copy(alpha = 0.35f)
        isCooldown -> Blue300.copy(alpha = 0.3f)
        else -> Mint300.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Label superior
            Text(
                stringResource(R.string.hiit_workout_label),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Ronda
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.1f),
            ) {
                Text(
                    when {
                        isCooldown -> stringResource(R.string.hiit_last_phase)
                        // Rondas infinitas: solo se muestra la ronda actual
                        settings.hiitRounds <= 0 -> stringResource(
                            R.string.hiit_round_count,
                            settings.hiitRound,
                        )
                        else -> stringResource(
                            R.string.hiit_round_of,
                            settings.hiitRound,
                            settings.hiitRounds,
                        )
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Fase con animación de cambio
            AnimatedContent(
                targetState = Triple(isPrep || isCooldown, isRun, isPaused),
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(300),
                    )).togetherWith(fadeOut(tween(200)))
                },
                label = "phaseLabel",
            ) { (specialPhase, running, paused) ->
                Text(
                    when {
                        paused -> stringResource(R.string.hiit_phase_paused)
                        isPrep -> stringResource(R.string.hiit_phase_prep)
                        isCooldown -> stringResource(R.string.hiit_phase_cooldown)
                        running -> stringResource(R.string.hiit_phase_run)
                        else -> stringResource(R.string.hiit_phase_walk)
                    },
                    fontSize = if (specialPhase && !paused) 30.sp else 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── Anillo Canvas personalizado ──────────────────────────
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    val glowStrokeWidth = 24.dp.toPx()
                    val padding = glowStrokeWidth / 2
                    val arcSize = Size(
                        size.width - glowStrokeWidth,
                        size.height - glowStrokeWidth,
                    )
                    val topLeft = Offset(padding, padding)

                    // Track de fondo
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                        ),
                    )

                    // Glow exterior sutil
                    drawArc(
                        color = glowColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = glowStrokeWidth,
                            cap = StrokeCap.Round,
                        ),
                    )

                    // Arco de progreso con gradiente
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(arcColorStart, arcColorEnd, arcColorStart),
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                        ),
                    )

                    // Punto luminoso en el extremo del arco
                    if (progress > 0.01f) {
                        val angle = Math.toRadians((-90.0 + 360.0 * progress))
                        val cx = size.width / 2 + (arcSize.width / 2) * kotlin.math.cos(angle).toFloat()
                        val cy = size.height / 2 + (arcSize.height / 2) * kotlin.math.sin(angle).toFloat()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            radius = strokeWidth * 0.6f,
                            center = Offset(cx, cy),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = strokeWidth * 1.2f,
                            center = Offset(cx, cy),
                        )
                    }
                }

                // Cronómetro en el centro
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        String.format("%02d:%02d", remainingSec / 60, remainingSec % 60),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        when {
                            isPaused -> stringResource(R.string.hiit_sub_paused)
                            isPrep -> stringResource(R.string.hiit_sub_prep)
                            isCooldown -> stringResource(R.string.hiit_sub_cooldown)
                            isRun -> stringResource(R.string.hiit_sub_run)
                            else -> stringResource(R.string.hiit_sub_walk)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "${formatDuration(context, settings.hiitWalkSeconds)}   ·   " +
                    formatDuration(context, settings.hiitRunSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
            )

            // Pasos y distancia de la sesión en vivo (solo con sensor)
            if (settings.hiitStepBaseline >= 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(
                            R.string.hiit_live_steps,
                            NumberFormat.getInstance().format(liveSteps),
                            formatDistance(HiitSession.distanceMeters(liveSteps)),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botones de control: cuadro de pausa/reanudar + terminar sesión
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Botón cuadrado de pausa / reanudar (solo ícono)
                Button(
                    onClick = {
                        scope.launch {
                            if (isPaused) HiitSession.resume(context) else HiitSession.pause(context)
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        // Mismo cristal en ambos estados: solo cambia el icono
                        // (pausa/reanudar), siempre blanco como el resto de la
                        // pantalla. El estado ya lo marca el cronómetro.
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White,
                    ),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) {
                            stringResource(R.string.hiit_resume)
                        } else {
                            stringResource(R.string.hiit_pause)
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Botón principal: Terminar Sesión (ocupa el resto del ancho)
                Button(
                    onClick = { scope.launch { HiitSession.stop(context) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        stringResource(R.string.hiit_finish),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ─── HIIT Completed Screen ──────────────────────────────────────────────────

/** Pantalla de celebración al completar una sesión HIIT. */
@Composable
fun HiitCompletedScreen(
    settings: AppSettings,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    // Animación de entrada
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Green500,
                        Blue700,
                    ),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Emoji con spring scale
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                ) + fadeIn(),
            ) {
                Text("🎉", fontSize = 72.sp)
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(600, delayMillis = 200),
                ),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        stringResource(R.string.hiit_completed_title),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Text(
                        stringResource(R.string.hiit_completed_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats card
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 400)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(500, delayMillis = 400),
                ),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(20.dp),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CompletedStatRow(
                            stringResource(R.string.hiit_stat_rounds),
                            // Rondas realmente alcanzadas (clave con rondas
                            // infinitas o al terminar a mano antes de tiempo)
                            "${if (settings.hiitLastRounds > 0) settings.hiitLastRounds else settings.hiitRounds}",
                        )
                        CompletedStatRow(
                            stringResource(R.string.hiit_stat_intervals),
                            "${formatDuration(context, settings.hiitWalkSeconds)} · " +
                                formatDuration(context, settings.hiitRunSeconds),
                        )
                        CompletedStatRow(
                            stringResource(R.string.hiit_stat_total_time),
                            formatDuration(
                                context,
                                if (settings.hiitLastSeconds > 0) {
                                    settings.hiitLastSeconds
                                } else {
                                    settings.hiitTotalSeconds
                                },
                            ),
                        )
                        if (StepsValidator.hasSensor(context)) {
                            CompletedStatRow(
                                stringResource(R.string.hiit_stat_steps),
                                NumberFormat.getInstance().format(settings.hiitLastSteps),
                            )
                            CompletedStatRow(
                                stringResource(R.string.hiit_stat_distance),
                                formatDistance(settings.hiitLastDistanceM),
                            )
                        }
                        CompletedStatRow(
                            stringResource(R.string.hiit_stat_total_sessions),
                            "${settings.hiitTotalSessions}",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = 600)),
            ) {
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Green700,
                    ),
                ) {
                    Text(stringResource(R.string.common_back), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CompletedStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
