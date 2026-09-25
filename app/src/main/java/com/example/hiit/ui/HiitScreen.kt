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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hiit.R
import com.example.hiit.alarm.HiitPhase
import com.example.hiit.alarm.HiitSession
import com.example.hiit.alarm.StepsValidator
import com.example.hiit.alarm.formatDuration
import com.example.hiit.data.AppSettings
import com.example.hiit.data.SettingsRepository
import com.example.hiit.data.formatDistance
import java.text.NumberFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ─── Gradientes para el workout ─────────────────────────────────────────────

// Fuego para la alta intensidad: rojo profundo → rojo vivo
private val RunGradient = Brush.verticalGradient(
    listOf(Color(0xFF8E0000), Color(0xFFB71C1C), Color(0xFFE53935)),
)
private val WalkGradient = Brush.verticalGradient(
    // Green500 arriba, como la pantalla de sesión completada: la menta se
    // percibía pálida y blancuzca en la mitad superior de la pantalla.
    listOf(Green500, Blue500),
)
private val PrepGradient = Brush.verticalGradient(
    listOf(Aqua500, Blue700),
)
private val CooldownGradient = Brush.verticalGradient(
    listOf(Aqua500, Green700),
)
// Trote: naranja → naranja profundo (entre caminata y carrera)
private val JogGradient = Brush.verticalGradient(
    listOf(Color(0xFFF97316), Color(0xFFD84315)),
)

// Si en la primera caminata el sensor registra menos pasos que esto, se asume
// que el celular está quieto (p. ej. apoyado en la caminadora) y se pregunta
// al usuario si activar el modo caminadora.
private const val TREADMILL_MIN_STEPS = 10

