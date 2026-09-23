package com.example.hiit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hiit.R
import com.example.hiit.alarm.formatDuration
import com.example.hiit.data.AppSettings
import com.example.hiit.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Ajustes de la sesión HIIT: cada fila abre la hoja de ruedas y guarda al aceptar. */
@Composable
fun HiitConfigScreen(
    settings: AppSettings,
    repository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onBack: () -> Unit,
) {
    // Hoja abierta ahora mismo; null = ninguna
    var openSheet by remember { mutableStateOf<HiitSheet?>(null) }
    val context = LocalContext.current

    // Solo se pasa el parámetro que cambia; el resto se relee de los ajustes
    // ya guardados para no pisar con valores viejos un cambio recién hecho
    // desde otra hoja (el estado de la pantalla tarda un instante en refrescar).
    fun save(
        walk: Int? = null,
        run: Int? = null,
        rounds: Int? = null,
        warmup: Int? = null,
        cooldown: Int? = null,
    ) {
        scope.launch {
            val current = repository.settings.first()
            repository.setHiitParams(
                walk ?: current.hiitWalkSeconds,
                run ?: current.hiitRunSeconds,
                rounds ?: current.hiitRounds,
                warmup ?: current.hiitWarmupSeconds,
                cooldown ?: current.hiitCooldownSeconds,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        ConfigHeader(
            title = stringResource(R.string.hiit_title),
            subtitle = stringResource(R.string.hiit_config_subtitle),
            onBack = onBack,
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingRow(
                icon = Icons.Default.Timer,
                iconBackground = Blue500,
                title = stringResource(R.string.hiit_warmup),
                value = if (settings.hiitWarmupSeconds > 0) {
                    formatDuration(context, settings.hiitWarmupSeconds)
                } else {
                    stringResource(R.string.common_disabled)
                },
                onClick = { openSheet = HiitSheet.WARMUP },
            )
            SettingRow(
                icon = Icons.Default.Speed,
                iconBackground = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.hiit_high_intensity),
                value = formatDuration(context, settings.hiitRunSeconds),
                onClick = { openSheet = HiitSheet.RUN },
            )
            SettingRow(
                icon = Icons.AutoMirrored.Default.DirectionsWalk,
                iconBackground = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.hiit_low_intensity),
                value = formatDuration(context, settings.hiitWalkSeconds),
                onClick = { openSheet = HiitSheet.WALK },
            )
            SettingRow(
                icon = Icons.Default.Refresh,
                iconBackground = MaterialTheme.colorScheme.tertiary,
                title = stringResource(R.string.hiit_rounds),
                value = if (settings.hiitRounds <= 0) {
                    stringResource(R.string.hiit_rounds_infinite)
                } else {
                    stringResource(R.string.hiit_rounds_value, settings.hiitRounds)
                },
                onClick = { openSheet = HiitSheet.ROUNDS },
            )
            SettingRow(
                icon = Icons.Default.Favorite,
                iconBackground = Mint500,
                title = stringResource(R.string.hiit_cooldown),
                value = if (settings.hiitCooldownSeconds > 0) {
                    formatDuration(context, settings.hiitCooldownSeconds)
                } else {
                    stringResource(R.string.common_disabled)
                },
                onClick = { openSheet = HiitSheet.COOLDOWN },
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardSectionTitle(stringResource(R.string.hiit_session_duration_section))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (settings.hiitRounds <= 0) {
                                    stringResource(
                                        R.string.hiit_session_infinite,
                                        formatDuration(context, settings.hiitWalkSeconds),
                                        formatDuration(context, settings.hiitRunSeconds),
                                    )
                                } else {
                                    stringResource(
                                        R.string.hiit_session_format,
                                        formatDuration(context, settings.hiitWalkSeconds),
                                        formatDuration(context, settings.hiitRunSeconds),
                                        settings.hiitRounds,
                                    )
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(
                                    if (settings.hiitRounds <= 0) {
                                        R.string.hiit_total_infinite
                                    } else {
                                        R.string.hiit_total_format
                                    },
                                    formatDuration(context, settings.hiitTotalSeconds),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    ActivationSwitch(
                        title = stringResource(R.string.hiit_sounds_title),
                        subtitle = stringResource(R.string.hiit_sounds_subtitle),
                        checked = settings.hiitSounds,
                        onChange = { scope.launch { repository.setHiitSounds(it) } },
                    )
                    ActivationSwitch(
                        title = stringResource(R.string.hiit_voice_title),
                        subtitle = stringResource(R.string.hiit_voice_subtitle),
                        checked = settings.hiitVoice,
                        onChange = { scope.launch { repository.setHiitVoice(it) } },
                    )
                }
            }

            // Espacio para que el último elemento quede por encima de la barra
            // de navegación flotante al llegar al final del scroll
            Spacer(modifier = Modifier.navigationBarsPadding().height(104.dp))
        }
    }

    // ── Hojas de ruedas ──────────────────────────────────────────────────────
    when (openSheet) {
        HiitSheet.WARMUP -> DurationPickerSheet(
            title = stringResource(R.string.hiit_warmup),
            initialSeconds = settings.hiitWarmupSeconds,
            maxMinutes = 30,
            onConfirm = {
                save(warmup = it)
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )
        HiitSheet.RUN -> DurationPickerSheet(
            title = stringResource(R.string.hiit_high_intensity),
            initialSeconds = settings.hiitRunSeconds,
            maxMinutes = 30,
            onConfirm = {
                save(run = it)
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )
        HiitSheet.WALK -> DurationPickerSheet(
            title = stringResource(R.string.hiit_low_intensity),
            initialSeconds = settings.hiitWalkSeconds,
            maxMinutes = 30,
            onConfirm = {
                save(walk = it)
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )
        HiitSheet.ROUNDS -> RoundsPickerSheet(
            title = stringResource(R.string.hiit_rounds),
            initialRounds = settings.hiitRounds,
            onConfirm = {
                save(rounds = it)
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )
        HiitSheet.COOLDOWN -> DurationPickerSheet(
            title = stringResource(R.string.hiit_cooldown),
            initialSeconds = settings.hiitCooldownSeconds,
            maxMinutes = 30,
            onConfirm = {
                save(cooldown = it)
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )
        null -> Unit
    }
}

private enum class HiitSheet { WARMUP, RUN, WALK, ROUNDS, COOLDOWN }
