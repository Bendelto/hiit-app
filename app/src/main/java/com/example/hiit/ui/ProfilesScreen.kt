package com.example.hiit.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hiit.R
import com.example.hiit.alarm.HiitSession
import com.example.hiit.alarm.formatDuration
import com.example.hiit.data.AppSettings
import com.example.hiit.data.IntervalProfile
import com.example.hiit.data.SettingsRepository
import com.example.hiit.data.parseProfiles
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Listado de perfiles de intervalos personalizados. Es una función Pro: sin
 * desbloqueo, las acciones muestran el diálogo informativo y la lista queda
 * atenuada. Iniciar un perfil dispara la sesión (la pantalla del cronómetro
 * aparece sola al activarse `hiitActive`).
 */
@Composable
fun ProfilesScreen(
    settings: AppSettings,
    repository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val context = LocalContext.current
    val profiles = remember(settings.hiitCustomProfilesJson) {
        parseProfiles(settings.hiitCustomProfilesJson)
    }
    val proUnlocked = settings.hiitProUnlocked

    var showProDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<IntervalProfile?>(null) }

    fun requirePro(action: () -> Unit) {
        if (proUnlocked) action() else showProDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        ConfigHeader(
            title = stringResource(R.string.profiles_title),
            subtitle = stringResource(R.string.profiles_subtitle),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .alpha(if (proUnlocked) 1f else 0.55f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (profiles.isEmpty()) {
                EmptyProfilesCard(locked = !proUnlocked)
            } else {
                profiles.forEach { profile ->
                    ProfileCard(
                        profile = profile,
                        locked = !proUnlocked,
                        isActive = settings.hiitActiveProfileId == profile.id,
                        onStart = {
                            requirePro {
                                scope.launch { HiitSession.startCustom(context, profile) }
                            }
                        },
                        onActivate = {
                            requirePro {
                                scope.launch {
                                    repository.setActiveProfile(profile.id)
                                    onBack()
                                }
                            }
                        },
                        onEdit = { requirePro { onEdit(profile.id) } },
                        onDelete = { requirePro { profileToDelete = profile } },
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { requirePro(onNew) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.profiles_new),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showProDialog) {
        ProLockedDialog(
            onDismiss = { showProDialog = false },
            onUnlock = {
                showProDialog = false
                scope.launch { repository.setProUnlocked(true) }
            },
        )
    }

    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text(stringResource(R.string.profiles_delete_title)) },
            text = {
                Text(stringResource(R.string.profiles_delete_message, profile.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = profile.id
                        profileToDelete = null
                        scope.launch {
                            val current = parseProfiles(
                                repository.settings.first().hiitCustomProfilesJson,
                            )
                            repository.saveCustomProfiles(current.filterNot { it.id == id })
                            // Si era el perfil activo de la pantalla principal,
                            // se deselecciona para volver a la sesión clásica.
                            if (id == settings.hiitActiveProfileId) {
                                repository.setActiveProfile("")
                            }
                        }
                    },
                ) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyProfilesCard(locked: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (locked) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
            Text(
                stringResource(R.string.profiles_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.profiles_empty_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: IntervalProfile,
    locked: Boolean,
    isActive: Boolean,
    onStart: () -> Unit,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        stringResource(
                            R.string.profiles_mode_line,
                            profileModeLabel(profile.mode),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (locked) {
                    ProBadge()
                } else {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.common_edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.common_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ProfileStatRow(
                        label = stringResource(R.string.profiles_stat_intervals),
                        value = profile.intervals.size.toString(),
                    )
                    ProfileStatRow(
                        label = stringResource(R.string.profiles_stat_reps),
                        value = stringResource(R.string.editor_reps_value, profile.repetitions),
                    )
                    ProfileStatRow(
                        label = stringResource(R.string.profiles_stat_total),
                        value = formatDuration(LocalContext.current, profile.totalSeconds),
                    )
                }
            }
            if (!locked) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.profiles_start),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    Button(
                        onClick = onActivate,
                        modifier = Modifier.weight(1f),
                        enabled = !isActive,
                        shape = RoundedCornerShape(14.dp),
                        colors = if (isActive) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                contentColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                disabledContentColor = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        },
                    ) {
                        Icon(
                            if (isActive) Icons.Default.Check else Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(
                                if (isActive) R.string.profiles_active else R.string.profiles_activate,
                            ),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

