// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación, incluidas sus pruebas, son de
// Víctor Alejandro Martín Andrade, para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat

import com.princesscanarias.chat.datos.ApiChat
import com.princesscanarias.chat.datos.ErrorChat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Prueba de punta a punta contra Control de Pedidos y el servicio del chat arrancados EN LOCAL
 * (nunca contra producción). Solo se ejecuta si se dan las variables INTEGRACION_CP_URL,
 * INTEGRACION_CHAT_URL e INTEGRACION_CLAVE (ver README, «Prueba de integración»).
 * Usuarios: «compras prueba» hace de móvil y «admin prueba» de compañero en el Organizador.
 */
class PruebaIntegracion {
    private val cp = System.getenv("INTEGRACION_CP_URL").orEmpty()
    private val chat = System.getenv("INTEGRACION_CHAT_URL").orEmpty()
    private val clave = System.getenv("INTEGRACION_CLAVE").orEmpty()

    private fun api(usuario: String, c: String = clave) = ApiChat({ usuario to c }, cp, chat)

    private fun socket(api: ApiChat, recibidos: MutableList<JSONObject>): Socket {
        val s = IO.socket(chat, IO.Options().apply {
            transports = arrayOf("websocket", "polling")
            extraHeaders = mapOf("Cookie" to listOf(api.cookie))
        })
        s.on("nuevo_mensaje") { a -> (a.firstOrNull() as? JSONObject)?.let { recibidos.add(it) } }
        s.connect()
        val hasta = System.currentTimeMillis() + 10_000
        while (!s.connected() && System.currentTimeMillis() < hasta) Thread.sleep(100)
        assertTrue("el socket conecta con la cookie de Control de Pedidos", s.connected())
        return s
    }

    private fun esperar(cond: () -> Boolean): Boolean {
        val hasta = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10)
        while (System.currentTimeMillis() < hasta) { if (cond()) return true; Thread.sleep(100) }
        return false
    }

    @Test fun deMovilAEscritorioYALaInversa() {
        assumeTrue("Sin servidores locales: prueba de integración omitida", cp.isNotBlank() && chat.isNotBlank() && clave.isNotBlank())

        // Contraseña incorrecta → error claro, sin sesión
        try { api("compras prueba", "mala").login("compras prueba", "mala"); throw AssertionError("debía fallar") }
        catch (e: ErrorChat) { assertEquals(401, e.codigo) }

        // Sin login previo: la primera llamada inicia sesión sola con las credenciales guardadas
        val movil = api("compras prueba")
        val canales = movil.canales()
        assertTrue("aparece el General", canales.any { it.id == "general" })
        assertTrue("la lista de usuarios trae a los compañeros", movil.usuarios().any { it.username == "admin prueba" })

        // El mismo usuario conectado dos veces (Organizador + móvil) y un compañero
        val escritorio = api("compras prueba").also { it.login("compras prueba", clave) }
        val companero = api("admin prueba").also { it.login("admin prueba", clave) }
        val enMovil = CopyOnWriteArrayList<JSONObject>()
        val enEscritorio = CopyOnWriteArrayList<JSONObject>()
        val sMovil = socket(movil, enMovil)
        val sEscritorio = socket(escritorio, enEscritorio)
        try {
            // Un compañero escribe por privado: llega al móvil Y al escritorio
            val m = companero.enviarMensaje(null, "compras prueba", "Hola desde el PC 👍")
            assertTrue("el privado llega al móvil", esperar { enMovil.any { it.optLong("id") == m.id } })
            assertTrue("y también al Organizador del mismo usuario", esperar { enEscritorio.any { it.optLong("id") == m.id } })

            // Lo que se envía desde el móvil también aparece en el escritorio del mismo usuario
            val r = movil.enviarMensaje(m.canalId, null, "Respondo desde el móvil 👌 Perfecto")
            assertTrue("la respuesta del móvil llega al escritorio", esperar { enEscritorio.any { it.optLong("id") == r.id } })

            // Historial de la conversación, con los emoticonos intactos
            val (canal, historial) = movil.mensajes(null, "admin prueba")
            assertEquals(m.canalId, canal)
            assertTrue(historial.any { it.contenido == "Hola desde el PC 👍" })
            assertTrue(historial.any { it.contenido == "Respondo desde el móvil 👌 Perfecto" })

            // Grupo creado desde el móvil
            val grupo = movil.crearGrupo("Grupo de prueba", listOf("admin prueba", "hotel prueba"))
            assertTrue(grupo.startsWith("grp:"))
            assertTrue(movil.canales().any { it.id == grupo && it.nombre == "Grupo de prueba" })
        } finally {
            sMovil.disconnect(); sEscritorio.disconnect()
        }
    }
}
