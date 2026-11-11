// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación, incluidas sus pruebas, son de
// Víctor Alejandro Martín Andrade, para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat

import com.princesscanarias.chat.datos.Canal
import com.princesscanarias.chat.datos.Fechas
import com.princesscanarias.chat.datos.Mensaje
import com.princesscanarias.chat.datos.Notas
import com.princesscanarias.chat.datos.Seguridad
import com.princesscanarias.chat.ui.Emoticonos
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Lógica sin Android: lo que llega del servidor, seguridad de adjuntos, versiones y emoticonos. */
class PruebasLogica {
    @Test fun mensajeConAdjuntoYNulos() {
        val m = Mensaje.deJson(JSONObject("""{"id":7,"canal_id":"dm:a:b","remitente":"a","contenido":"📎 x.pdf",
            "creado_en":"2026-11-09T13:15:00+00:00","tipo":"archivo",
            "adjunto":{"url":"https://abc.supabase.co/x.pdf","nombre_archivo":"x.pdf","mime_type":null,"tamano_bytes":10}}"""))
        assertEquals(7L, m.id); assertEquals("x.pdf", m.adjunto?.nombre); assertNull(m.adjunto?.mime)
    }

    @Test fun canalPrivadoYUltimoMensaje() {
        val c = Canal.deJson(JSONObject("""{"canal_id":"dm:a:b","tipo":"privado","nombre":null,"otro_usuario":"b",
            "ultimo_mensaje":{"contenido":"Hola","remitente":"b","creado_en":"2026-11-09T10:00:00+00:00"},"no_leidos":2}"""))
        assertNull(c.nombre); assertEquals("b", c.otroUsuario); assertEquals("Hola", c.ultimoContenido); assertEquals(2, c.noLeidos)
        val g = Canal.deJson(JSONObject("""{"canal_id":"general","tipo":"general","ultimo_mensaje":null,"no_leidos":0}"""))
        assertNull(g.ultimoContenido); assertNull(g.otroUsuario)
    }

    @Test fun adjuntosSoloDelAlmacen() {
        assertTrue(Seguridad.urlAdjuntoPermitida("https://xyz.supabase.co/storage/v1/a.pdf"))
        assertFalse(Seguridad.urlAdjuntoPermitida("http://xyz.supabase.co/a.pdf"))
        assertFalse(Seguridad.urlAdjuntoPermitida("https://supabase.co.malo.com/a.pdf"))
        assertFalse(Seguridad.urlAdjuntoPermitida(null))
    }

    @Test fun nombresDeArchivoSeguros() {
        assertEquals("informe.pdf", Seguridad.nombreSeguro("../../informe.pdf"))
        assertEquals("a_b.txt", Seguridad.nombreSeguro("a:b.txt"))
        assertEquals("archivo", Seguridad.nombreSeguro("..."))
        assertTrue(Seguridad.nombreSeguro("x".repeat(300) + ".pdf").let { it.length == 120 && it.endsWith(".pdf") })
    }

    @Test fun versiones() {
        assertTrue(Seguridad.versionMayor("v1.0.2", "1.0.0"))
        assertTrue(Seguridad.versionMayor("v1.10.0", "1.9.8"))
        assertFalse(Seguridad.versionMayor("v1.0.0", "1.0.0"))
        assertFalse(Seguridad.versionMayor("v0.9.9", "1.0.0"))
    }

    @Test fun emoticonosSeAnadenConEspacio() {
        assertEquals("👍 ", Emoticonos.anadir("", "👍"))
        assertEquals("Vale 👌 Perfecto ", Emoticonos.anadir("Vale", "👌 Perfecto"))
        assertEquals(32, Emoticonos.lista.size)
    }

    @Test fun fotosDePerfil() {
        val c = Canal.deJson(JSONObject("""{"canal_id":"grp:x","tipo":"grupo","nombre":"Compras","avatar":"https://a.supabase.co/f.jpg","no_leidos":0}"""))
        assertEquals("https://a.supabase.co/f.jpg", c.avatar)
        assertNull(Canal.deJson(JSONObject("""{"canal_id":"general","tipo":"general","avatar":null,"no_leidos":0}""")).avatar)
        assertEquals("AD", com.princesscanarias.chat.ui.Fotos.iniciales("Ana Demo"))
        assertEquals("C", com.princesscanarias.chat.ui.Fotos.iniciales("compras"))
        assertEquals("?", com.princesscanarias.chat.ui.Fotos.iniciales("  "))
        assertEquals(com.princesscanarias.chat.ui.Fotos.color("Ana"), com.princesscanarias.chat.ui.Fotos.color("ana"))
    }

    @Test fun fechasCortas() {
        val hoy = LocalDate.now()
        assertTrue(Fechas.corta(java.time.OffsetDateTime.now().toString(), hoy).matches(Regex("\\d\\d:\\d\\d")))
        assertTrue(Fechas.corta("2020-01-02T10:00:00+00:00", hoy).matches(Regex("\\d\\d/\\d\\d \\d\\d:\\d\\d")))
        assertEquals("", Fechas.corta("no es fecha", hoy))
    }

    // (v1.0.6) Notas de cada versión (Ajustes → «Versión x.y.z»)
    @Test fun notasDeUnaVersion() {
        val md = "# v1.0.6\n\n- **Copiar** un mensaje.\n\n# v1.0.4\n\n- Fotos.\n"
        assertEquals("- **Copiar** un mensaje.", Notas.de("1.0.6", md))
        assertEquals("- Fotos.", Notas.de("v1.0.04", md))
        assertNull(Notas.de("9.9.9", md))
        assertEquals("• Copiar un mensaje.", Notas.legible(Notas.de("1.0.6", md)!!))
    }

    @Test fun elChangelogTieneLaVersionActual() {
        val raiz = generateSequence(java.io.File("").absoluteFile) { it.parentFile }.first { java.io.File(it, "CHANGELOG.md").exists() }
        val md = java.io.File(raiz, "CHANGELOG.md").readText()
        assertTrue("CHANGELOG.md sin la versión ${BuildConfig.VERSION_NAME}", Notas.de(BuildConfig.VERSION_NAME, md) != null)
    }

    @Test fun cuandoEsUnaImagen() {
        fun m(tipo: String?, nombre: String, mime: String? = null, url: String = "https://abc.supabase.co/x") =
            Mensaje(1, "general", "a", "", "", tipo, com.princesscanarias.chat.datos.Adjunto(url, nombre, mime, 1))
        assertTrue(m("imagen", "x").esImagen)
        assertTrue(m("archivo", "FOTO.JPG").esImagen)
        assertTrue(m(null, "x", "image/png").esImagen)
        assertFalse(m("archivo", "factura.pdf").esImagen)
        assertFalse(m("imagen", "x.jpg", url = "").esImagen)
        assertFalse(Mensaje(1, "general", "a", "hola", "", "texto", null).esImagen)
    }
}
