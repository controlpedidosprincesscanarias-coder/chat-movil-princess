// Chat Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ErrorChat(mensaje: String, val codigo: Int = 0) : IOException(mensaje)

/**
 * Cliente REST del chat. El mismo contrato que el Organizador (app/services/chat_client.py):
 *  1. POST {CONTROL_PEDIDOS_URL}/api/bridge/login con usuario y contraseña → cookie de sesión.
 *  2. Esa cookie se envía a mano al servicio del chat (otro dominio; el servicio del chat comparte
 *     la SECRET_KEY y lee la misma cookie).
 *  3. Si el chat responde 401 (sesión caducada), se vuelve a iniciar sesión una vez y se repite.
 * Todas las llamadas son bloqueantes: hay que hacerlas fuera del hilo principal.
 */
class ApiChat(
    private val credenciales: () -> Pair<String, String>?,
    private val urlControlPedidos: String = Config.CONTROL_PEDIDOS_URL,
    val urlChat: String = Config.CHAT_URL,
) {
    private val json = "application/json; charset=utf-8".toMediaType()
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    @Volatile var cookie: String = ""
        private set
    @Volatile var username: String = ""
        private set
    @Volatile var nombre: String = ""
        private set

    /** Inicia sesión en Control de Pedidos. Devuelve el nombre real del usuario. */
    @Synchronized
    fun login(usuario: String, clave: String): String {
        val cuerpo = JSONObject().put("username", usuario.trim().lowercase()).put("password", clave)
        val req = Request.Builder().url(urlControlPedidos + "/api/bridge/login")
            .header("User-Agent", Config.USER_AGENT)
            .post(cuerpo.toString().toRequestBody(json)).build()
        http.newCall(req).execute().use { r ->
            val texto = r.body?.string().orEmpty()
            val datos = try { JSONObject(texto) } catch (e: Exception) { JSONObject() }
            if (r.code == 429) throw ErrorChat("Demasiados intentos. Espera unos minutos.", 429)
            if (!r.isSuccessful || !datos.optBoolean("ok", false)) {
                throw ErrorChat(datos.texto("error") ?: "Usuario o contraseña incorrectos", r.code)
            }
            val cookies = r.headers("Set-Cookie").mapNotNull { it.substringBefore(";").takeIf { c -> "=" in c } }
            if (cookies.isEmpty()) throw ErrorChat("Control de Pedidos no devolvió la sesión")
            cookie = cookies.joinToString("; ")
            username = datos.texto("username") ?: usuario.trim().lowercase()
            nombre = datos.texto("nombre") ?: username
            return nombre
        }
    }

    /** Vuelve a iniciar sesión con las credenciales guardadas (sesión caducada). */
    fun reiniciarSesion(): Boolean {
        val (u, c) = credenciales() ?: return false
        return try { login(u, c); true } catch (e: Exception) { false }
    }

    private fun peticion(ruta: String, metodo: String = "GET", cuerpo: JSONObject? = null, reintento: Boolean = true): JSONObject {
        if (cookie.isEmpty() && !reiniciarSesion()) throw ErrorChat("No hay sesión. Vuelve a entrar.", 401)
        val b = Request.Builder().url(urlChat + ruta)
            .header("User-Agent", Config.USER_AGENT).header("Cookie", cookie)
        if (metodo == "POST") b.post((cuerpo ?: JSONObject()).toString().toRequestBody(json)) else b.get()
        http.newCall(b.build()).execute().use { r: Response ->
            if (r.code == 401 && reintento) {
                if (reiniciarSesion()) return peticion(ruta, metodo, cuerpo, reintento = false)
                throw ErrorChat("La sesión ha caducado. Vuelve a entrar.", 401)
            }
            val texto = r.body?.string().orEmpty()
            val datos = try { JSONObject(texto) } catch (e: Exception) { JSONObject() }
            if (!r.isSuccessful) throw ErrorChat(datos.texto("error") ?: "Error del chat (${r.code})", r.code)
            return datos
        }
    }

    fun canales(): List<Canal> = peticion("/api/chat/canales").optJSONArray("canales").lista { Canal.deJson(it) }

    fun usuarios(): List<Usuario> = peticion("/api/chat/usuarios").optJSONArray("usuarios").lista {
        Usuario(it.texto("username") ?: "", it.texto("nombre") ?: it.texto("username") ?: "", it.texto("rol"))
    }

    /** Historial (últimos 50). Al pedirlo, el servidor marca la conversación como leída. */
    fun mensajes(canalId: String?, destinatario: String? = null): Pair<String, List<Mensaje>> {
        val q = buildString {
            append("/api/chat/mensajes?")
            if (!canalId.isNullOrBlank()) append("canal_id=").append(URLEncoder.encode(canalId, "UTF-8"))
            else if (!destinatario.isNullOrBlank()) append("destinatario=").append(URLEncoder.encode(destinatario, "UTF-8"))
        }
        val d = peticion(q)
        val canal = d.texto("canal_id") ?: canalId ?: ""
        return canal to d.optJSONArray("mensajes").lista { Mensaje.deJson(it, canal) }
    }

    fun enviarMensaje(canalId: String?, destinatario: String?, contenido: String): Mensaje {
        val cuerpo = JSONObject().put("contenido", contenido)
        if (!canalId.isNullOrBlank()) cuerpo.put("canal_id", canalId) else cuerpo.put("destinatario", destinatario)
        val d = peticion("/api/chat/mensajes", "POST", cuerpo)
        return Mensaje.deJson(d.getJSONObject("mensaje"))
    }

    fun enviarArchivo(canalId: String, nombreArchivo: String, mime: String, datos: ByteArray): Mensaje {
        if (datos.size > Config.ADJUNTO_MAX_BYTES) throw ErrorChat("El archivo pasa de 10 MB.")
        val cuerpo = JSONObject().put("canal_id", canalId).put("nombre_archivo", nombreArchivo).put("mime_type", mime)
            .put("contenido_base64", java.util.Base64.getEncoder().encodeToString(datos))
        val d = peticion("/api/chat/mensajes/archivo", "POST", cuerpo)
        return Mensaje.deJson(d.getJSONObject("mensaje"))
    }

    fun crearGrupo(nombre: String, usuarios: List<String>): String {
        val d = peticion("/api/chat/grupos", "POST", JSONObject().put("nombre", nombre).put("usuarios", JSONArray(usuarios)))
        return d.texto("canal_id") ?: throw ErrorChat("No se pudo crear el grupo")
    }

    /** Descarga un adjunto (solo del almacén permitido y como máximo 12 MB). */
    fun descargar(url: String): ByteArray {
        if (!Seguridad.urlAdjuntoPermitida(url)) throw ErrorChat("La dirección del adjunto no es de confianza.")
        val req = Request.Builder().url(url).header("User-Agent", Config.USER_AGENT).build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw ErrorChat("No se pudo descargar el adjunto (${r.code})")
            val cuerpo = r.body ?: throw ErrorChat("Adjunto vacío")
            if (cuerpo.contentLength() > Config.ADJUNTO_MAX_BYTES + 2 * 1024 * 1024) throw ErrorChat("El adjunto es demasiado grande.")
            return cuerpo.bytes()
        }
    }

    fun cerrarSesion() {
        cookie = ""; username = ""; nombre = ""
    }
}

private inline fun <T> JSONArray?.lista(f: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optJSONObject(it)?.let(f) }
}
