// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

/**
 * (v1.0.16, chat v1.8.0) Mensajes de voz: lo que no depende de Android (se prueba en PruebasLogica).
 * Se graba manteniendo pulsado el micrófono; al soltar se envía y, si se desliza el dedo a la izquierda, se cancela.
 */
object Voz {
    const val MAX_MS = 3 * 60 * 1000L        // lo mismo que el servidor
    const val MIN_MS = 700L                  // un toque sin querer no se envía
    const val CANCELAR_DP = 110f             // cuánto hay que deslizar a la izquierda para cancelar
    const val MIME = "audio/mp4"

    /** 12400 → «0:12». */
    fun textoDuracion(ms: Long?): String {
        val s = ((ms ?: 0L).coerceAtLeast(0L)) / 1000
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    /** Qué hacer al soltar el micrófono. */
    enum class AlSoltar { ENVIAR, DEMASIADO_CORTO, CANCELADO }

    fun alSoltar(duracionMs: Long, desplazamientoDp: Float): AlSoltar = when {
        desplazamientoDp <= -CANCELAR_DP -> AlSoltar.CANCELADO
        duracionMs < MIN_MS -> AlSoltar.DEMASIADO_CORTO
        else -> AlSoltar.ENVIAR
    }

    fun nombreArchivo(ahoraMs: Long): String = "voz_$ahoraMs.m4a"

    /** Avance de 0 a 1 para la barra de la burbuja. */
    fun avance(posicionMs: Long, duracionMs: Long?): Float {
        val total = duracionMs ?: 0L
        if (total <= 0L) return 0f
        return (posicionMs.toFloat() / total).coerceIn(0f, 1f)
    }
}
