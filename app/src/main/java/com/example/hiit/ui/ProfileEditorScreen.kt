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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hiit.R
import com.example.hiit.alarm.HiitPhase
import com.example.hiit.alarm.asIntensity
import com.example.hiit.alarm.formatDuration
import com.example.hiit.data.AppSettings
import com.example.hiit.data.CustomInterval
import com.example.hiit.data.IntervalIntensity
import com.example.hiit.data.IntervalProfile
import com.example.hiit.data.ProfileMode
import com.example.hiit.data.SettingsRepository
import com.example.hiit.data.buildPlan
import com.example.hiit.data.parseProfiles
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Tope de intervalos por perfil: suficiente para secuencias largas sin
// degradar el rendimiento del editor.
private const val MAX_INTERVALS = 30

// Duración mínima de un intervalo de esfuerzo (la sesión clásica usa 5 s).
private const val MIN_INTERVAL_SECONDS = 5

private sealed interface EditorSheet {
    data class IntervalDuration(val index: Int) : EditorSheet
    data class IntervalIntensity(val index: Int) : EditorSheet
    data object Repetitions : EditorSheet
    data object Warmup : EditorSheet
    data object Cooldown : EditorSheet
}

/**
 * Editor de un perfil de intervalos personalizado: nombre, lista de intervalos
 * (intensidad + duración propias), modo de ejecución, repeticiones de la
 * secuencia y calentamiento/enfriamiento. Guarda el perfil completo en
 * DataStore al pulsar «Guardar perfil».
 */
