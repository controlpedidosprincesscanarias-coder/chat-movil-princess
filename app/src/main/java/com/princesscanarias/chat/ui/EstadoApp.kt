// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.princesscanarias.chat.BuildConfig
import com.princesscanarias.chat.datos.Actualizaciones
import com.princesscanarias.chat.datos.Canal
import com.princesscanarias.chat.datos.Chat
import com.princesscanarias.chat.datos.DetalleGrupo
import com.princesscanarias.chat.datos.ErrorChat
import com.princesscanarias.chat.datos.Mensaje
import com.princesscanarias.chat.datos.Usuario
import com.princesscanarias.chat.datos.VersionNueva
import com.princesscanarias.chat.servicio.Avisos
import com.princesscanarias.chat.servicio.ServicioChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Pantalla {
    data object Acceso : Pantalla()
    data object Lista : Pantalla()
    data class Conversacion(val canalId: String?, val destinatario: String?, val titulo: String) : Pantalla()
    data object Nueva : Pantalla()
    data object Ajustes : Pantalla()
}

/** Estado de la interfaz: qué pantalla se ve, conversaciones, mensajes abiertos y avisos. */
class EstadoApp(app: Application) : AndroidViewModel(app) {
    val chat = Chat.de(app)
    val pantalla = mutableStateOf<Pantalla>(if (chat.credenciales.hayCredenciales) Pantalla.Lista else Pantalla.Acceso)
    val canales = mutableStateListOf<Canal>()
    val mensajes = mutableStateListOf<Mensaje>()
    val usuarios = mutableStateListOf<Usuario>()
    val cargando = mutableStateOf(false)
    val aviso = mutableStateOf<String?>(null)
    val actualizacion = mutableStateOf<VersionNueva?>(null)
    /** (v1.0.8) Mensaje al que se está respondiendo (barra «Respondiendo a…» encima de la caja de escribir). */
    val respondiendo = mutableStateOf<Mensaje?>(null)
    /** (v1.0.10) Integrantes del grupo abierto, si se ha pulsado su cabecera (null = cerrado). */
    val detalleGrupo = mutableStateOf<DetalleGrupo?>(null)
    private var canalAbierto: String? = null
    private var buscadaActualizacion = false
    private var refresco: Job? = null

    init {
        viewModelScope.launch {
            chat.nuevos.collect { m ->
                if (m.canalId == canalAbierto && mensajes.none { it.id != null && it.id == m.id }) {
                    mensajes.add(m)
                    if (m.canalId == canalAbierto) Avisos.quitar(getApplication(), m.canalId)
                }
                refrescarListaPronto()
            }
        }
        viewModelScope.launch { chat.errores.collect { aviso.value = it } }
        // (v1.0.10) Un grupo ha cambiado: si es el abierto y se ha eliminado o ya no soy miembro, a la lista
        viewModelScope.launch {
            chat.grupos.collect { (canal, eliminado) ->
                if (canal == canalAbierto) {
                    if (eliminado) { cerrarConversacion(); aviso.value = "El grupo se ha eliminado" }
                    else comprobarGrupo(canal)
                }
                refrescarListaPronto()
            }
        }
        // (v1.0.8) Un mensaje eliminado (por su autor, desde el móvil o el Organizador)
        viewModelScope.launch {
            chat.eliminados.collect { (id, canal) ->
                if (canal == canalAbierto) marcarEliminado(id)
                refrescarListaPronto()
            }
        }
    }

    fun alVolver() {
        if (pantalla.value == Pantalla.Lista) cargarCanales()
        (pantalla.value as? Pantalla.Conversacion)?.let { cargarMensajes(it) }
        buscarActualizacion()
    }

    fun entrar(usuario: String, clave: String, alTerminar: (String?) -> Unit) {
        cargando.value = true
        viewModelScope.launch {
            val error = withContext(Dispatchers.IO) {
                try { chat.entrar(usuario, clave); null } catch (e: Exception) { e.message ?: "No se pudo entrar" }
            }
            cargando.value = false
            if (error == null) {
                ServicioChat.arrancar(getApplication())
                pantalla.value = Pantalla.Lista
                cargarCanales()
            }
            alTerminar(error)
        }
    }

    fun salir() {
        ServicioChat.parar(getApplication())
        viewModelScope.launch(Dispatchers.IO) { chat.salir() }
        canales.clear(); mensajes.clear()
        pantalla.value = Pantalla.Acceso
    }

    private fun <T> enSegundoPlano(bloque: () -> T, alTerminar: (T) -> Unit) {
        viewModelScope.launch {
            try { alTerminar(withContext(Dispatchers.IO) { bloque() }) } catch (e: Exception) {
                aviso.value = e.message ?: "Error de conexión"
            }
        }
    }

