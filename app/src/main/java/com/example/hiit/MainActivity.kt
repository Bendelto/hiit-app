package com.example.hiit

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hiit.data.AppSettings
import com.example.hiit.data.SettingsRepository
import com.example.hiit.ui.Green500
import com.example.hiit.ui.HiitCompletedScreen
import com.example.hiit.ui.HiitConfigScreen
import com.example.hiit.ui.HiitHomeScreen
import com.example.hiit.ui.HiitWorkoutScreen
import com.example.hiit.ui.Mint300
import com.example.hiit.ui.OnboardingScreen
import com.example.hiit.ui.HiitTheme
import com.example.hiit.ui.PermissionWarningBanner
import com.example.hiit.ui.ProfileEditorScreen
import com.example.hiit.ui.ProfilesScreen
import com.example.hiit.ui.SettingsScreen
import com.example.hiit.ui.StatsScreen
import com.example.hiit.util.hasAllPermissions
import com.example.hiit.util.isExactAlarmMissing
import com.example.hiit.util.missingPermissions
import com.example.hiit.util.openAppSettings
import com.example.hiit.util.openExactAlarmSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Routes {
    const val STATS = "stats"
    const val HIIT = "hiit"
    const val HIIT_CONFIG = "hiit_config"
    const val CUSTOM_PROFILES = "custom_profiles"
    const val PROFILE_EDITOR = "profile_editor"
    const val SETTINGS = "settings"

    val rootRoutes = setOf(STATS, HIIT, SETTINGS)
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = SettingsRepository(applicationContext)

        setContent {
            HiitTheme {
                val settings by repository.settings.collectAsState(initial = null)
                val scope = rememberCoroutineScope()
                settings?.let {
                    if (!it.onboardingSeen) {
                        OnboardingScreen(
                            onFinish = { scope.launch { repository.setOnboardingSeen(true) } },
                        )
                    } else {
                        AppScaffold(settings = it, repository = repository)
                    }
                }
            }
        }
    }
}

