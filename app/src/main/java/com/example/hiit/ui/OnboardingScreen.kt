package com.example.hiit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import com.example.hiit.R

private data class OnboardingPage(
    val stat: String,
    val title: String,
    val body: String,
    val illustration: @Composable (Modifier) -> Unit,
)

/** Onboarding de primer uso: 3 pantallas de bienvenida al entrenamiento HIIT. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            stat = stringResource(R.string.onboarding_page1_stat),
            title = stringResource(R.string.onboarding_page1_title),
            body = stringResource(R.string.onboarding_page1_body),
            illustration = { m -> FrequencyIllustration(m) },
        ),
        OnboardingPage(
            stat = stringResource(R.string.onboarding_page2_stat),
            title = stringResource(R.string.onboarding_page2_title),
            body = stringResource(R.string.onboarding_page2_body),
            illustration = { m -> HeartIllustration(m) },
        ),
        OnboardingPage(
            stat = stringResource(R.string.onboarding_page3_stat),
            title = stringResource(R.string.onboarding_page3_title),
            body = stringResource(R.string.onboarding_page3_body),
            illustration = { m -> EnergyIllustration(m) },
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Botón "Saltar"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (!isLast) {
                TextButton(onClick = onFinish) {
                    Text(
                        stringResource(R.string.onboarding_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // Mantiene la altura de la fila estable al llegar a la última página
                TextButton(onClick = {}, enabled = false) { Text("") }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        // Indicador de puntos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                val dotWidth by animateDpAsState(
                    targetValue = if (selected) 24.dp else 8.dp,
                    animationSpec = tween(300),
                    label = "dotWidth",
                )
                val dotColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    },
                    animationSpec = tween(300),
                    label = "dotColor",
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(dotWidth)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dotColor),
                )
            }
        }

        // Botón principal
        Button(
            onClick = {
                if (isLast) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                if (isLast) {
                    stringResource(R.string.onboarding_start)
                } else {
                    stringResource(R.string.onboarding_next)
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        page.illustration(Modifier.size(200.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            page.stat,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Ilustraciones vectoriales ──────────────────────────────────────────────

/** Reloj de intervalos: anillo segmentado con el tramo activo en gradiente. */
@Composable
private fun FrequencyIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Canvas(modifier = modifier) {
        val stroke = 13.dp.toPx()
        val radius = (size.minDimension - stroke * 2) / 2
        val center = this.center
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        // 12 segmentos con separación: las interrupciones del día
        val segments = 12
        val gap = 7f
        val sweep = 360f / segments - gap
        for (i in 0 until segments) {
            drawArc(
                color = track,
                startAngle = -90f + i * (360f / segments),
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }

        // Tramo activo en gradiente + punto «caminando» al final
        drawArc(
            brush = Brush.sweepGradient(
                listOf(tertiary, primary, tertiary),
                center = center,
            ),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        val dotAngle = Math.toRadians((-90f + sweep).toDouble())
        val dot = Offset(
            center.x + radius * cos(dotAngle).toFloat(),
            center.y + radius * sin(dotAngle).toFloat(),
        )
        drawCircle(color = primary, radius = stroke * 0.95f, center = dot)
        drawCircle(color = onPrimary, radius = stroke * 0.35f, center = dot)

        // Manecillas de reloj al centro
        drawLine(
            color = primary.copy(alpha = 0.7f),
            start = center,
            end = Offset(center.x, center.y - radius * 0.42f),
            strokeWidth = stroke * 0.32f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = primary.copy(alpha = 0.7f),
            start = center,
            end = Offset(center.x + radius * 0.3f, center.y + radius * 0.12f),
            strokeWidth = stroke * 0.32f,
            cap = StrokeCap.Round,
        )
        drawCircle(color = primary, radius = stroke * 0.42f, center = center)
    }
}

/** Corazón con línea de pulso (ECG) atravesándolo. */
@Composable
private fun HeartIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val heart = Path().apply {
            moveTo(w * 0.5f, h * 0.84f)
            cubicTo(w * 0.08f, h * 0.58f, w * 0.06f, h * 0.26f, w * 0.28f, h * 0.2f)
            cubicTo(w * 0.42f, h * 0.16f, w * 0.5f, h * 0.26f, w * 0.5f, h * 0.36f)
            cubicTo(w * 0.5f, h * 0.26f, w * 0.58f, h * 0.16f, w * 0.72f, h * 0.2f)
            cubicTo(w * 0.94f, h * 0.26f, w * 0.92f, h * 0.58f, w * 0.5f, h * 0.84f)
            close()
        }
        drawPath(heart, color = primary.copy(alpha = 0.12f))
        drawPath(
            heart,
            color = primary,
            style = Stroke(
                width = 6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Línea de pulso
        val ecg = Path().apply {
            moveTo(w * 0.18f, h * 0.5f)
            lineTo(w * 0.36f, h * 0.5f)
            lineTo(w * 0.43f, h * 0.37f)
            lineTo(w * 0.51f, h * 0.63f)
            lineTo(w * 0.58f, h * 0.5f)
            lineTo(w * 0.82f, h * 0.5f)
        }
        drawPath(
            ecg,
            color = tertiary,
            style = Stroke(
                width = 5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawCircle(
            color = tertiary,
            radius = 5.dp.toPx(),
            center = Offset(w * 0.82f, h * 0.5f),
        )
    }
}

/** Batería cargándose con rayo: la energía que devuelve cada sesión. */
@Composable
private fun EnergyIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyLeft = w * 0.13f
        val bodyRight = w * 0.8f
        val bodyTop = h * 0.3f
        val bodyBottom = h * 0.7f
        val corner = 18.dp.toPx()
        val strokeW = 6.dp.toPx()

        // Borde de la batería
        drawRoundRect(
            color = primary.copy(alpha = 0.12f),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
        )
        drawRoundRect(
            color = primary,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = Stroke(strokeW),
        )
        // Terminal
        drawRoundRect(
            color = primary,
            topLeft = Offset(bodyRight, h * 0.42f),
            size = Size(w * 0.06f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        )

        // Carga al ~70% con gradiente
        val pad = strokeW + 5.dp.toPx()
        val fillWidth = (bodyRight - bodyLeft - pad * 2) * 0.7f
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(primary, tertiary)),
            topLeft = Offset(bodyLeft + pad, bodyTop + pad),
            size = Size(fillWidth, bodyBottom - bodyTop - pad * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner * 0.5f, corner * 0.5f),
        )

        // Rayo al centro
        val cx = (bodyLeft + bodyRight) / 2
        val boltTop = bodyTop + (bodyBottom - bodyTop) * 0.18f
        val boltBottom = bodyTop + (bodyBottom - bodyTop) * 0.82f
        val boltMid = (boltTop + boltBottom) / 2
        val boltHalf = w * 0.055f
        val bolt = Path().apply {
            moveTo(cx + boltHalf * 0.4f, boltTop)
            lineTo(cx - boltHalf, boltMid + (boltBottom - boltTop) * 0.06f)
            lineTo(cx - boltHalf * 0.12f, boltMid + (boltBottom - boltTop) * 0.06f)
            lineTo(cx - boltHalf * 0.4f, boltBottom)
            lineTo(cx + boltHalf, boltMid - (boltBottom - boltTop) * 0.06f)
            lineTo(cx + boltHalf * 0.12f, boltMid - (boltBottom - boltTop) * 0.06f)
            close()
        }
        drawPath(bolt, color = Color.White)
    }
}
