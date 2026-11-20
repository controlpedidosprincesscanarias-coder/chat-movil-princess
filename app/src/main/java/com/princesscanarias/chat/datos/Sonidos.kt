// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

/**
 * (v1.0.22, petición de Víctor, 20 nov 2026) Sonido de los mensajes: uno general y, si se quiere, otro distinto para
 * cada chat o grupo, o silencio. Decisiones de Víctor: cada persona elige los suyos; se guardan solo en este móvil
 * (sin sincronizar con el Organizador ni tocar el servidor del chat); se elige con el selector de tonos de Android.
 *
 * En Android el sonido va en el «canal de notificación» y no se puede cambiar después de crearlo: por eso cada
 * cambio crea un canal nuevo (con un número de versión en su id) y borra el anterior. El general sigue usando el
 * canal de siempre («mensajes») mientras no se cambie, para no perder lo que cada uno haya ajustado en Android.
 *
 * Aquí solo la lógica sin Android (se prueba en PruebasLogica); los canales y las preferencias, en
 * servicio/SonidosCanales.kt.
 */
object Sonidos {
    /** El tono de notificación del móvil (el de siempre). */
    const val PREDETERMINADO = "predeterminado"
    const val SILENCIO = "silencio"
    const val CANAL_GENERAL = "mensajes"

    /** Lo que suena en un chat: el suyo si tiene, si no el general, y si tampoco, el del móvil. */
    fun efectivo(propio: String?, general: String?): String =
        propio?.takeIf { it.isNotBlank() } ?: general?.takeIf { it.isNotBlank() } ?: PREDETERMINADO

    /** Valor que guarda lo elegido en el selector de tonos de Android: nada = silencio; el predeterminado = el del móvil. */
    fun desdeSelector(uri: String?, esPredeterminado: Boolean): String = when {
        uri.isNullOrBlank() -> SILENCIO
        esPredeterminado -> PREDETERMINADO
        else -> uri
    }

    /** Id del canal de Android de los chats que siguen al general (versión 0 = el canal de siempre). */
    fun idGeneral(version: Int): String = if (version <= 0) CANAL_GENERAL else "${CANAL_GENERAL}_v$version"

    /** Id del canal de Android de un chat con sonido propio. Solo letras, números y «_», y corto. */
    fun idChat(canalId: String, version: Int): String {
        val limpio = canalId.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').take(40)
        val huella = Integer.toHexString(canalId.hashCode())
        return "chat_${limpio}_${huella}_v$version"
    }
}

/**
 * (v1.0.22, petición de Víctor, 20 nov 2026) Orden de la lista de conversaciones: el General y «Control Pedidos»
 * fijos arriba, en ese orden, y debajo las demás, la última utilizada primero. «Utilizada» es lo último que pasó en
 * ella: el último mensaje (enviado o recibido, en cualquier dispositivo) o la última vez que se abrió en este móvil.
 */
object OrdenConversaciones {
    /** 0 = General, 1 = Control Pedidos, 2 = las demás. */
    fun fijo(c: Canal): Int = when {
        c.tipo == "general" || c.id == "general" -> 0
        CuentaAvisos.esCanal(c.id) -> 1
        else -> 2
    }

    /** Milisegundos de lo último que pasó en la conversación (0 si nada). */
    fun ultimoUso(c: Canal, abiertas: Map<String, Long>): Long {
        val mensaje = Fechas.local(c.ultimoCreadoEn)?.toInstant()?.toEpochMilli() ?: 0L
        return maxOf(mensaje, abiertas[c.id] ?: 0L)
    }

    fun ordenar(lista: List<Canal>, abiertas: Map<String, Long> = emptyMap()): List<Canal> =
        lista.sortedWith(compareBy<Canal> { fijo(it) }.thenByDescending { ultimoUso(it, abiertas) })
}
