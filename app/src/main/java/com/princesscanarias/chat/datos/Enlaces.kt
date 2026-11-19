// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

import java.net.URI

/**
 * (v1.0.20, chat v1.9.0) Enlaces dentro de los mensajes: se pueden pulsar, y los de vídeo (TikTok, YouTube,
 * Instagram) llevan una tarjeta con miniatura, título y autor que prepara el servidor del chat.
 * Petición de Víctor (19 nov 2026): «¿se podría poner reproductor de vídeos o vídeos web si se envían?».
 * Mismas reglas que el servidor (chat/vista_previa.py) y el Organizador (app/utils/enlaces.py). Sin Android: se prueba en la JVM.
 */
object Enlaces {
    /** Trozo de un mensaje: texto normal o un enlace (url != null). */
    data class Trozo(val texto: String, val url: String? = null)

    private val reEnlace = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
    private const val FINALES = ".,;:!?)]}»"

    private val SITIOS = mapOf(
        "tiktok" to listOf("tiktok.com"),
        "youtube" to listOf("youtube.com", "youtu.be", "youtube-nocookie.com"),
        "instagram" to listOf("instagram.com", "instagr.am"),
    )

    private fun hostDe(url: String): String? = try {
        val u = URI(url)
        if (u.scheme?.lowercase() in setOf("http", "https") && u.userInfo == null) u.host?.lowercase()?.trimEnd('.') else null
    } catch (e: Exception) { null }

    /** El texto partido en trozos de texto y enlaces (solo http/https; sin la puntuación final pegada). */
    fun trozos(texto: String): List<Trozo> {
        val salida = mutableListOf<Trozo>()
        var desde = 0
        for (m in reEnlace.findAll(texto)) {
            val url = m.value.trimEnd { it in FINALES }
            if (url.isEmpty() || hostDe(url) == null) continue
            if (m.range.first > desde) salida += Trozo(texto.substring(desde, m.range.first))
            salida += Trozo(url, url)
            desde = m.range.first + url.length
        }
        if (desde < texto.length) salida += Trozo(texto.substring(desde))
        return salida
    }

    /** «tiktok», «youtube» o «instagram» si es un enlace https de esos sitios; si no, null. */
    fun sitioDeVideo(url: String): String? {
        if (!url.startsWith("https://", ignoreCase = true)) return null
        val host = hostDe(url) ?: return null
        return SITIOS.entries.firstOrNull { (_, hosts) -> hosts.any { host == it || host.endsWith(".$it") } }?.key
    }

    /** El primer enlace de vídeo del mensaje (la tarjeta se enseña solo para uno, como en WhatsApp). */
    fun primerVideo(texto: String): String? = trozos(texto).firstNotNullOfOrNull { t -> t.url?.takeIf { sitioDeVideo(it) != null } }

    /** Extensiones de vídeo que se reproducen dentro del chat al pulsar. */
    val EXT_VIDEO = setOf("mp4", "m4v", "mov", "3gp", "webm", "mkv")

    fun esVideo(nombre: String, mime: String?): Boolean =
        mime?.lowercase()?.startsWith("video/") == true || nombre.substringAfterLast('.', "").lowercase() in EXT_VIDEO
}

/** (v1.0.20, chat v1.9.0) Tarjeta de un enlace de vídeo (GET /api/chat/vista-previa). */
data class TarjetaEnlace(val url: String, val proveedor: String, val sitio: String, val titulo: String,
                         val autor: String, val miniatura: String?) {
    /** Los bytes de la miniatura (viene como «data:image/…;base64,…»), o null. */
    fun bytesMiniatura(): ByteArray? {
        val m = miniatura ?: return null
        if (!m.startsWith("data:image/")) return null
        return try { java.util.Base64.getDecoder().decode(m.substringAfter(",")) } catch (e: Exception) { null }
    }

    companion object {
        fun deJson(o: org.json.JSONObject): TarjetaEnlace = TarjetaEnlace(
            o.optString("url"), o.optString("proveedor"), o.optString("sitio"),
            o.optString("titulo"), o.optString("autor"),
            if (o.isNull("miniatura")) null else o.optString("miniatura").ifBlank { null })
    }
}
