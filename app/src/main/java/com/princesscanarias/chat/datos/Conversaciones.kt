// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

/**
 * (v1.0.14) Vaciar y eliminar conversaciones, y qué decir cuando el otro ya no está (petición de Víctor, 13 nov 2026).
 * Necesita el servicio del chat v1.7.0: «vaciar» y «eliminar» son solo para mí (el otro no nota nada); un chat
 * eliminado vuelve a salir si el otro escribe. Sin Android, para probarlo en PruebasLogica.
 */
object Conversaciones {
    const val DESACTIVADO = "usuario_desactivado"
    const val NO_EXISTE = "usuario_no_existe"

    /** Vaciar: cualquier conversación que se esté viendo. */
    fun puedeVaciar(canalId: String?): Boolean = !canalId.isNullOrBlank()

    /** Eliminar: solo los chats privados (de un grupo se sale; el General no se elimina). */
    fun puedeEliminar(canalId: String?): Boolean = canalId.orEmpty().startsWith("dm:")

    /** ¿Es un error de «el otro está desactivado / ya no existe»? */
    fun esDestinoNoDisponible(motivo: String?): Boolean = motivo == DESACTIVADO || motivo == NO_EXISTE

    /** Aviso fijo bajo la cabecera de la conversación según el estado del otro (null si está activo). */
    fun avisoEstado(otroEstado: String?, nombre: String): String? = when (otroEstado) {
        "desactivado" -> "$nombre está temporalmente desactivado en Control Pedidos: no recibirá tus mensajes."
        "no_existe" -> "$nombre ya no existe en Control Pedidos: no se le pueden enviar mensajes."
        else -> null
    }

    /** Etiqueta corta para la lista de conversaciones (null si está activo). */
    fun etiquetaEstado(otroEstado: String?): String? = when (otroEstado) {
        "desactivado" -> "⏸ Desactivado"
        "no_existe" -> "⛔ Ya no existe"
        else -> null
    }

    /** Título de la ventana que sale al intentar enviar. */
    fun tituloDestino(motivo: String?): String =
        if (motivo == NO_EXISTE) "Usuario no disponible" else "Usuario desactivado"
}
