package com.example.hiit.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.WorkspacePremium
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
    IntervalIntensity.JOG -> Color(0xFFFFC400)
    IntervalIntensity.RUN -> Color(0xFFE64A19)
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

/** Sello premium compacto para marcar funciones exclusivas de la versión Pro.
 *  Diseño: pastilla monocroma plana (claro sobre oscuro, colores inversos del
 *  tema), sin degradados, bordes brillantes ni sombras: minimalista y moderno. */
@Composable
fun ProBadge(onSecretTaps: (() -> Unit)? = null) {
    val tapHandler = onSecretTaps?.let { rememberSecretTap(it) }
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.inverseSurface, shape)
            .then(
                if (tapHandler != null) {
                    Modifier.clickable(onClick = tapHandler)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.size(11.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                stringResource(R.string.pro_badge),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                softWrap = false,
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
                        MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.12f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseSurface,
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
