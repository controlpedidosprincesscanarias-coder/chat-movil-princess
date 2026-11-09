// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.servicio

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.princesscanarias.chat.datos.Chat
import com.princesscanarias.chat.datos.EstadoConexion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio en marcha («foreground service»): mantiene abierta la conexión del chat aunque la app
 * esté cerrada y avisa con una notificación de cada mensaje nuevo de otra persona. Android obliga
 * a mostrar mientras tanto un aviso fijo («Chat conectado»), de prioridad mínima.
 */
class ServicioChat : Service() {
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Avisos.crearCanales(this)
        val tipo = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING else 0
        ServiceCompat.startForeground(this, Avisos.ID_SERVICIO, Avisos.notificacionServicio(this, "Conectando…"), tipo)
        val chat = Chat.de(this)
        ambito.launch {
            chat.estado.collect { e ->
                val texto = when (e) {
                    EstadoConexion.CONECTADO -> "Conectado: te avisará de los mensajes nuevos"
                    EstadoConexion.CONECTANDO -> "Conectando…"
                    EstadoConexion.DESCONECTADO -> "Sin conexión, reintentando…"
                    EstadoConexion.SIN_SESION -> "Sin sesión"
                }
                getSystemService(android.app.NotificationManager::class.java)
                    .notify(Avisos.ID_SERVICIO, Avisos.notificacionServicio(this@ServicioChat, texto))
            }
        }
        ambito.launch {
            chat.nuevos.collect { m ->
                val propio = m.remitente.equals(chat.username, ignoreCase = true)
                val visible = chat.appEnPrimerPlano && chat.canalVisible == m.canalId
                if (!propio && !visible) {
                    val quien = chat.nombreDe(m.remitente)
                    val titulo = when {
                        m.canalId == "general" -> "General"
                        m.canalId.startsWith("grp:") -> "$quien (grupo)"
                        else -> quien
                    }
                    Avisos.mensajeNuevo(this@ServicioChat, m, titulo, quien)
                }
            }
        }
        chat.conectar()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val chat = Chat.de(this)
        if (!chat.credenciales.hayCredenciales) { stopSelf(); return START_NOT_STICKY }
        chat.conectar()
        return START_STICKY
    }

    override fun onDestroy() {
        ambito.cancel()
        super.onDestroy()
    }

    companion object {
        fun arrancar(context: Context) {
            if (!Chat.de(context).credenciales.hayCredenciales) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, ServicioChat::class.java))
            } catch (e: Exception) {
                android.util.Log.w("ServicioChat", "No se pudo arrancar el servicio: ${e.message}")
            }
        }

        fun parar(context: Context) = context.stopService(Intent(context, ServicioChat::class.java))
    }
}
