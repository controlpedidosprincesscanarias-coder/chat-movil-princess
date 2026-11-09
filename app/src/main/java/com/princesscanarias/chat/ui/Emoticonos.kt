// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.ui

/** Los mismos emoticonos y respuestas rápidas que el Organizador (app/ui/emoticonos.py). */
object Emoticonos {
    val lista = listOf(
        "👍", "👌", "✅", "🙏", "👏", "🙌", "💪", "👀",
        "😊", "😀", "😂", "😉", "😅", "🤔", "😮", "😢",
        "😡", "😴", "❤️", "🎉", "⭐", "🔥", "☕", "🍽️",
        "📦", "🚚", "📄", "📅", "⏰", "⚠️", "❌", "✔️",
    )
    val respuestas = listOf("👍 Ok", "👌 Perfecto", "👍 Me gusta", "🙏 Gracias", "✅ Hecho", "👀 Lo miro", "⏰ Ahora voy", "📞 Te llamo")

    /** Añade `texto` al final de lo escrito, con un espacio delante si hace falta. */
    fun anadir(actual: String, texto: String): String {
        val sep = if (actual.isEmpty() || actual.endsWith(" ")) "" else " "
        return "$actual$sep$texto "
    }
}
