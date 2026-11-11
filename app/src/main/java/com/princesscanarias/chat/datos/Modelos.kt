// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate

/** optString() devuelve «null» (texto) cuando el valor es JSON null: esto devuelve null de verdad. */
fun JSONObject.texto(clave: String): String? =
    if (!has(clave) || isNull(clave)) null else optString(clave).ifBlank { null }

data class Adjunto(val url: String, val nombre: String, val mime: String?, val tamano: Long?)

/** (v1.0.8, chat v1.4.0) El mensaje al que responde otro: quién, qué (recortado por el servidor) y de qué tipo. */
data class Cita(val id: Long, val remitente: String, val contenido: String, val tipo: String?)

data class Mensaje(
    val id: Long?,
    val canalId: String,
    val remitente: String,
    val contenido: String,
    val creadoEn: String,
    val tipo: String?,
    val adjunto: Adjunto?,
    val respondeA: Cita? = null,      // (v1.0.8) responder citando
    val reenviado: Boolean = false,   // (v1.0.8) copia reenviada
) {
    /** (v1.0.8) Lo eliminó su autor: se ve «🚫 Mensaje eliminado», sin menú. */
    val eliminado: Boolean get() = tipo == "eliminado"

    /** (v1.0.6) Imagen adjunta: se ve en miniatura en la conversación en vez de solo su nombre. */
    val esImagen: Boolean
        get() {
            val a = adjunto ?: return false
            if (a.url.isBlank()) return false
            val ext = a.nombre.substringAfterLast('.', "").lowercase()
            return tipo == "imagen" || a.mime?.startsWith("image/") == true ||
                ext in setOf("png", "jpg", "jpeg", "gif", "bmp", "webp")
        }

    companion object {
        fun deJson(o: JSONObject, canalPorDefecto: String = ""): Mensaje {
            val adj = o.optJSONObject("adjunto")?.let {
                Adjunto(it.texto("url") ?: "", it.texto("nombre_archivo") ?: "archivo",
                    it.texto("mime_type"), if (it.has("tamano_bytes")) it.optLong("tamano_bytes") else null)
            }
            return Mensaje(
                id = if (o.has("id") && !o.isNull("id")) o.optLong("id") else null,
                canalId = o.texto("canal_id") ?: canalPorDefecto,
                remitente = o.texto("remitente") ?: "",
                contenido = o.texto("contenido") ?: "",
                creadoEn = o.texto("creado_en") ?: "",
                tipo = o.texto("tipo"),
                adjunto = adj,
                respondeA = o.optJSONObject("responde_a")?.let {
                    if (!it.has("id") || it.isNull("id")) null
                    else Cita(it.optLong("id"), it.texto("remitente") ?: "", it.texto("contenido") ?: "", it.texto("tipo"))
                },
                reenviado = o.optBoolean("reenviado", false),
            )
        }
    }
}

data class Canal(
    val id: String,
    val tipo: String,              // general | privado | grupo
    val nombre: String?,
    val otroUsuario: String?,
    val noLeidos: Int,
    val ultimoContenido: String?,
    val ultimoCreadoEn: String?,
    val avatar: String? = null,     // (v1.0.4) foto del compañero (privado) o del grupo
) {
    companion object {
        fun deJson(o: JSONObject) = Canal(
            id = o.texto("canal_id") ?: "",
            tipo = o.texto("tipo") ?: "privado",
            nombre = o.texto("nombre"),
            otroUsuario = o.texto("otro_usuario"),
            noLeidos = o.optInt("no_leidos", 0),
            ultimoContenido = o.optJSONObject("ultimo_mensaje")?.texto("contenido"),
            ultimoCreadoEn = o.optJSONObject("ultimo_mensaje")?.texto("creado_en"),
            avatar = o.texto("avatar"),
        )
    }
}

data class Usuario(val username: String, val nombre: String, val rol: String?, val avatar: String? = null)

/** (v1.0.10, chat v1.5.0) Un integrante de un grupo. */
data class Integrante(val username: String, val nombre: String, val avatar: String?, val esCreador: Boolean)

/** (v1.0.10, chat v1.5.0) Integrantes de un grupo y si puedo gestionarlo (quien lo creó; decisión de Víctor). */
data class DetalleGrupo(val canalId: String, val nombre: String, val creadoPor: String?, val avatar: String?,
                        val puedeGestionar: Boolean, val miembros: List<Integrante>) {
    /** Compañeros que se pueden añadir: los que no están ya, por nombre. */
    fun candidatos(usuarios: List<Usuario>): List<Usuario> {
        val dentro = miembros.map { it.username.lowercase() }.toSet()
        return usuarios.filter { it.username.lowercase() !in dentro }.sortedBy { it.nombre.lowercase() }
    }

    companion object {
        fun deJson(o: JSONObject): DetalleGrupo {
            val lista = o.optJSONArray("miembros")
            val miembros = (0 until (lista?.length() ?: 0)).mapNotNull { i ->
                lista?.optJSONObject(i)?.let {
                    Integrante(it.texto("username") ?: "", it.texto("nombre") ?: it.texto("username") ?: "",
                        it.texto("avatar"), it.optBoolean("es_creador", false))
                }
            }
            return DetalleGrupo(o.texto("canal_id") ?: "", o.texto("nombre") ?: "Grupo", o.texto("creado_por"),
                o.texto("avatar"), o.optBoolean("puede_gestionar", false), miembros)
        }
    }
}

/** Formatos de fecha y hora de las burbujas (hora local del móvil). */
object Fechas {
    private val hora = DateTimeFormatter.ofPattern("HH:mm")
    private val diaHora = DateTimeFormatter.ofPattern("dd/MM HH:mm")

    fun local(iso: String?): OffsetDateTime? = try {
        if (iso.isNullOrBlank()) null
        else OffsetDateTime.parse(iso.replace("Z", "+00:00")).atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime()
    } catch (e: Exception) {
        try { java.time.LocalDateTime.parse(iso).atOffset(java.time.ZoneOffset.UTC)
            .atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime() } catch (e2: Exception) { null }
    }

    /** «14:05» si es de hoy; «09/11 14:05» si no. */
    fun corta(iso: String?, hoy: LocalDate = LocalDate.now()): String {
        val f = local(iso) ?: return ""
        return if (f.toLocalDate() == hoy) f.format(hora) else f.format(diaHora)
    }
}
