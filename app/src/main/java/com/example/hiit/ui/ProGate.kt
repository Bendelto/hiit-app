package com.example.hiit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hiit.R
import com.example.hiit.data.IntervalIntensity

// ─── Intensidades de los intervalos personalizados ──────────────────────────

/** Color distintivo de cada nivel de intensidad (escala del esfuerzo). */
fun intensityColor(intensity: IntervalIntensity): Color = when (intensity) {
    IntervalIntensity.WALK -> Color(0xFF12B277)
    IntervalIntensity.JOG -> Color(0xFFF97316)
    IntervalIntensity.RUN -> Color(0xFFE53935)
}

/** Nombre visible de cada nivel de intensidad. */
@Composable
fun intensityLabel(intensity: IntervalIntensity): String = stringResource(
    when (intensity) {
        IntervalIntensity.WALK -> R.string.intensity_walk
        IntervalIntensity.JOG -> R.string.intensity_jog
        IntervalIntensity.RUN -> R.string.intensity_run
    },
)

// ─── Sello Pro ──────────────────────────────────────────────────────────────

// Toques seguidos necesarios para el desbloqueo oculto de pruebas (no hay
// billing todavía; esto se quita cuando llegue la compra real).
private const val PRO_UNLOCK_TAPS = 5

/** Cuenta toques seguidos y dispara [onUnlock] al llegar al tope. */
@Composable
private fun rememberSecretTap(onUnlock: () -> Unit): () -> Unit {
    var taps by remember { mutableStateOf(0) }
    return {
        taps++
        if (taps >= PRO_UNLOCK_TAPS) {
            taps = 0
            onUnlock()
        }
    }
}

/** Sello dorado pequeño para marcar funciones exclusivas de la versión Pro. */
@Composable
fun ProBadge(onSecretTaps: (() -> Unit)? = null) {
    val tapHandler = onSecretTaps?.let { rememberSecretTap(it) }
    Box(
        modifier = Modifier
            .then(
                if (tapHandler != null) {
                    Modifier.clickable(onClick = tapHandler)
                } else {
                    Modifier
                },
            )
            .border(
                1.dp,
                Color(0xFFFFD54F).copy(alpha = 0.6f),
                RoundedCornerShape(50),
            )
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFB8860B), Color(0xFFFFD54F)),
                ),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                stringResource(R.string.pro_badge),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

/**
 * Diálogo informativo del candado Pro. La app aún no tiene compras integradas:
 * el desbloqueo real llegará con el billing; mientras tanto se explica que la
 * función es exclusiva de la versión Pro. [onUnlock] se dispara con el gesto
 * oculto de pruebas (5 toques sobre el candado).
 */
@Composable
fun ProLockedDialog(onDismiss: () -> Unit, onUnlock: () -> Unit = {}) {
    val tapHandler = rememberSecretTap(onUnlock)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = tapHandler)
                    .background(
                        Color(0xFFFFD54F).copy(alpha = 0.15f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFB8860B),
                )
            }
        },
        title = {
            Text(
                stringResource(R.string.pro_dialog_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.pro_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProBadge()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.pro_dialog_button),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}
