// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

enum class EstadoConexion { DESCONECTADO, CONECTANDO, CONECTADO, SIN_SESION }

/**
 * Punto único de la app: sesión, conexión en tiempo real (Socket.IO) y caché de nombres.
 * Lo usan la interfaz y el servicio en marcha (ServicioChat), que es quien lo mantiene conectado
 * aunque la app esté cerrada. El servidor mete cada conexión en la sala «user:<usuario>», así que
 * se puede estar a la vez en el Organizador y en el móvil: los mensajes llegan a los dos.
 */
class Chat(context: Context) {
    private val app = context.applicationContext
    val credenciales = Credenciales(app)
    val api = ApiChat(credenciales = { credenciales.usuario?.let { u -> credenciales.clave?.let { c -> u to c } } })
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _estado = MutableStateFlow(EstadoConexion.DESCONECTADO)
    val estado: StateFlow<EstadoConexion> = _estado
    private val _nuevos = MutableSharedFlow<Mensaje>(extraBufferCapacity = 64)
    /** Cada mensaje que llega en tiempo real (de cualquiera, también los propios desde el PC). */
    val nuevos: SharedFlow<Mensaje> = _nuevos
    private val _eliminados = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 32)
    /** (v1.0.8) Mensajes eliminados en tiempo real: (id, canal). */
    val eliminados: SharedFlow<Pair<Long, String>> = _eliminados
    private val _grupos = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 16)
    /** (v1.0.10) Un grupo ha cambiado: (canal, eliminado). */
    val grupos: SharedFlow<Pair<String, Boolean>> = _grupos
    private val _errores = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errores: SharedFlow<String> = _errores

    private val nombres = ConcurrentHashMap<String, String>()
    /** (v1.0.4) Fotos de perfil: usuario → URL (las de grupo van en cada Canal). */
    private val fotos = ConcurrentHashMap<String, String>()
    private val _fotosCambiadas = MutableStateFlow(0)
    /** Cambia cada vez que llega una foto nueva (para refrescar la interfaz). */
    val fotosCambiadas: StateFlow<Int> = _fotosCambiadas
    @Volatile var miFoto: String? = null
        private set

    fun fotoDe(usuario: String?): String? = usuario?.let { fotos[it.lowercase()] }
    @Volatile private var socket: Socket? = null
    @Volatile private var ultimoLogin = 0L

    /** Conversación que se está viendo ahora en pantalla (para no avisar de lo que ya se ve). */
    @Volatile var canalVisible: String? = null
    @Volatile var appEnPrimerPlano: Boolean = false

    val username: String get() = api.username.ifBlank { credenciales.usuario.orEmpty() }

    fun nombreDe(usuario: String?): String {
        if (usuario.isNullOrBlank()) return ""
        if (usuario.equals(CuentaAvisos.BOT, true)) return CuentaAvisos.NOMBRE   // (v1.0.12) avisos de Control Pedidos
        return nombres[usuario.lowercase()] ?: usuario
    }

    /** Login desde la pantalla de acceso: si va bien, guarda las credenciales cifradas. */
    fun entrar(usuario: String, clave: String): String {
        val nombre = api.login(usuario, clave)
        credenciales.guardar(usuario.trim().lowercase(), clave, nombre)
        refrescarNombres()
        conectar()
        return nombre
    }

    fun salir() {
        desconectar()
        api.cerrarSesion()
        credenciales.borrar()
        nombres.clear()
        _estado.value = EstadoConexion.SIN_SESION
    }

    fun refrescarNombres() {
        try {
            api.usuarios().forEach { u ->
                nombres[u.username.lowercase()] = u.nombre
                if (u.avatar != null) fotos[u.username.lowercase()] = u.avatar else fotos.remove(u.username.lowercase())
            }
            miFoto = api.miAvatar()
            _fotosCambiadas.value++
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer la lista de usuarios: ${e.message}")
        }
        if (api.username.isNotBlank()) nombres[api.username.lowercase()] = api.nombre
    }

    fun refrescarNombresSiHaceFalta() { if (nombres.size < 2) refrescarNombres() }

    /** Abre (o reabre) la conexión en tiempo real. No bloquea. */
    fun conectar() {
        if (!credenciales.hayCredenciales) { _estado.value = EstadoConexion.SIN_SESION; return }
        if (socket?.connected() == true) return
        ambito.launch {
            _estado.value = EstadoConexion.CONECTANDO
            if (api.cookie.isEmpty() && !api.reiniciarSesion()) {
                _estado.value = EstadoConexion.DESCONECTADO
                _errores.tryEmit("No se pudo iniciar sesión en Control de Pedidos")
                return@launch
            }
            if (nombres.isEmpty()) refrescarNombres()
            crearSocket()
        }
    }

    @Synchronized
    private fun crearSocket() {
        socket?.let { it.off(); it.disconnect() }
        val opciones = IO.Options().apply {
            transports = arrayOf("websocket", "polling")
            reconnection = true
            reconnectionDelay = 2_000
            reconnectionDelayMax = 15_000
            timeout = 20_000
            extraHeaders = mapOf("Cookie" to listOf(api.cookie), "User-Agent" to listOf(Config.USER_AGENT))
        }
        val s = IO.socket(api.urlChat, opciones)
        s.on(Socket.EVENT_CONNECT) { _estado.value = EstadoConexion.CONECTADO }
        s.on(Socket.EVENT_DISCONNECT) { _estado.value = EstadoConexion.CONECTANDO }
        s.on(Socket.EVENT_CONNECT_ERROR) {
            // El servidor rechaza la conexión si la sesión caducó (cada día): nueva sesión y socket nuevo
            _estado.value = EstadoConexion.CONECTANDO
            // Como mucho un intento de sesión por minuto: si el fallo es de red o el servicio está
            // despertando, Socket.IO ya reintenta solo y no hace falta pedir sesiones de más.
            val ahora = System.currentTimeMillis()
            if (ahora - ultimoLogin > 60_000) {
                ultimoLogin = ahora
                ambito.launch { if (api.reiniciarSesion()) crearSocket() }
            }
        }
        s.on("nuevo_mensaje") { args ->
            (args.firstOrNull() as? JSONObject)?.let { _nuevos.tryEmit(Mensaje.deJson(it)) }
        }
        s.on("avatar_cambiado") { args ->
            (args.firstOrNull() as? JSONObject)?.let { o ->
                val clave = o.texto("clave") ?: return@let
                val url = o.texto("avatar")
                if (clave.startsWith("user:")) {
                    val u = clave.removePrefix("user:")
                    if (u == username.lowercase()) miFoto = url
                    if (url != null) fotos[u] = url else fotos.remove(u)
                }
                _fotosCambiadas.value++   // los grupos se vuelven a leer con la lista de conversaciones
            }
        }
        s.on("mensaje_eliminado") { args ->
            (args.firstOrNull() as? JSONObject)?.let { o ->
                if (o.has("id")) _eliminados.tryEmit(o.optLong("id") to (o.texto("canal_id") ?: ""))
            }
        }
        s.on("grupo_cambiado") { args ->
            (args.firstOrNull() as? JSONObject)?.let { o ->
                o.texto("canal_id")?.let { _grupos.tryEmit(it to o.optBoolean("eliminado", false)) }
            }
        }
        s.on("error_chat") { args ->
            (args.firstOrNull() as? JSONObject)?.texto("error")?.let { _errores.tryEmit(it) }
        }
        socket = s
        s.connect()
    }

    /** (v1.0.4) Cambia mi foto o la de un grupo (bloqueante: llamar fuera del hilo principal). */
    fun cambiarFoto(datosJpeg: ByteArray?, canalId: String? = null): String? {
        val url = api.cambiarAvatar(datosJpeg, canalId)
        if (canalId == null) {
            miFoto = url
            if (url != null) fotos[username.lowercase()] = url else fotos.remove(username.lowercase())
        }
        _fotosCambiadas.value++
        return url
    }

    /** Para recibir al momento los mensajes de un grupo o privado recién creado. */
    fun unirseCanal(canalId: String) {
        socket?.emit("unirse_canal", JSONObject().put("canal_id", canalId))
    }

    fun desconectar() {
        socket?.let { it.off(); it.disconnect() }
        socket = null
        _estado.value = EstadoConexion.DESCONECTADO
    }

    companion object {
        private const val TAG = "Chat"
        @Volatile private var unico: Chat? = null
        fun de(context: Context): Chat = unico ?: synchronized(this) { unico ?: Chat(context).also { unico = it } }
    }
}
