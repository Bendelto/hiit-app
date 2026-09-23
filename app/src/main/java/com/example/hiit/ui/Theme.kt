package com.example.hiit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.hiit.R

// ─── Paleta "MoveInterval" ──────────────────────────────────────────────────
//
// Colores sacados del icono de la app (logo-mi.svg): verde de marca #12B277,
// azul profundo #004B7C y menta del reloj #5CFA9E. Superficies con tinte
// azulado en oscuro para acompañar al azul de marca.

// Marca: verde MoveInterval (base #12B277)
val Green50  = Color(0xFFE9FAF2)
val Green100 = Color(0xFFCCF4E2)
val Green200 = Color(0xFFA0EACB)
val Green300 = Color(0xFF6DDFB0)
val Green400 = Color(0xFF36CD91)
val Green500 = Color(0xFF12B277)
val Green600 = Color(0xFF0E9C68)
val Green700 = Color(0xFF0B7F55)
val Green800 = Color(0xFF096445)
val Green900 = Color(0xFF074A33)

// Secundario: azul profundo del degradado (base #004B7C)
val Blue100 = Color(0xFFCCE5F2)
val Blue300 = Color(0xFF6FA3C4)
val Blue500 = Color(0xFF004B7C)
val Blue700 = Color(0xFF003A61)
val Blue900 = Color(0xFF001F34)

// Terciario: menta del reloj (base #5CFA9E)
val Mint300 = Color(0xFF5CFA9E)
val Mint400 = Color(0xFF3BEA90)
val Mint500 = Color(0xFF17C173)
val Mint700 = Color(0xFF0D8A53)

// Agua marina / Océano (tonos vivos y deportivos afines a la marca, para HIIT)
val Aqua300  = Color(0xFF4DD0E1)
val Aqua500  = Color(0xFF009FB7) // Agua marina vibrante y oceánico
val Aqua700  = Color(0xFF007A94) // Agua marina profundo
val Ocean900 = Color(0xFF001726) // Azul abisal oscuro


// Superficies (tinte azulado)
val Slate50  = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate700 = Color(0xFF1E293B)
val Slate800 = Color(0xFF0F172A)
val Slate900 = Color(0xFF020617)

// Neutros
val Cool400 = Color(0xFF94A3B8)
val Cool500 = Color(0xFF64748B)
val Cool600 = Color(0xFF475569)

// Estado
val ErrorRed     = Color(0xFFEF4444)
val SuccessGreen = Green600

// ─── Gradientes de utilidad ─────────────────────────────────────────────────

// Degradado del icono: verde de marca → azul profundo
val PrimaryGradient = Brush.horizontalGradient(
    listOf(Green500, Blue500),
)
val PrimaryGradientVertical = Brush.verticalGradient(
    listOf(Green500, Blue700),
)
val AccentGradient = Brush.horizontalGradient(
    listOf(Mint300, Green500),
)
val MintGradient = Brush.horizontalGradient(
    listOf(Mint400, Mint500),
)
val AquaOceanGradient = Brush.verticalGradient(
    listOf(Aqua500, Blue700, Ocean900),
)
val SurfaceGradientLight = Brush.verticalGradient(
    listOf(Color(0xFFF6FBF8), Color(0xFFEDF4F0)),
)
val SurfaceGradientDark = Brush.verticalGradient(
    listOf(Color(0xFF0F1A17), Color(0xFF020807)),
)

// ─── Color Schemes ──────────────────────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary = Green400,
    onPrimary = Green900,
    primaryContainer = Green700,
    onPrimaryContainer = Green100,
    secondary = Blue300,
    onSecondary = Blue900,
    secondaryContainer = Blue700,
    onSecondaryContainer = Blue100,
    tertiary = Mint400,
    onTertiary = Color(0xFF06301E),
    background = Slate900,
    onBackground = Color(0xFFE2E8F0),
    surface = Slate800,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Slate700,
    onSurfaceVariant = Cool400,
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Slate900,
)

// ─── Tipografía ─────────────────────────────────────────────────────────────
//
// Poppins como única familia: cubre todos los pesos del sistema y mantiene
// una voz visual consistente en toda la app.

val PoppinsFamily = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_mediumitalic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_bolditalic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold),
    Font(R.font.poppins_black, FontWeight.Black),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.8.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ─── Theme ──────────────────────────────────────────────────────────────────

@Composable
fun HiitTheme(
    content: @Composable () -> Unit,
) {
    // El diseño es siempre oscuro: las pantallas inmersivas de la sesión HIIT
    // usan degradados oscuros y la barra de cristal no encaja sobre fondos
    // claros. Forzar el tema oscuro mantiene todo coherente.
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content,
    )
}