// Tope de la ventana de detección: pasado este tiempo de sesión ya no se pregunta.
private const val TREADMILL_ASK_MAX_MS = 10 * 60_000L

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
    val isJog = settings.hiitPhase == HiitPhase.JOG.name
    val isCooldown = settings.hiitPhase == HiitPhase.COOLDOWN.name
    val isPaused = settings.hiitPausedSeconds > 0
    // Plan de sesión personalizada (null = sesión clásica)
    val plan = settings.planSteps
    val phaseTotalSec = plan
        ?.getOrNull((settings.hiitRound - 1).coerceAtLeast(0))
        ?.seconds ?: when {
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

    // Detección de caminadora: si en la primera caminata el sensor no registra
    // casi pasos, el celular está quieto y se pregunta una vez si se activa el
    // modo caminadora (la distancia real se anota al terminar la sesión)
    var treadmillAsked by remember { mutableStateOf(false) }
    var treadmillDismissed by remember { mutableStateOf(false) }
    val repository = remember { SettingsRepository(context) }

    // En pausa el tiempo queda congelado: se muestra el guardado, no el del reloj
    val remainingMs = if (isPaused) {
        settings.hiitPausedSeconds * 1_000L
    } else {
        (settings.hiitPhaseEnd - nowMs).coerceAtLeast(0L)
    }
    val remainingSec = remainingMs / 1_000L

    // Comprobación periódica (cada segundo) con la sesión releída de DataStore:
    // los valores de la composición quedan obsoletos (de hecho la pantalla se
    // abre con hiitStartedAt aún en 0, antes de que start() lo escriba) y, con
    // el teléfono quieto, liveSteps no cambia nunca, así que un efecto keyed
    // en liveSteps jamás volvería a evaluarse.
    LaunchedEffect(Unit) {
        while (!treadmillAsked && !treadmillDismissed) {
            delay(1_000)
            val s = repository.settings.first()
            // Sin línea de base no hay sensor/permiso (no hay qué detectar) y
            // con el modo ya activo no hace falta preguntar
            if (s.hiitStepBaseline < 0 || s.hiitTreadmillMode) break
            if (s.hiitPausedSeconds > 0) continue
            // En sesiones personalizadas los tiempos no salen de hiitWalkSeconds:
            // la detección de caminadora queda descartada para no falsear.
            if (s.planSteps != null) break
            if (s.hiitPhase != HiitPhase.WALK.name || s.hiitRound > 1) continue
            // La detección solo tiene sentido al inicio de la sesión
            if (s.hiitStartedAt > 0 && System.currentTimeMillis() - s.hiitStartedAt > TREADMILL_ASK_MAX_MS) break
            val walkElapsedMs = s.hiitWalkSeconds * 1_000L - (s.hiitPhaseEnd - System.currentTimeMillis())
            if (walkElapsedMs in 20_000..60_000 && liveSteps < TREADMILL_MIN_STEPS) {
                treadmillAsked = true
            }
        }
    }
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
        isJog -> JogGradient
        isRun -> RunGradient
        isCooldown -> CooldownGradient
        else -> WalkGradient
    }

    // Colores del anillo
    val arcColorStart = when {
        isPrep -> Blue300
        isJog -> Color(0xFFFFB74D)
        isRun -> Color(0xFFEF5350)
        isCooldown -> Blue300
        else -> Mint300
    }
    val arcColorEnd = when {
        isPrep -> Blue500
        isJog -> Color(0xFFF57C00)
        isRun -> Color(0xFFB71C1C)
        isCooldown -> Green700
        else -> Mint500
    }
    val trackColor = Color.White.copy(alpha = 0.12f)
    val glowColor = when {
        isPrep -> Blue300.copy(alpha = 0.3f)
        isJog -> Color(0xFFFF9800).copy(alpha = 0.35f)
        isRun -> Color(0xFFE53935).copy(alpha = 0.4f)
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
            // Logo de la marca en la cabecera de la sesión
            BrandLogo()
            Spacer(modifier = Modifier.height(12.dp))

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
                        plan != null -> stringResource(
                            R.string.hiit_step_of,
                            settings.hiitRound,
                            plan.size,
                        )
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

            // Espacio fijo pequeño: el sobrante vertical queda abajo, entre el
            // anillo y los botones, para dar aire a la tarjeta de caminadora
            Spacer(modifier = Modifier.height(20.dp))

            // Fase con animación de cambio
            AnimatedContent(
                targetState = Triple(isPrep || isCooldown, isRun || isJog, isPaused),
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
                        isJog -> stringResource(R.string.hiit_phase_jog)
                        running -> stringResource(R.string.hiit_phase_run)
                        else -> stringResource(R.string.hiit_phase_walk)
                    },
                    fontSize = if (specialPhase && !paused) 23.sp else 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

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
                            isJog -> stringResource(R.string.hiit_sub_jog)
                            isRun -> stringResource(R.string.hiit_sub_run)
                            else -> stringResource(R.string.hiit_sub_walk)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (plan != null) {
                    stringResource(
                        R.string.hiit_step_time_of,
                        formatDuration(context, phaseTotalSec),
                        formatDuration(context, plan.sumOf { it.seconds }),
                    )
                } else {
                    "${formatDuration(context, settings.hiitWalkSeconds)}   ·   " +
                        formatDuration(context, settings.hiitRunSeconds)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
            )

            // Pasos y distancia de la sesión en vivo (solo con sensor; en
            // caminadora el contador no aporta nada y se oculta)
            if (settings.hiitStepBaseline >= 0 && !settings.hiitTreadmillMode) {
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

            // Pregunta de caminadora (una sola vez por sesión): si el sensor
            // no detectó movimiento en la primera caminata, se ofrece activar
            // el modo; la distancia real se anota al terminar
            if (treadmillAsked && !treadmillDismissed) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(16.dp),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.hiit_treadmill_ask),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    treadmillDismissed = true
                                    scope.launch { repository.setHiitTreadmillMode(true) }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Green700,
                                ),
                            ) {
                                Text(
                                    stringResource(R.string.common_yes),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Button(
                                onClick = { treadmillDismissed = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text(
                                    stringResource(R.string.common_no),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
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
    val scope = rememberCoroutineScope()
    val repository = remember { SettingsRepository(context) }

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
                        if (StepsValidator.hasSensor(context) || settings.hiitTreadmillMode) {
                            CompletedStatRow(
                                stringResource(R.string.hiit_stat_steps),
                                NumberFormat.getInstance().format(settings.hiitLastSteps),
                            )
                            CompletedStatRow(
                                stringResource(R.string.hiit_stat_distance),
                                formatDistance(settings.hiitLastDistanceM),
                            )
                        }
                        // Campo de distancia: visible con el modo caminadora
                        // activo o de forma semi-automática cuando el sensor no
                        // contó pasos en una sesión > 1 min (celular quieto,
                        // p. ej. apoyado en la caminadora)
                        val noStepsDetected = settings.hiitLastSteps == 0 && settings.hiitLastSeconds >= 60
                        if (settings.hiitTreadmillMode || noStepsDetected) {
                            TreadmillDistanceEditor(
                                initialMeters = settings.hiitLastDistanceM,
                                onSave = { meters ->
                                    scope.launch {
                                        repository.updateLastSessionStats(
                                            steps = HiitSession.stepsForDistance(meters),
                                            distanceMeters = meters,
                                        )
                                    }
                                },
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

/**
 * Campo para anotar la distancia que marca la caminadora al terminar la sesión.
 * Acepta km con coma o punto decimal; al guardar se derivan los pasos con la
 * zancada estándar de [HiitSession]. Diseñado sobre el cristal de la pantalla
 * de celebración (fondo oscuro, borde y texto blancos).
 */
@Composable
private fun TreadmillDistanceEditor(
    initialMeters: Int,
    onSave: (Int) -> Unit,
) {
    var text by remember {
        mutableStateOf(if (initialMeters > 0) "%.2f".format(initialMeters / 1000f) else "")
    }
    var saved by remember { mutableStateOf(initialMeters > 0) }
    val meters = text.replace(',', '.').toFloatOrNull()?.let { (it * 1000).toInt() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.hiit_treadmill_distance_label),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input.filter { it.isDigit() || it == ',' || it == '.' }
                saved = false
            },
            modifier = Modifier.width(104.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
            ),
            placeholder = {
                Text("0.00", color = Color.White.copy(alpha = 0.4f))
            },
            suffix = {
                Text("km", color = Color.White.copy(alpha = 0.7f))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
            ),
        )
        Spacer(modifier = Modifier.width(6.dp))
        TextButton(
            onClick = {
                meters?.let {
                    onSave(it)
                    saved = true
                }
            },
            enabled = meters != null && meters > 0 && !saved,
        ) {
            Text(
                stringResource(if (saved) R.string.common_saved_check else R.string.common_save),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    saved -> Mint300
                    else -> Color.White
                },
            )
        }
    }
}
