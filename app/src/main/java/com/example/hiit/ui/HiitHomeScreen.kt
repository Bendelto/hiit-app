package com.example.hiit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hiit.R
import com.example.hiit.alarm.HiitSession
import com.example.hiit.alarm.formatDuration
import com.example.hiit.data.AppSettings
import kotlinx.coroutines.launch

// ─── Gradiente de la pestaña HIIT ───────────────────────────────────────────

// Agua marina vibrante que desciende a azul océano y noche profundo: afín a
// la paleta MoveInterval, pero vivo, deportivo y con personalidad propia.
private val HiitHomeGradient = AquaOceanGradient

private val MintAccent = Mint300

/** Pestaña HIIT: pantalla inmersiva con el resumen de la sesión y el botón de inicio. */
@Composable
fun HiitHomeScreen(
    settings: AppSettings,
    scope: kotlinx.coroutines.CoroutineScope,
    onConfig: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HiitHomeGradient),
    ) {
        // Orbes decorativos en deriva para profundidad visual
        FloatingOrbs(accent = MintAccent)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Línea de acento
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(MintAccent, Color.White.copy(alpha = 0.5f)),
                        ),
                    ),
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Marca
            BrandLogo()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.hiit_title),
                style = MaterialTheme.typography.bodySmall,
                color = White70,
                letterSpacing = 1.5.sp,
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                stringResource(R.string.hiit_ready),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tarjeta de cristal con el resumen de la sesión
            GlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(MintAccent, Color.White.copy(alpha = 0.5f)),
                                    ),
                                ),
                        )
                        Text(
                            stringResource(R.string.hiit_your_session).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = White70,
                            letterSpacing = 1.2.sp,
                        )
                    }
                    Text(
                        if (settings.hiitRounds <= 0) {
                            stringResource(
                                R.string.hiit_summary_infinite,
                                formatDuration(context, settings.hiitWalkSeconds),
                                formatDuration(context, settings.hiitRunSeconds),
                            )
                        } else {
                            stringResource(
                                R.string.hiit_summary_format,
                                formatDuration(context, settings.hiitWalkSeconds),
                                formatDuration(context, settings.hiitRunSeconds),
                                settings.hiitRounds,
                            )
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Text(
                        stringResource(R.string.hiit_summary_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = White70,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.hiit_warmup),
                                style = MaterialTheme.typography.labelMedium,
                                color = White70,
                                letterSpacing = 1.sp,
                            )
                            Text(
                                if (settings.hiitWarmupSeconds > 0) {
                                    formatDuration(context, settings.hiitWarmupSeconds)
                                } else {
                                    stringResource(R.string.common_disabled)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.hiit_cooldown),
                                style = MaterialTheme.typography.labelMedium,
                                color = White70,
                                letterSpacing = 1.sp,
                            )
                            Text(
                                if (settings.hiitCooldownSeconds > 0) {
                                    formatDuration(context, settings.hiitCooldownSeconds)
                                } else {
                                    stringResource(R.string.common_disabled)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                    Text(
                        stringResource(
                            if (settings.hiitRounds <= 0) {
                                R.string.hiit_total_infinite
                            } else {
                                R.string.hiit_total_duration
                            },
                            formatDuration(context, settings.hiitTotalSeconds),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = White70,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Enlace a los tiempos de la sesión (mismo lenguaje que
            // «Ajustar sesión» de esta pestaña)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onConfig)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = Color.White,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    stringResource(R.string.hiit_adjust_session),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = White70,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        HiitSession.start(
                            context,
                            settings.hiitWalkSeconds,
                            settings.hiitRunSeconds,
                            settings.hiitRounds,
                            settings.hiitWarmupSeconds,
                            settings.hiitCooldownSeconds,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp),
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.hiit_start), fontWeight = FontWeight.ExtraBold)
            }

            if (settings.hiitWarmupSeconds > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.hiit_warmup_note, settings.hiitWarmupSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = White70,
                )
            }

            // Espacio para que el último elemento quede por encima de la barra
            // de navegación flotante al llegar al final del scroll
            Spacer(modifier = Modifier.navigationBarsPadding().height(104.dp))
        }
    }
}
