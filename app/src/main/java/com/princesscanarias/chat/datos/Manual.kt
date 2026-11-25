// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

import java.text.Normalizer

/**
 * (v1.0.18, petición de Víctor) Manual de uso dentro de la app: Ajustes → 📖 Manual de uso. Sin conexión, con
 * buscador, y al final el enlace al manual completo en PDF (el de usuario del Organizador, capítulos 7 y 8).
 * El texto sigue a ese manual; si cambia una pantalla de la app, cambiar también este texto.
 * Lo que no depende de Android (contenido y búsqueda) se prueba en PruebasLogica.
 */
object Manual {
    /** El manual de usuario del Organizador (repositorio público): el chat del móvil es su capítulo 8. */
    const val URL_PDF = "https://github.com/controlpedidosprincesscanarias-coder/organizador-princess/raw/main/" +
        "Manual_Usuario_OrganizadorPrincess.pdf"

    data class Apartado(val icono: String, val titulo: String, val lineas: List<String>)

    val apartados = listOf(
        Apartado("👋", "Qué es esta app", listOf(
            "Chat Dpto. Compras Princess es el chat del departamento en el móvil: las mismas conversaciones que en el Organizador de Windows, con la misma cuenta de Control de Pedidos.",
            "Puedes estar conectado a la vez en el Organizador y en el móvil: los mensajes llegan a los dos, también los que escribes tú desde el otro.",
            "Para entrar, usa tu usuario y tu contraseña de Control de Pedidos.",
        )),
        Apartado("💬", "Conversaciones y grupos", listOf(
            "La lista muestra el chat General, tus chats privados y los grupos. El número junto al nombre son los mensajes sin leer.",
            "Arriba, siempre fijos, el General y «Control Pedidos». Debajo, las demás conversaciones: la última que has usado (con un mensaje enviado o recibido, o al abrirla) va la primera.",
            "«Nueva conversación» (abajo): elige un compañero para un chat privado, o varios para crear un grupo; te pide un nombre para el grupo.",
            "En un grupo, pulsa su nombre arriba para ver quién está. Quien lo creó puede añadir y quitar compañeros, renombrarlo o eliminarlo; cualquiera puede salir del grupo.",
            "El botón de actualizar (arriba) vuelve a cargar la lista.",
        )),
        Apartado("✍️", "Escribir y emoticonos", listOf(
            "Escribe en la caja de abajo y pulsa el botón de enviar.",
            "😊 abre los emoticonos y las respuestas rápidas («👍 Ok», «🙏 Gracias»…): se añaden a lo que estás escribiendo.",
        )),
        Apartado("📎", "Fotos y documentos", listOf(
            "📎 → «Hacer una foto», «Foto de la galería» o «Documento». Antes de enviar se ve lo que vas a mandar; hasta 10 MB por archivo.",
            "Un toque en una foto o un documento recibido lo abre. Manteniéndolo pulsado: «Abrir», «Guardar en…» (elige la carpeta) o «Compartir» con otra app.",
            "Por seguridad, las apps de Android (.apk) no se pueden enviar ni abrir desde el chat. Instala las apps solo desde su página oficial.",
        )),
        Apartado("🎬", "Enlaces y vídeos", listOf(
            "Los enlaces de los mensajes salen subrayados: púlsalos y se abren en su app (TikTok, YouTube…) o en el navegador.",
            "Bajo un enlace de TikTok, YouTube o Instagram sale una tarjeta con la imagen del vídeo, el título y el autor. Púlsala para verlo.",
            "Los vídeos enviados como archivo (📎 → «Documento») salen con ▶: púlsalo y se ven aquí mismo, a pantalla completa; ✕ lo cierra. Nunca empiezan solos. Hasta 10 MB por archivo.",
        )),
        Apartado("🎤", "Mensajes de voz", listOf(
            "Con la caja de escribir vacía, a su derecha aparece el 🎤 (si escribes algo, cambia por el botón de enviar).",
            "Mantén pulsado el 🎤 y habla: se ve 🔴 y el tiempo que llevas. Suelta para enviarlo.",
            "Para cancelar, desliza el dedo a la izquierda antes de soltar: cuando sale «Suelta para cancelar», suelta y no se envía nada.",
            "Como mucho 3 minutos: al llegar, se envía solo. La primera vez, Android pide permiso para usar el micrófono: pulsa «Permitir».",
            "Para escucharlo, pulsa ▶ en la burbuja; ⏸ lo para. En el Organizador también se escuchan, con un clic.",
        )),
        Apartado("↩️", "Responder, reenviar, copiar y eliminar", listOf(
            "Mantén pulsado un mensaje para ver lo que puedes hacer con él:",
            "↩ Responder: tu respuesta lleva arriba el mensaje citado.",
            "↪ Reenviar…: elige la conversación o el compañero. Las fotos y archivos no se vuelven a subir.",
            "📋 Copiar (textos) y 📤 Compartir con otra app.",
            "🗑 Eliminar (solo los tuyos): queda «🚫 Mensaje eliminado» para todos.",
        )),
        Apartado("🧹", "Vaciar o eliminar un chat", listOf(
            "Menú ⋮ de la conversación (arriba a la derecha), o mantener pulsado el chat en la lista.",
            "🧹 Vaciar chat: desaparecen los mensajes que hay ahora, solo para ti, en el móvil y en el Organizador. Los demás siguen viendo los suyos.",
            "🗑 Eliminar chat (solo privados): sale de tu lista con sus mensajes. Vuelve a aparecer si esa persona te escribe.",
        )),
        Apartado("⏸", "Compañeros desactivados o que ya no existen", listOf(
            "«⏸ Desactivado»: está desactivado en Control de Pedidos. Si le escribes, el mensaje no se envía y el texto vuelve a la caja de escribir.",
            "«⛔ Ya no existe»: se eliminó su usuario. Al escribirle te lo dice y te ofrece eliminar el chat.",
        )),
        Apartado("🔔", "Avisos de Control Pedidos", listOf(
            "En la conversación «Control Pedidos» te llegan tus avisos (pedidos sin entregar, cambios de estado, techo de gastos…), con su notificación.",
            "En ella no se puede escribir. Un aviso se puede reenviar a un compañero (mantener pulsado → «↪ Reenviar…»).",
            "Qué avisos recibe cada persona se decide en Control de Pedidos (Configuración de avisos).",
        )),
        Apartado("🔔", "Sonidos de los mensajes", listOf(
            "Ajustes → Sonidos de los mensajes → «Cambiar»: el sonido general, con el selector de tonos del móvil.",
            "Para un chat o grupo: menú ⋮ de la conversación (o mantenerla pulsada en la lista) → «🔔 Sonido de este chat»: «Elegir tono…», «🔕 Silencio» o «Como el general».",
            "En Ajustes se ven los chats con sonido propio: «Quitar» lo devuelve al general y «↺ Todos con el general» los quita todos.",
            "Se guarda solo en este móvil. En el Organizador los sonidos se eligen aparte.",
        )),
        Apartado("📲", "Notificaciones y ahorro de batería", listOf(
            "Acepta las notificaciones para enterarte de los mensajes con la app cerrada.",
            "El aviso fijo «Chat conectado» es lo que mantiene la conexión; al encender el móvil se vuelve a conectar solo.",
            "Sin internet (modo avión, sin datos ni wifi), arriba sale «🔴 Sin conexión a internet». En cuanto vuelve la conexión, el chat se conecta solo.",
            "En Ajustes, pulsa «Quitar el ahorro de batería». En algunos móviles (Xiaomi, Huawei, Oppo…) hay que permitir además el «inicio automático».",
        )),
        Apartado("👤", "Mi foto y la del grupo", listOf(
            "Ajustes → Mi foto: «Galería» o «Cámara». La ven tus compañeros en el móvil y en el Organizador.",
            "La foto de un grupo se cambia desde la cabecera de su conversación.",
        )),
        Apartado("⬆️", "Actualizaciones", listOf(
            "Al abrir la app, si hay una versión nueva, sale «Nueva versión…» con sus novedades: «Descargar e instalar» y, en la pantalla de Android, «Actualizar».",
            "Si aparece Google Play Protect, «Más detalles» → «Instalar de todas formas». No se pierde nada.",
            "En Ajustes: «Buscar actualización», y al pulsar el número de versión se ven sus novedades.",
            "Instala la app solo desde el código QR del Organizador o su enlace, nunca si te la pasan por otro medio.",
        )),
        Apartado("🚪", "Cerrar sesión", listOf(
            "Ajustes → Cerrar sesión: se borran del móvil el usuario y la contraseña, y deja de avisar. Hazlo si cambias de móvil o si el móvil no es solo tuyo.",
        )),
    )

    /** Quita tildes y mayúsculas, para buscar «version» y encontrar «versión». */
    fun normal(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

    /** Los apartados que contienen todas las palabras de la consulta (en el título o en el texto). Vacía → todos. */
    fun buscar(consulta: String): List<Apartado> {
        val palabras = normal(consulta).split(Regex("\\s+")).filter { it.isNotBlank() }
        if (palabras.isEmpty()) return apartados
        return apartados.filter { a ->
            val todo = normal(a.titulo + " " + a.lineas.joinToString(" "))
            palabras.all { it in todo }
        }
    }
}