    fun cargarCanales() = enSegundoPlano({ chat.refrescarNombresSiHaceFalta(); chat.api.canales() }) { lista ->
        canales.clear()
        canales.addAll(lista.sortedWith(compareBy<Canal> { it.tipo != "general" }.thenByDescending { it.ultimoCreadoEn ?: "" }))
    }

    private fun refrescarListaPronto() {
        if (pantalla.value != Pantalla.Lista) return
        refresco?.cancel()
        refresco = viewModelScope.launch { delay(600); cargarCanales() }
    }

    fun titulo(c: Canal): String = when (c.tipo) {
        "general" -> "General"
        "grupo" -> c.nombre ?: "Grupo"
        else -> chat.nombreDe(c.otroUsuario)
    }

    fun abrirConversacion(canalId: String?, destinatario: String?, titulo: String) {
        val p = Pantalla.Conversacion(canalId, destinatario, titulo)
        respondiendo.value = null
        pantalla.value = p
        cargarMensajes(p)
    }

    private fun cargarMensajes(p: Pantalla.Conversacion) {
        canalAbierto = p.canalId
        chat.canalVisible = p.canalId
        if (p.canalId == null) mensajes.clear()
        cargando.value = true
        enSegundoPlano({ chat.api.mensajes(p.canalId, p.destinatario) }) { (canal, lista) ->
            cargando.value = false
            canalAbierto = canal
            chat.canalVisible = canal
            chat.unirseCanal(canal)
            Avisos.quitar(getApplication(), canal)
            if (p.canalId == null) pantalla.value = p.copy(canalId = canal)
            mensajes.clear(); mensajes.addAll(lista)
        }
    }

    fun cerrarConversacion() {
        respondiendo.value = null
        detalleGrupo.value = null
        canalAbierto = null
        chat.canalVisible = null
        mensajes.clear()
        pantalla.value = Pantalla.Lista
        cargarCanales()
    }

    private fun anadirSiFalta(m: Mensaje) {
        if (mensajes.none { it.id != null && it.id == m.id }) mensajes.add(m)
    }

    fun enviar(texto: String) {
        val p = pantalla.value as? Pantalla.Conversacion ?: return
        val canal = canalAbierto ?: p.canalId
        val citado = respondiendo.value?.id
        respondiendo.value = null
        enSegundoPlano({ chat.api.enviarMensaje(canal, p.destinatario, texto, citado) }) { anadirSiFalta(it) }
    }

    // ── (v1.0.10) Gestión de grupos ───────────────────────────────────────────
    private fun comprobarGrupo(canal: String) {
        viewModelScope.launch {
            try {
                val d = withContext(Dispatchers.IO) { chat.api.detalleGrupo(canal) }
                if (detalleGrupo.value != null) detalleGrupo.value = d
                val p = pantalla.value as? Pantalla.Conversacion
                if (p != null && p.canalId == canal && p.titulo != d.nombre) pantalla.value = p.copy(titulo = d.nombre)
            } catch (e: ErrorChat) {
                if (e.codigo == 404 && canal == canalAbierto) { cerrarConversacion(); aviso.value = "Ya no estás en ese grupo" }
            } catch (e: Exception) { /* sin red: se verá al volver */ }
        }
    }

    fun verGrupo(canal: String) {
        cargarUsuarios()
        enSegundoPlano({ chat.api.detalleGrupo(canal) }) { detalleGrupo.value = it }
    }

    private fun trasCambiarGrupo(d: DetalleGrupo) {
        detalleGrupo.value = d
        val p = pantalla.value as? Pantalla.Conversacion
        if (p != null && p.canalId == d.canalId) pantalla.value = p.copy(titulo = d.nombre)
        refrescarListaPronto()
    }

    fun anadirAlGrupo(canal: String, usuarios: List<String>) =
        enSegundoPlano({ chat.api.miembrosGrupo(canal, anadir = usuarios) }) { trasCambiarGrupo(it) }

    fun quitarDelGrupo(canal: String, usuario: String) =
        enSegundoPlano({ chat.api.miembrosGrupo(canal, quitar = listOf(usuario)) }) { trasCambiarGrupo(it) }

    fun renombrarGrupo(canal: String, nombre: String) =
        enSegundoPlano({ chat.api.renombrarGrupo(canal, nombre) }) { trasCambiarGrupo(it) }

    fun salirDelGrupo(canal: String) = enSegundoPlano({ chat.api.salirGrupo(canal) }) {
        detalleGrupo.value = null; cerrarConversacion(); aviso.value = "Has salido del grupo"
    }

    fun eliminarGrupo(canal: String) = enSegundoPlano({ chat.api.eliminarGrupo(canal) }) {
        detalleGrupo.value = null; cerrarConversacion(); aviso.value = "Grupo eliminado"
    }

