// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * (v1.0.24, petición de Víctor, 25 nov 2026: «revisar el mensaje cuando no existe conexión en el chat móvil»)
 * Sin internet, el aviso de abajo enseñaba el error técnico de Android en inglés («Unable to resolve host
 * "proxy-chat…": No address associated with hostname»). Aquí se traduce cualquier fallo a un texto claro.
 * Sin Android, para probarlo en PruebasLogica.
 */
object Errores {
    const val SIN_INTERNET = "Sin conexión a internet. Comprueba los datos móviles o el wifi: el chat se conectará solo en cuanto vuelva."
    const val TARDA = "El chat tarda en responder. Comprueba la conexión y vuelve a intentarlo en un momento."
    const val SEGURA = "No se pudo abrir una conexión segura con el chat. Prueba con datos móviles u otra wifi (algunas wifi públicas lo impiden)."
    const val SIN_RESPUESTA = "No se pudo conectar con el chat. Vuelve a intentarlo en un momento."

    /** Texto para la persona. Los mensajes propios de la app (ErrorChat) se dejan tal cual. */
    fun legible(e: Throwable?, porDefecto: String = SIN_RESPUESTA): String {
        var actual: Throwable? = e
        var vueltas = 0
        while (actual != null && vueltas < 8) {
            when (actual) {
                is ErrorChat -> return actual.message?.takeIf { it.isNotBlank() } ?: porDefecto
                is UnknownHostException, is NoRouteToHostException, is ConnectException -> return SIN_INTERNET
                is InterruptedIOException -> return TARDA          // también SocketTimeoutException
                is SSLException -> return SEGURA
                is SocketException -> return SIN_INTERNET           // «Network is unreachable», conexión cortada…
            }
            actual = actual.cause.takeIf { it !== actual }
            vueltas++
        }
        return if (e is IOException) SIN_RESPUESTA else porDefecto
    }
}
