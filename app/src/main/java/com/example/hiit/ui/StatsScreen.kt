package com.example.hiit.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hiit.R
import com.example.hiit.data.AppSettings
import com.example.hiit.data.dayKey
import com.example.hiit.data.streakDays
import java.util.Calendar

// ─── Pantalla de Estadísticas ───────────────────────────────────────────────

/** Progreso HIIT: racha de días entrenando, sesiones totales y gráfico semanal. */
@Composable
fun StatsScreen(settings: AppSettings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        AppHeader(
            title = stringResource(R.string.stats_title),
            subtitle = stringResource(R.string.stats_subtitle),
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Racha (compacta, horizontal) ────────────────────────────
            val streak = settings.streakDays()
            PremiumCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Icono de racha en círculo con gradiente
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            when {
                                streak >= 7 -> "⚡"
                                streak >= 3 -> "🔥"
                                streak >= 1 -> "✨"
                                else -> "💤"
                            },
                            fontSize = 22.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    // Texto de racha
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.stats_current_streak),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            when {
                                streak >= 7 -> stringResource(R.string.stats_streak_msg_7)
                                streak >= 3 -> stringResource(R.string.stats_streak_msg_3)
                                streak >= 1 -> stringResource(R.string.stats_streak_msg_1)
                                else -> stringResource(R.string.stats_streak_msg_0)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 16.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    // Número grande de racha
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$streak",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 36.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            if (streak == 1) {
                                stringResource(R.string.stats_streak_day_one)
                            } else {
                                stringResource(R.string.stats_streak_day_other)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Sesiones HIIT totales ───────────────────────────────────
            PremiumCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.stats_hiit_sessions),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.stats_hiit_sessions_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 16.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        settings.hiitTotalSessions.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 36.sp,
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // ── Gráfico semanal de sesiones HIIT ────────────────────────
            PremiumCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(stringResource(R.string.stats_last_7_days))
                    WeeklySessionsChart(settings)
                    Text(
                        stringResource(R.string.stats_sessions_per_day),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            // Espacio para que el último elemento quede por encima de la barra
            // de navegación flotante al llegar al final del scroll
            Spacer(modifier = Modifier.navigationBarsPadding().height(104.dp))
        }
    }
}

// ─── Gráfico de barras de sesiones de la semana ─────────────────────────────

private data class DayEntry(
    val label: String,
    val value: Int,
    val isToday: Boolean,
)

@Composable
private fun WeeklySessionsChart(settings: AppSettings) {
    // Iniciales de los días en el idioma de la app, de lunes a domingo
    val dayLabels = stringArrayResource(R.array.stats_day_labels)
    val days = (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        val label = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> dayLabels[0]
            Calendar.TUESDAY -> dayLabels[1]
            Calendar.WEDNESDAY -> dayLabels[2]
            Calendar.THURSDAY -> dayLabels[3]
            Calendar.FRIDAY -> dayLabels[4]
            Calendar.SATURDAY -> dayLabels[5]
            else -> dayLabels[6]
        }
        DayEntry(
            label = label,
            value = settings.hiitSessionsHistory[dayKey(daysAgo)] ?: 0,
            isToday = daysAgo == 0,
        )
    }
    val max = (days.maxOf { it.value }).coerceAtLeast(1)

    // Animación de entrada progresiva
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationTriggered = true }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEachIndexed { index, day ->
            val targetHeight = if (animationTriggered) {
                (6 + 76.0 * day.value / max).toFloat()
            } else {
                6f
            }
            val animatedHeight by animateFloatAsState(
                targetValue = targetHeight,
                animationSpec = tween(
                    durationMillis = 600,
                    delayMillis = index * 80,
                    easing = FastOutSlowInEasing,
                ),
                label = "barHeight$index",
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Valor encima de la barra
                Text(
                    if (day.value > 0) day.value.toString() else "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (day.isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                )
                // Barra con forma de píldora
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(animatedHeight.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (day.isToday) {
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                    ),
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    ),
                                )
                            },
                        ),
                )
                // Etiqueta del día
                Text(
                    day.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                    ),
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                )
            }
        }
    }
}
