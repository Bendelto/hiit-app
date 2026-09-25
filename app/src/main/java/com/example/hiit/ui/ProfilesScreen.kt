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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.hiit.data.ProfileMode
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (profiles.isEmpty()) {
                EmptyProfilesCard(locked = !proUnlocked)
            } else {
                profiles.forEach { profile ->
                    ProfileCard(
                        profile = profile,
                        locked = !proUnlocked,
                        onStart = {
                            requirePro {
                                scope.launch { HiitSession.startCustom(context, profile) }
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
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            modeLabel(profile.mode),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (locked) {
                    ProBadge()
                } else {
                    IconButton(onClick = onStart) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.profiles_start_desc),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileMetaChip(stringResource(R.string.profiles_intervals_count, profile.intervals.size))
                ProfileMetaChip(stringResource(R.string.profiles_reps_line, profile.repetitions))
                ProfileMetaChip(
                    stringResource(
                        R.string.profiles_total_line,
                        formatDuration(LocalContext.current, profile.totalSeconds),
                    ),
                )
            }
            if (!locked) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.common_edit))
                    }
                    TextButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.common_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun modeLabel(mode: ProfileMode): String = stringResource(
    when (mode) {
        ProfileMode.SEQUENCE -> R.string.profile_mode_sequence
        ProfileMode.LADDER -> R.string.profile_mode_ladder
        ProfileMode.RANDOM -> R.string.profile_mode_random
    },
)
