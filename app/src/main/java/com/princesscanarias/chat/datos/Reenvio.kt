// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

/** (v1.0.8) Un sitio al que reenviar: una conversación que ya existe o un compañero sin privado abierto. */
data class Destino(val etiqueta: String, val canalId: String?, val destinatario: String?)

object Reenvio {
    /** Primero General, grupos y privados; después los compañeros sin privado (sin mí). Sin Android: PruebasLogica. */
    fun destinos(canales: List<Canal>, usuarios: List<Usuario>, yo: String, nombreDe: (String) -> String): List<Destino> {
        val conPrivado = mutableSetOf<String>()
        val salida = mutableListOf<Destino>()
        for (c in canales) when (c.tipo) {
            "general" -> salida += Destino("💬  General", "general", null)
            "grupo" -> salida += Destino("👥  ${c.nombre ?: "Grupo"}", c.id, null)
            else -> c.otroUsuario?.let { conPrivado += it.lowercase(); salida += Destino("👤  ${nombreDe(it)}", c.id, null) }
        }
        for (u in usuarios) {
            val n = u.username.lowercase()
            if (n.isNotBlank() && n != yo.lowercase() && n !in conPrivado) salida += Destino("👤  ${u.nombre}", null, u.username)
        }
        return salida
    }
}
