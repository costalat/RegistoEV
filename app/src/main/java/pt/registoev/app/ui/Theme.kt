package pt.registoev.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cores da Identidade Unificada
private val AppBlue = Color(0xFF2196F3)      // Azul Azure
private val AppGreen = Color(0xFF4CAF50)     // Verde Elétrico (estilo Splash)
private val AppBlack = Color(0xFF000000)     // Preto Puro
private val AppLeadGrey = Color(0xFF121212)  // Cinza Chumbo
private val AppSurface = Color(0xFF1E1E1E)   // Cinza para Cartões

private val DarkColorScheme = darkColorScheme(
    primary = AppBlue,
    secondary = AppGreen,
    tertiary = Color(0xFF00BCD4), // Ciano para gradientes
    background = AppBlack,
    surface = AppSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = AppBlue.copy(alpha = 0.2f),
    onPrimaryContainer = AppBlue
)

// Mantemos o LightColorScheme apenas por compatibilidade, mas o foco é o Dark
private val LightColorScheme = lightColorScheme(
    primary = AppBlue,
    secondary = AppGreen,
    background = Color.White,
    surface = Color(0xFFF5F5F5)
)

@Composable
fun RegistoEVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Forçamos o modo escuro para garantir a nova estética "Deep Dark"
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}