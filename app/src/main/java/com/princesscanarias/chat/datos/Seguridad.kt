// Chat Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

import java.net.URI

/** Mismas reglas que el Organizador (app/services/chat_client.py) para lo que llega de otros usuarios. */
object Seguridad {
    private val malos = Regex("[\\x00-\\x1f\\x7f<>:\"/\\\\|?*\\u202a-\\u202e\\u2066-\\u2069]")

    /** La URL de un adjunto debe ser https y del almacén de adjuntos. */
    fun urlAdjuntoPermitida(url: String?): Boolean = try {
        val u = URI(url ?: "")
        u.scheme == "https" && !u.host.isNullOrBlank() && Config.HOSTS_ADJUNTOS.any { u.host.lowercase().endsWith(it) }
    } catch (e: Exception) { false }

    /** Nombre de archivo sin rutas ni caracteres peligrosos, de 120 caracteres como mucho. */
    fun nombreSeguro(nombre: String?): String {
        var n = (nombre ?: "").replace("\\", "/").substringAfterLast("/")
        n = malos.replace(n, "_").trim(' ', '.')
        if (n.isEmpty()) n = "archivo"
        if (n.length > 120) {
            val ext = n.substringAfterLast('.', "").let { if (it.length in 1..10) ".$it" else "" }
            n = n.take(120 - ext.length) + ext
        }
        return n
    }

    /** Versiones «v1.0.2» / «1.0.2» comparadas como números. */
    fun versionMayor(remota: String, local: String): Boolean {
        fun partes(v: String) = Regex("\\d+").findAll(v).map { it.value.toInt() }.toList()
        val a = partes(remota); val b = partes(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
