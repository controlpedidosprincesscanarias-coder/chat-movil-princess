// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.servicio

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import com.princesscanarias.chat.datos.Sonidos

/**
 * (v1.0.22, petición de Víctor) Sonido de los mensajes en Android: lo elegido (en este móvil, en preferencias) y
 * los canales de notificación que lo hacen sonar. La lógica sin Android está en datos/Sonidos.kt.
 *
 * Preferencias «sonidos»: "general" y "general_v" (valor y versión del canal general); para cada chat con sonido
 * propio, "c:<canal>" (valor), "v:<canal>" (versión de su canal) y "t:<canal>" (su nombre, para Ajustes).
 * La versión se guarda aunque se quite el sonido propio, para no reutilizar un id de canal ya borrado
 * (Android recordaría su sonido antiguo).
 */
object SonidosCanales {
    private const val GRUPO = "chats"
    private val audio = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

    data class Propio(val canalId: String, val titulo: String, val valor: String)

    private fun prefs(c: Context) = c.getSharedPreferences("sonidos", Context.MODE_PRIVATE)
    private fun nm(c: Context) = c.getSystemService(NotificationManager::class.java)

    fun general(c: Context): String = prefs(c).getString("general", null) ?: Sonidos.PREDETERMINADO
    fun propio(c: Context, canalId: String): String? = prefs(c).getString("c:$canalId", null)

    fun propios(c: Context): List<Propio> {
        val p = prefs(c)
        return p.all.keys.filter { it.startsWith("c:") }.map { k ->
            val id = k.removePrefix("c:")
            Propio(id, p.getString("t:$id", null) ?: id, p.getString(k, null) ?: Sonidos.PREDETERMINADO)
        }.sortedBy { it.titulo.lowercase() }
    }

    /** Cambia el sonido general: canal nuevo y se borra el anterior. */
    fun fijarGeneral(c: Context, valor: String) {
        val p = prefs(c)
        val v = p.getInt("general_v", 0)
        nm(c).deleteNotificationChannel(Sonidos.idGeneral(v))
        p.edit().putString("general", valor).putInt("general_v", v + 1).apply()
        canalGeneral(c)
    }

    /** Sonido propio de un chat; null lo devuelve al general. */
    fun fijarPropio(c: Context, canalId: String, titulo: String, valor: String?) {
        val p = prefs(c)
        val v = p.getInt("v:$canalId", 0)
        if (p.contains("c:$canalId")) nm(c).deleteNotificationChannel(Sonidos.idChat(canalId, v))
        if (valor == null) {
            p.edit().remove("c:$canalId").remove("t:$canalId").apply()
        } else {
            p.edit().putString("c:$canalId", valor).putString("t:$canalId", titulo).putInt("v:$canalId", v + 1).apply()
        }
    }

    /** Quita los sonidos propios de todos los chats. */
    fun todosConElGeneral(c: Context) {
        propios(c).forEach { fijarPropio(c, it.canalId, it.titulo, null) }
    }

    /** Canal de Android por el que avisar de un mensaje de ese chat (lo crea si hace falta). */
    fun canalPara(c: Context, canalId: String): String {
        val p = prefs(c)
        val valor = p.getString("c:$canalId", null) ?: return canalGeneral(c)
        val id = Sonidos.idChat(canalId, p.getInt("v:$canalId", 1))
        crear(c, id, "Chat: " + (p.getString("t:$canalId", null) ?: canalId), valor)
        return id
    }

    /** Canal de los chats sin sonido propio (lo crea si hace falta). */
    fun canalGeneral(c: Context): String {
        val v = prefs(c).getInt("general_v", 0)
        val id = Sonidos.idGeneral(v)
        crear(c, id, "Mensajes nuevos", general(c))
        return id
    }

    private fun crear(c: Context, id: String, nombre: String, valor: String) {
        val nm = nm(c)
        if (nm.getNotificationChannel(id) != null) return
        nm.createNotificationChannelGroup(NotificationChannelGroup(GRUPO, "Chats"))
        nm.createNotificationChannel(NotificationChannel(id, nombre, NotificationManager.IMPORTANCE_HIGH).apply {
            description = if (valor == Sonidos.SILENCIO) "Aviso sin sonido" else "Aviso con sonido cuando te escriben"
            group = GRUPO
            when (valor) {
                Sonidos.SILENCIO -> { setSound(null, null); enableVibration(false) }
                Sonidos.PREDETERMINADO -> { setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audio); enableVibration(true) }
                else -> { setSound(Uri.parse(valor), audio); enableVibration(true) }
            }
        })
    }

    /** Nombre para la pantalla: «Tono del móvil», «🔕 Silencio» o el título del tono elegido. */
    fun nombre(c: Context, valor: String): String = when (valor) {
        Sonidos.PREDETERMINADO -> "Tono del móvil"
        Sonidos.SILENCIO -> "🔕 Silencio"
        else -> try { RingtoneManager.getRingtone(c, Uri.parse(valor))?.getTitle(c) } catch (e: Exception) { null } ?: "Tono elegido"
    }

    /** Hace sonar un valor (para probarlo). */
    fun probar(c: Context, valor: String) {
        val uri = when (valor) {
            Sonidos.SILENCIO -> return
            Sonidos.PREDETERMINADO -> Settings.System.DEFAULT_NOTIFICATION_URI
            else -> Uri.parse(valor)
        }
        try { RingtoneManager.getRingtone(c, uri)?.play() } catch (e: Exception) { }
    }
}

/** Selector de tonos de Android. Entrada: el valor actual. Salida: el valor elegido, o null si se canceló. */
class ElegirTono(private val titulo: String) : ActivityResultContract<String?, String?>() {
    override fun createIntent(context: Context, input: String?): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, titulo)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            val actual: Uri? = when (input) {
                null, Sonidos.PREDETERMINADO -> Settings.System.DEFAULT_NOTIFICATION_URI
                Sonidos.SILENCIO -> null
                else -> Uri.parse(input)
            }
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, actual)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        @Suppress("DEPRECATION")
        val uri: Uri? = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        return Sonidos.desdeSelector(uri?.toString(), uri == Settings.System.DEFAULT_NOTIFICATION_URI)
    }
}