    /** (v1.0.8) Pone un mensaje de la lista como eliminado (al eliminarlo yo o al llegar el aviso). */
    private fun marcarEliminado(id: Long) {
        val i = mensajes.indexOfFirst { it.id == id }
        if (i >= 0) mensajes[i] = mensajes[i].copy(contenido = "🚫 Mensaje eliminado", tipo = "eliminado", adjunto = null)
    }

    /** (v1.0.8) Elimina un mensaje propio para todos. */
    fun eliminar(m: Mensaje) {
        val id = m.id ?: return
        enSegundoPlano({ chat.api.eliminar(id) }) { marcarEliminado(id); refrescarListaPronto() }
    }

    /** (v1.0.8) Reenvía un mensaje a una conversación o a un compañero. */
    fun reenviar(m: Mensaje, canalId: String?, destinatario: String?) {
        val id = m.id ?: return
        aviso.value = "Reenviando…"
        enSegundoPlano({ chat.api.reenviar(id, canalId, destinatario) }) { nuevo ->
            aviso.value = "Mensaje reenviado"
            if (nuevo.canalId == canalAbierto) anadirSiFalta(nuevo)
            refrescarListaPronto()
        }
    }

    fun enviarArchivo(a: ArchivoListo) {
        val canal = canalAbierto ?: return
        aviso.value = "Enviando ${a.nombre}…"
        enSegundoPlano({ chat.api.enviarArchivo(canal, a.nombre, a.mime, a.datos) }) {
            anadirSiFalta(it); aviso.value = null
        }
    }

    fun abrirAdjunto(m: Mensaje) {
        val adj = m.adjunto ?: return
        aviso.value = "Abriendo ${adj.nombre}…"
        enSegundoPlano({ chat.api.descargar(adj.url) }) { datos ->
            aviso.value = null
            Archivos.abrir(getApplication(), adj.nombre, adj.mime, datos)
        }
    }

    /** (v1.0.6) «Guardar en…»: descarga el adjunto y lo escribe en el sitio elegido con el selector de Android. */
    fun guardarAdjunto(m: Mensaje, destino: android.net.Uri?) {
        val adj = m.adjunto ?: return
        if (destino == null) return
        aviso.value = "Guardando ${adj.nombre}…"
        enSegundoPlano({ Archivos.guardar(getApplication(), destino, chat.api.descargar(adj.url)) }) {
            aviso.value = "${adj.nombre} guardado"
        }
    }

    /** (v1.0.6) «Compartir» un adjunto con otra app. */
    fun compartirAdjunto(m: Mensaje) {
        val adj = m.adjunto ?: return
        aviso.value = "Preparando ${adj.nombre}…"
        enSegundoPlano({ chat.api.descargar(adj.url) }) { datos ->
            aviso.value = null
            Archivos.compartir(getApplication(), adj.nombre, adj.mime, datos)
        }
    }

    /** (v1.0.4) Pone mi foto (canalId null) o la de un grupo. */
    fun cambiarFoto(uri: android.net.Uri?, canalId: String? = null) {
        if (uri == null) return
        aviso.value = "Subiendo la foto…"
        enSegundoPlano({ chat.cambiarFoto(Fotos.preparar(getApplication(), uri), canalId) }) {
            aviso.value = "Foto actualizada"
            if (canalId != null) cargarCanales()
        }
    }

    fun quitarMiFoto() = enSegundoPlano({ chat.cambiarFoto(null) }) { aviso.value = "Foto quitada" }

    fun canal(canalId: String?): Canal? = canales.firstOrNull { it.id == canalId }

    fun cargarUsuarios() = enSegundoPlano({ chat.api.usuarios() }) { usuarios.clear(); usuarios.addAll(it) }

    fun crearGrupo(nombre: String, miembros: List<String>) =
        enSegundoPlano({ chat.api.crearGrupo(nombre, miembros) }) { canal ->
            chat.unirseCanal(canal)
            abrirConversacion(canal, null, nombre)
        }

    fun buscarActualizacion(forzar: Boolean = false) {
        if (buscadaActualizacion && !forzar) return
        buscadaActualizacion = true
        viewModelScope.launch {
            val v = withContext(Dispatchers.IO) { try { Actualizaciones.buscar(BuildConfig.VERSION_NAME) } catch (e: Exception) { null } }
            actualizacion.value = v
            if (forzar && v == null) aviso.value = "Tienes la última versión (${BuildConfig.VERSION_NAME})"
        }
    }

    fun instalarActualizacion(v: VersionNueva) {
        aviso.value = "Descargando la versión ${v.version}…"
        enSegundoPlano({ Actualizaciones.descargar(getApplication(), v) }) { apk ->
            aviso.value = null
            try { Actualizaciones.instalar(getApplication(), apk) } catch (e: Exception) { aviso.value = e.message }
        }
    }
}
