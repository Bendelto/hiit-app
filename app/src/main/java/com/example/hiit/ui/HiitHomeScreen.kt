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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hiit.R
import com.example.hiit.alarm.HiitSession
import com.example.hiit.alarm.formatDuration
import com.example.hiit.data.AppSettings
import kotlinx.coroutines.launch

// ─── Gradiente de la pestaña HIIT ───────────────────────────────────────────

// Verde de marca que desciende a verde profundo y cierra en turquesa oscuro:
// el mismo lenguaje cromático con el que arranca la sesión (fase de
// preparación) para que la pantalla principal anuncie el color del entrenamiento.
private val HiitHomeGradient = Brush.verticalGradient(
    listOf(Green500, Green700, Aqua700),
)

private val GreenAccent = Green300

// Degradado del botón de inicio: verde de marca → menta, el acento más
// luminoso de la paleta para que la acción principal destaque sobre el fondo.
private val StartButtonGradient = Brush.horizontalGradient(
    listOf(Green500, Mint400),
)

/** Columna de dato dentro de la tarjeta de sesión: icono, etiqueta y valor. */
@Composable
private fun StatColumn(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = White70,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Línea de calentamiento/enfriamiento: icono teñido + etiqueta y valor. */
@Composable
private fun WarmCoolLine(
    icon: ImageVector,
    iconTint: Color,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = White70,
        )
    }
}

/** Separador vertical entre columnas de datos. */
@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color.White.copy(alpha = 0.12f)),
    )
}

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
        FloatingOrbs(accent = GreenAccent)

        // Pantalla fija, sin scroll: los espacios se compactan para caber
        // en cualquier altura y el sobrante lo absorbe el espaciador con peso
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Marca
            BrandLogo(logoHeight = 20.dp)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                stringResource(R.string.hiit_ready),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.hiit_ready_subtitle).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = White70,
                letterSpacing = 3.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta de cristal con el resumen de la sesión
            GlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Encabezado centrado: rayo + etiqueta, sin caja
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Mint300,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.hiit_your_session).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = White70,
                            letterSpacing = 1.5.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Resumen grande: la alta intensidad va en menta para destacar.
                    // Las rondas viven en los cuadros de abajo para no saturar la línea.
                    val walkText = formatDuration(context, settings.hiitWalkSeconds)
                    val runText = formatDuration(context, settings.hiitRunSeconds)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            walkText,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                        Text(
                            "  →  ",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = White70,
                        )
                        Text(
                            runText,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Mint300,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        stringResource(R.string.hiit_summary_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = White70,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cuadros de datos: rondas y duración
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatColumn(
                            icon = Icons.Filled.Refresh,
                            label = stringResource(R.string.hiit_rounds_label),
                            value = if (settings.hiitRounds <= 0) {
                                "∞"
                            } else {
                                "${settings.hiitRounds}"
                            },
                            modifier = Modifier.weight(1f),
                        )
                        StatDivider()
                        StatColumn(
                            icon = Icons.Filled.Timer,
                            label = stringResource(R.string.hiit_total_label),
                            value = if (settings.hiitRounds <= 0) {
                                "∞"
                            } else {
                                formatDuration(context, settings.hiitTotalSeconds)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calentamiento y enfriamiento, una línea cada uno: los nombres
                    // son largos y no caben en cuadros estrechos
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        WarmCoolLine(
                            icon = Icons.Filled.Whatshot,
                            iconTint = Color(0xFFFF8A65),
                            text = stringResource(R.string.hiit_warmup) + " · " + (
                                if (settings.hiitWarmupSeconds > 0) {
                                    formatDuration(context, settings.hiitWarmupSeconds)
                                } else {
                                    "OFF"
                                }
                                ),
                        )
                        WarmCoolLine(
                            icon = Icons.Filled.AcUnit,
                            iconTint = Color(0xFF81D4FA),
                            text = stringResource(R.string.hiit_cooldown) + " · " + (
                                if (settings.hiitCooldownSeconds > 0) {
                                    formatDuration(context, settings.hiitCooldownSeconds)
                                } else {
                                    "OFF"
                                }
                                ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Enlace a los tiempos de la sesión como píldora compacta
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.18f),
                        RoundedCornerShape(50),
                    )
                    .clickable(onClick = onConfig)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.hiit_adjust_session),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = White70,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(StartButtonGradient)
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.25f),
                        RoundedCornerShape(28.dp),
                    ),
            ) {
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
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Green900,
                    ),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.hiit_start), fontWeight = FontWeight.ExtraBold)
                }
            }

            if (settings.hiitWarmupSeconds > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.hiit_warmup_note, settings.hiitWarmupSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = White70,
                )
            }

            // Absorbe el sobrante de altura para que la pantalla quede fija
            Spacer(modifier = Modifier.weight(1f))

            // Espacio para que el último elemento quede por encima de la barra
            // de navegación flotante
            Spacer(modifier = Modifier.navigationBarsPadding().height(84.dp))
        }
    }
}
