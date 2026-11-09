// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.servicio

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.princesscanarias.chat.R
import com.princesscanarias.chat.datos.Mensaje
import com.princesscanarias.chat.ui.MainActivity

/** Canales de notificación y avisos de mensaje nuevo. */
object Avisos {
    const val CANAL_MENSAJES = "mensajes"
    const val CANAL_CONEXION = "conexion"
    const val EXTRA_CANAL = "canal_id"
    const val EXTRA_TITULO = "titulo"
    const val ID_SERVICIO = 1

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CANAL_MENSAJES, "Mensajes nuevos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Aviso con sonido cuando te escriben"
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CANAL_CONEXION, "Chat conectado", NotificationManager.IMPORTANCE_MIN).apply {
            description = "Aviso fijo mientras la app mantiene la conexión para recibir los mensajes"
            setShowBadge(false)
        })
    }

    fun abrirApp(context: Context, canalId: String? = null, titulo: String? = null): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (canalId != null) { putExtra(EXTRA_CANAL, canalId); putExtra(EXTRA_TITULO, titulo) }
        }
        return PendingIntent.getActivity(context, (canalId ?: "").hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun notificacionServicio(context: Context, texto: String) =
        NotificationCompat.Builder(context, CANAL_CONEXION)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Chat Dpto. Compras Princess")
            .setContentText(texto)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(abrirApp(context))
            .build()

    /** Aviso de un mensaje nuevo; uno por conversación (los siguientes lo sustituyen). */
    fun mensajeNuevo(context: Context, m: Mensaje, titulo: String, quien: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val texto = m.adjunto?.let { "📎 ${it.nombre}" } ?: m.contenido
        val cuerpo = if (m.canalId.startsWith("dm:")) texto else "$quien: $texto"
        val n = NotificationCompat.Builder(context, CANAL_MENSAJES)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(abrirApp(context, m.canalId, titulo))
            .build()
        NotificationManagerCompat.from(context).notify(m.canalId.hashCode(), n)
    }

    fun quitar(context: Context, canalId: String) = NotificationManagerCompat.from(context).cancel(canalId.hashCode())
}
