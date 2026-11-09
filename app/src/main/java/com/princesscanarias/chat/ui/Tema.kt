// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colores del chat del Organizador (ui/chat_popup.py) y del resto de las apps de Compras. */
object Colores {
    val fondo = Color(0xFF1B2735)
    val cabecera = Color(0xFF134E6F)
    val propio = Color(0xFF1F8F63)
    val ajeno = Color(0xFF3A4A63)
    val texto = Color(0xFFE8EDF2)
    val meta = Color(0xFF8FA3B8)
    val caja = Color(0xFF25313F)
    val dorado = Color(0xFFB8913A)
    val navy = Color(0xFF0F1F3D)
    val error = Color(0xFFE74C3C)
}

@Composable
fun TemaChat(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Colores.propio, onPrimary = Color.White,
            secondary = Colores.dorado, background = Colores.fondo, surface = Colores.fondo,
            surfaceVariant = Colores.caja, onBackground = Colores.texto, onSurface = Colores.texto,
            onSurfaceVariant = Colores.meta, error = Colores.error,
        ),
        content = contenido,
    )
}
