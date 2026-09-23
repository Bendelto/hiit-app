package com.example.hiit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

// Botón ACEPTAR naranja, como en las capturas de referencia
private val AcceptOrange = Color(0xFFF97316)

/**
 * Selector tipo spinner: una caja con el valor actual y una flecha; al tocarla
 * se despliega un menú con las opciones. Sin deslizar ni teclear.
 */
@Composable
private fun SpinnerOption(
    range: IntRange,
    selected: Int,
    onSelected: (Int) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    formatValue: (Int) -> String = { it.toString().padStart(2, '0') },
) {
    val options = remember(range) { range.toList() }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                    1.dp,
                    if (expanded) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    },
                ),
                modifier = Modifier.clickable { expanded = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatValue(selected),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.common_choose_unit, unit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    val isSelected = option == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                formatValue(option),
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            unit,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )
    }
}

/** Encabezado de la hoja: título centrado + botón de cierre explícito. */
@Composable
private fun SheetHeader(title: String, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center),
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

/**
 * Hoja inferior con selectores de minutos y segundos para elegir una duración.
 * Al pulsar ACEPTAR se confirma el valor en segundos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationPickerSheet(
    title: String,
    initialSeconds: Int,
    maxMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var minutes by remember { mutableIntStateOf((initialSeconds / 60).coerceIn(0, maxMinutes)) }
    var seconds by remember { mutableIntStateOf((initialSeconds % 60).coerceIn(0, 59)) }

    // Sin tirador de arrastre: la hoja no se cierra deslizando, solo con la X,
    // tap fuera o el botón atrás.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SheetHeader(title, onDismiss)

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpinnerOption(
                    range = 0..maxMinutes,
                    selected = minutes,
                    onSelected = { minutes = it },
                    unit = stringResource(R.string.unit_min),
                )
                Text(
                    ":",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SpinnerOption(
                    range = 0..59,
                    selected = seconds,
                    onSelected = { seconds = it },
                    unit = stringResource(R.string.unit_s),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onConfirm(minutes * 60 + seconds) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcceptOrange,
                    contentColor = Color.White,
                ),
            ) {
                Text(stringResource(R.string.common_accept), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

/**
 * Hoja inferior para elegir las rondas de la sesión HIIT: número fijo (1–50)
 * o infinitas (0, «hasta agotarme»), en cuyo caso la sesión solo termina
 * cuando el usuario la detiene.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundsPickerSheet(
    title: String,
    initialRounds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var infinite by remember { mutableStateOf(initialRounds <= 0) }
    var rounds by remember { mutableIntStateOf(if (initialRounds <= 0) 8 else initialRounds) }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SheetHeader(title, onDismiss)

            // Selector de modo: número fijo o hasta agotarme
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false, true).forEach { modeInfinite ->
                    val selected = infinite == modeInfinite
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                        ),
                        modifier = Modifier.clickable { infinite = modeInfinite },
                    ) {
                        Text(
                            stringResource(
                                if (modeInfinite) {
                                    R.string.hiit_rounds_infinite
                                } else {
                                    R.string.hiit_rounds_fixed
                                },
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!infinite) {
                SpinnerOption(
                    range = 1..50,
                    selected = rounds,
                    onSelected = { rounds = it },
                    unit = stringResource(R.string.unit_rounds),
                )
            } else {
                Text(
                    stringResource(R.string.hiit_infinite_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onConfirm(if (infinite) 0 else rounds) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcceptOrange,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    stringResource(R.string.common_accept),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

/**
 * Hoja inferior con un solo selector para elegir un número entero
 * (repeticiones, horas del día, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberPickerSheet(
    title: String,
    initial: Int,
    range: IntRange,
    unit: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableIntStateOf(initial.coerceIn(range.first, range.last)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SheetHeader(title, onDismiss)

            SpinnerOption(
                range = range,
                selected = value,
                onSelected = { value = it },
                unit = unit,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onConfirm(value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcceptOrange,
                    contentColor = Color.White,
                ),
            ) {
                Text(stringResource(R.string.common_accept), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