@Composable
fun ProfileEditorScreen(
    profileId: String?,
    settings: AppSettings,
    repository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val existing = remember(profileId) {
        profileId?.let { id ->
            parseProfiles(settings.hiitCustomProfilesJson).find { it.id == id }
        }
    }

    var name by remember(profileId) { mutableStateOf(existing?.name ?: "") }
    val intervals = remember(profileId) {
        (existing?.intervals ?: listOf(CustomInterval(IntervalIntensity.RUN, 60)))
            .toMutableStateList()
    }
    var mode by remember(profileId) { mutableStateOf(existing?.mode ?: ProfileMode.SEQUENCE) }
    var repetitions by remember(profileId) {
        mutableIntStateOf(existing?.repetitions ?: 1)
    }
    var warmupSeconds by remember(profileId) {
        mutableIntStateOf(existing?.warmupSeconds ?: 0)
    }
    var cooldownSeconds by remember(profileId) {
        mutableIntStateOf(existing?.cooldownSeconds ?: 120)
    }
    var openSheet by remember { mutableStateOf<EditorSheet?>(null) }
    var showNameError by remember { mutableStateOf(false) }

    val draft = IntervalProfile(
        id = existing?.id ?: "",
        name = name,
        intervals = intervals.toList(),
        mode = mode,
        repetitions = repetitions,
        warmupSeconds = warmupSeconds,
        cooldownSeconds = cooldownSeconds,
    )
    // Vista previa determinista: la sesión real baraja con otra semilla.
    val previewPlan = remember(draft) { draft.buildPlan(seed = 0) }
    val totalSeconds = warmupSeconds +
        repetitions * intervals.sumOf { it.seconds } +
        cooldownSeconds

    fun save() {
        if (name.isBlank()) {
            showNameError = true
            return
        }
        scope.launch {
            val current = parseProfiles(repository.settings.first().hiitCustomProfilesJson)
            val profile = draft.copy(id = existing?.id ?: UUID.randomUUID().toString())
            repository.saveCustomProfiles(
                current.filterNot { it.id == profile.id } + profile,
            )
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        ConfigHeader(
            title = stringResource(
                if (existing != null) R.string.editor_title_edit else R.string.editor_title_new,
            ),
            subtitle = stringResource(R.string.editor_subtitle),
            onBack = onBack,
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Nombre ────────────────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    showNameError = false
                },
                label = { Text(stringResource(R.string.editor_name_label)) },
                placeholder = { Text(stringResource(R.string.editor_name_placeholder)) },
                singleLine = true,
                isError = showNameError,
                supportingText = if (showNameError) {
                    { Text(stringResource(R.string.editor_error_name)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )

            // ── Intervalos ────────────────────────────────────────────
            SectionTitle(stringResource(R.string.editor_intervals))

            intervals.forEachIndexed { index, interval ->
                IntervalRow(
                    position = index + 1,
                    interval = interval,
                    canMoveUp = index > 0,
                    canMoveDown = index < intervals.lastIndex,
                    onIntensityClick = { openSheet = EditorSheet.IntervalIntensity(index) },
                    onDurationClick = { openSheet = EditorSheet.IntervalDuration(index) },
                    onMoveUp = {
                        intervals.add(index - 1, intervals.removeAt(index))
                    },
                    onMoveDown = {
                        intervals.add(index + 1, intervals.removeAt(index))
                    },
                    onDelete = { intervals.removeAt(index) },
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = intervals.size < MAX_INTERVALS) {
                        intervals.add(CustomInterval(IntervalIntensity.RUN, 60))
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.editor_add_interval),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // ── Modo de ejecución ─────────────────────────────────────
            SectionTitle(stringResource(R.string.editor_mode))
            ProfileMode.entries.forEach { option ->
                ModeOptionRow(
                    mode = option,
                    selected = mode == option,
                    onClick = { mode = option },
                )
            }

            // ── Repeticiones, calentamiento y enfriamiento ────────────
            SectionTitle(stringResource(R.string.editor_session))
            EditorValueRow(
                label = stringResource(R.string.editor_reps),
                value = stringResource(R.string.editor_reps_value, repetitions),
                onClick = { openSheet = EditorSheet.Repetitions },
            )
            EditorValueRow(
                label = stringResource(R.string.editor_warmup),
                value = if (warmupSeconds > 0) {
                    formatDuration(context, warmupSeconds)
                } else {
                    stringResource(R.string.common_disabled)
                },
                onClick = { openSheet = EditorSheet.Warmup },
            )
            EditorValueRow(
                label = stringResource(R.string.editor_cooldown),
                value = if (cooldownSeconds > 0) {
                    formatDuration(context, cooldownSeconds)
                } else {
                    stringResource(R.string.common_disabled)
                },
                onClick = { openSheet = EditorSheet.Cooldown },
            )

            // ── Vista previa y total ──────────────────────────────────
            SectionTitle(stringResource(R.string.editor_preview))
            Text(
                stringResource(R.string.editor_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PlanPreviewChips(
                previewPlan.map { step ->
                    val stepName = when (step.phase) {
                        HiitPhase.PREP -> stringResource(R.string.editor_preview_warmup)
                        HiitPhase.COOLDOWN -> stringResource(R.string.editor_preview_cooldown)
                        else -> intensityLabel(
                            step.phase.asIntensity() ?: IntervalIntensity.RUN,
                        )
                    }
                    "$stepName ${formatDuration(context, step.seconds)}"
                },
            )
            EditorValueRow(
                label = stringResource(R.string.editor_total),
                value = formatDuration(context, totalSeconds),
                onClick = null,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = ::save,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                stringResource(R.string.editor_save),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    // ── Hojas de selección ─────────────────────────────────────────────
    when (val sheet = openSheet) {
        is EditorSheet.IntervalDuration -> DurationPickerSheet(
            title = stringResource(
                R.string.editor_interval_duration_title,
                sheet.index + 1,
            ),
            initialSeconds = intervals[sheet.index].seconds,
            maxMinutes = 60,
            onConfirm = { seconds ->
                intervals[sheet.index] = intervals[sheet.index].copy(
                    seconds = seconds.coerceAtLeast(MIN_INTERVAL_SECONDS),
                )
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )
        is EditorSheet.IntervalIntensity -> IntensityPickerSheet(
            title = stringResource(
                R.string.editor_interval_intensity_title,
                sheet.index + 1,
            ),
            selected = intervals[sheet.index].intensity,
            onConfirm = { intensity ->
                intervals[sheet.index] = intervals[sheet.index].copy(intensity = intensity)
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )
        EditorSheet.Repetitions -> NumberPickerSheet(
            title = stringResource(R.string.editor_reps),
            initial = repetitions,
            range = 1..50,
            unit = stringResource(R.string.editor_reps_unit),
            onConfirm = {
                repetitions = it
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )
        EditorSheet.Warmup -> DurationPickerSheet(
            title = stringResource(R.string.editor_warmup),
            initialSeconds = warmupSeconds,
            maxMinutes = 60,
            onConfirm = {
                warmupSeconds = it
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )
        EditorSheet.Cooldown -> DurationPickerSheet(
            title = stringResource(R.string.editor_cooldown),
            initialSeconds = cooldownSeconds,
            maxMinutes = 60,
            onConfirm = {
                cooldownSeconds = it
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )
        null -> Unit
    }
}

// ─── Fila de intervalo ──────────────────────────────────────────────────────

@Composable
private fun IntervalRow(
    position: Int,
    interval: CustomInterval,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onIntensityClick: () -> Unit,
    onDurationClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = intensityColor(interval.intensity)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$position.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Intensidad: punto de color + nombre; abre el selector
                Surface(
                    shape = RoundedCornerShape(50),
                    color = color.copy(alpha = 0.14f),
                    modifier = Modifier.clickable(onClick = onIntensityClick),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            intensityLabel(interval.intensity),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = color,
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                // Duración propia del intervalo
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.clickable(onClick = onDurationClick),
                ) {
                    Text(
                        formatDuration(LocalContext.current, interval.seconds),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

// ─── Fila de valor genérica (repeticiones, calentamiento, etc.) ─────────────

@Composable
private fun EditorValueRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
) {
    val rowModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth().then(rowModifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ─── Opción de modo de ejecución ────────────────────────────────────────────

@Composable
private fun ModeOptionRow(
    mode: ProfileMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                },
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(
                        when (mode) {
                            ProfileMode.SEQUENCE -> R.string.profile_mode_sequence
                            ProfileMode.LADDER -> R.string.profile_mode_ladder
                            ProfileMode.RANDOM -> R.string.profile_mode_random
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    stringResource(
                        when (mode) {
                            ProfileMode.SEQUENCE -> R.string.profile_mode_sequence_desc
                            ProfileMode.LADDER -> R.string.profile_mode_ladder_desc
                            ProfileMode.RANDOM -> R.string.profile_mode_random_desc
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ─── Vista previa del orden de la sesión ────────────────────────────────────

@Composable
private fun PlanPreviewChips(labels: List<String>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        itemsIndexed(labels) { index, label ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ) {
                Text(
                    "${index + 1} · $label",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

// ─── Selector de intensidad de un intervalo ─────────────────────────────────

/** Hoja inferior con los 4 niveles de intensidad para un intervalo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntensityPickerSheet(
    title: String,
    selected: IntervalIntensity,
    onConfirm: (IntervalIntensity) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(16.dp))
            IntervalIntensity.entries.forEach { intensity ->
                val color = intensityColor(intensity)
                val isSelected = intensity == selected
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        color.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onConfirm(intensity) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            intensityLabel(intensity),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = color,
                            )
                        }
                    }
                }
            }
        }
    }
}