@Composable
fun AppScaffold(settings: AppSettings, repository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    // Reloj global: detecta cuándo mostrar la pantalla de sesión completada
    // justo al terminar la sesión HIIT.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1_000)
        }
    }

    // ── Permisos en tiempo de ejecución ─────────────────────────────────
    // Se piden todos juntos al iniciar la app y, mientras falte alguno, se
    // muestra un aviso persistente con acceso directo a concederlos. El
    // estado se recalcula al volver de los diálogos/ajustes del sistema.
    var refreshTrigger by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshTrigger++ }

    var askedOnStart by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!askedOnStart) {
            askedOnStart = true
            val missing = missingPermissions(context)
            if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
        }
    }

    val missingPerms = remember(refreshTrigger) { missingPermissions(context) }
    val exactAlarmMissing = remember(refreshTrigger) { isExactAlarmMissing(context) }
    val anyPermMissing = remember(refreshTrigger) { !hasAllPermissions(context) }
    val activity = context as? Activity
    // Si el usuario marcó "no volver a preguntar", el sistema ya no muestra el
    // diálogo: en ese caso el botón del aviso lleva directo a los ajustes.
    val canAskAgain = missingPerms.any {
        activity?.shouldShowRequestPermissionRationale(it) == true
    }

    // Fundido al arrancar o terminar la sesión HIIT: sin él el cambio de
    // pantalla completa se percibe como un corte en seco.
    val mainScreen = when {
        settings.hiitActive -> 1
        settings.hiitLastCompleted > 0 &&
            nowMs - settings.hiitLastCompleted < 120_000 -> 2
        else -> 0
    }
    Crossfade(
        targetState = mainScreen,
        animationSpec = tween(350),
        label = "mainScreen",
    ) { screen ->
    when (screen) {
        1 ->
            HiitWorkoutScreen(settings, context, scope)

        2 ->
            HiitCompletedScreen(settings) {
                scope.launch { repository.clearHiitLastCompleted() }
            }

        else -> {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            val showBottomBar = currentRoute in Routes.rootRoutes

            // Box overlay: el contenido llena toda la pantalla (los degradados
            // llegan hasta el borde inferior) y la barra de cristal flota encima.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Contenido principal: ocupa toda la pantalla sin desplazamiento
                Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.HIIT,
                            modifier = Modifier.fillMaxSize(),
                            // Fundido por defecto (cambios de pestaña); las
                            // pantallas apiladas se deslizan desde la derecha.
                            enterTransition = { fadeIn(tween(220)) },
                            exitTransition = { fadeOut(tween(220)) },
                        ) {
                            composable(Routes.STATS) { StatsScreen(settings) }
                            composable(Routes.HIIT) {
                                HiitHomeScreen(
                                    settings = settings,
                                    scope = scope,
                                    onConfig = { navController.navigate(Routes.HIIT_CONFIG) },
                                )
                            }
                            composable(
                                Routes.HIIT_CONFIG,
                                enterTransition = {
                                    slideInHorizontally(tween(280)) { it / 4 } +
                                        fadeIn(tween(280))
                                },
                                popExitTransition = {
                                    slideOutHorizontally(tween(280)) { it / 4 } +
                                        fadeOut(tween(200))
                                },
                            ) {
                                HiitConfigScreen(
                                    settings = settings,
                                    repository = repository,
                                    scope = scope,
                                    onBack = { navController.popBackStack() },
                                    onCustomProfiles = {
                                        navController.navigate(Routes.CUSTOM_PROFILES)
                                    },
                                )
                            }
                            composable(
                                Routes.CUSTOM_PROFILES,
                                enterTransition = {
                                    slideInHorizontally(tween(280)) { it / 4 } +
                                        fadeIn(tween(280))
                                },
                                popExitTransition = {
                                    slideOutHorizontally(tween(280)) { it / 4 } +
                                        fadeOut(tween(200))
                                },
                            ) {
                                ProfilesScreen(
                                    settings = settings,
                                    repository = repository,
                                    scope = scope,
                                    onBack = { navController.popBackStack() },
                                    onNew = {
                                        navController.navigate(
                                            "${Routes.PROFILE_EDITOR}/new",
                                        )
                                    },
                                    onEdit = { id ->
                                        navController.navigate("${Routes.PROFILE_EDITOR}/$id")
                                    },
                                )
                            }
                            composable(
                                "${Routes.PROFILE_EDITOR}/{profileId}",
                                enterTransition = {
                                    slideInHorizontally(tween(280)) { it / 4 } +
                                        fadeIn(tween(280))
                                },
                                popExitTransition = {
                                    slideOutHorizontally(tween(280)) { it / 4 } +
                                        fadeOut(tween(200))
                                },
                            ) { entry ->
                                ProfileEditorScreen(
                                    profileId = entry.arguments?.getString("profileId")
                                        ?.takeIf { it != "new" },
                                    settings = settings,
                                    repository = repository,
                                    scope = scope,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(Routes.SETTINGS) {
                                SettingsScreen(
                                    settings = settings,
                                    repository = repository,
                                    scope = scope,
                                    onHiitConfig = {
                                        navController.navigate(Routes.HIIT_CONFIG)
                                    },
                                )
                            }
                        }

                        // Barra de navegación flotante sobre el contenido
                        if (showBottomBar) {
                            GlassNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }

                // Banner de permisos superpuesto: flota sobre el contenido sin
                // desplazar el fondo ni el layout de la pantalla.
                // Se muestra mientras falte CUALQUIER permiso (runtime o alarmas exactas).
                AnimatedVisibility(
                    visible = anyPermMissing,
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    PermissionWarningBanner(
                        actionLabel = stringResource(
                            if (canAskAgain) R.string.common_grant else R.string.perm_banner_settings,
                        ),
                        onAction = {
                            when {
                                // Aún hay permisos runtime que se pueden pedir con diálogo
                                canAskAgain -> {
                                    permissionLauncher.launch(missingPerms.toTypedArray())
                                }
                                // Ya se concedieron los runtime, solo falta alarmas exactas
                                missingPerms.isEmpty() && exactAlarmMissing -> {
                                    openExactAlarmSettings(context)
                                }
                                // El usuario marcó "no volver a preguntar": ajustes generales
                                else -> {
                                    openAppSettings(context)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Barra de navegación inferior flotante con efecto de cristal (glassmorphism):
 * fondo semitransparente que deja ver el color de la pantalla, borde luminoso
 * sutil y solo la pestaña activa muestra su etiqueta.
 */
@Composable
private fun GlassNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        Quad(
            Routes.STATS,
            stringResource(R.string.stats_title),
            Icons.Filled.Home,
            Icons.Outlined.Home,
        ),
        Quad(
            Routes.HIIT,
            stringResource(R.string.hiit_title),
            Icons.Filled.Favorite,
            Icons.Outlined.FavoriteBorder,
        ),
        Quad(
            Routes.SETTINGS,
            stringResource(R.string.settings_title),
            Icons.Filled.Settings,
            Icons.Outlined.Settings,
        ),
    )

    Box(modifier = modifier.fillMaxWidth()) {
        // Fundido inferior: el contenido que hace scroll se desvanece antes de
        // llegar a la barra, así nada se mezcla con los íconos (la pantalla
        // queda a oscuras debajo de la pastilla en vez de verse enrredado)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(148.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x00020807),
                            Color(0xA3020807),
                            Color(0xFF020807),
                        ),
                    ),
                ),
        )

        // Contenedor de la pastilla: respeta la barra de navegación del sistema
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Mint300.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.30f),
                )
                .background(
                    // Cristal oscuro semitransparente: deja pasar una pizca del
                    // color de la pantalla sin que el contenido del scroll se lea
                    Brush.verticalGradient(
                        listOf(
                            Color(0xB3223038),   // ~70% opacidad
                            Color(0xCC172128),   // ~80% opacidad
                        ),
                    ),
                    RoundedCornerShape(32.dp),
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                    RoundedCornerShape(32.dp),
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (route, label, selectedIcon, unselectedIcon) ->
                val selected = currentRoute == route
                val iconTint by animateColorAsState(
                    targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.50f),
                    animationSpec = tween(200),
                    label = "navIconTint",
                )
                val labelColor by animateColorAsState(
                    targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.45f),
                    animationSpec = tween(200),
                    label = "navLabelColor",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(route) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Pastilla luminosa degradada detrás del ícono activo
                        // (de tamaño fijo: al no seleccionarla es transparente,
                        // así el ícono nunca se desplaza de altura)
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 26.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    if (selected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Mint300.copy(alpha = 0.85f),
                                                Green500.copy(alpha = 0.85f),
                                            ),
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color.Transparent),
                                        )
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (selected) selectedIcon else unselectedIcon,
                                contentDescription = label,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        // Etiqueta siempre visible, pegada al ícono para que el
                        // conjunto quede compacto y centrado en la pastilla
                        Text(
                            label,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = labelColor,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
            }
        }
    }
}
