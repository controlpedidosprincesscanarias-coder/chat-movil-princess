// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación, incluidas sus pruebas, son de
// Víctor Alejandro Martín Andrade, para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat

import com.princesscanarias.chat.datos.Canal
import com.princesscanarias.chat.datos.Fechas
import com.princesscanarias.chat.datos.Mensaje
import com.princesscanarias.chat.datos.CuentaAvisos
import com.princesscanarias.chat.datos.Conversaciones
import com.princesscanarias.chat.datos.DetalleGrupo
import com.princesscanarias.chat.datos.Notas
import com.princesscanarias.chat.datos.Reenvio
import com.princesscanarias.chat.datos.Usuario
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

    // (v1.0.8, chat v1.4.0) Responder citando, reenviado y eliminado
    @Test fun respuestaReenvioYEliminado() {
        val r = Mensaje.deJson(JSONObject("""{"id":8,"canal_id":"grp:1","remitente":"beto","contenido":"Yo lo pido",
            "creado_en":"2026-11-11T18:00:00+00:00","tipo":"texto","adjunto":null,"reenviado":true,
            "responde_a":{"id":3,"remitente":"ana","contenido":"¿Quién pide?","tipo":"texto"}}"""))
        assertEquals(3L, r.respondeA?.id); assertEquals("ana", r.respondeA?.remitente); assertTrue(r.reenviado)
        val antiguo = Mensaje.deJson(JSONObject("""{"id":9,"canal_id":"general","remitente":"a","contenido":"hola","creado_en":""}"""))
        assertNull(antiguo.respondeA); assertFalse(antiguo.reenviado); assertFalse(antiguo.eliminado)
        assertTrue(Mensaje.deJson(JSONObject("""{"id":10,"canal_id":"general","remitente":"a","contenido":"x","tipo":"eliminado"}""")).eliminado)
        assertNull(Mensaje.deJson(JSONObject("""{"id":11,"canal_id":"general","remitente":"a","contenido":"x","responde_a":null}""")).respondeA)
    }

    @Test fun destinosParaReenviar() {
        val canales = listOf(
            Canal.deJson(JSONObject("""{"canal_id":"general","tipo":"general"}""")),
            Canal.deJson(JSONObject("""{"canal_id":"grp:1","tipo":"grupo","nombre":"Compras"}""")),
            Canal.deJson(JSONObject("""{"canal_id":"dm:ana:beto","tipo":"privado","otro_usuario":"beto"}""")))
        val usuarios = listOf(Usuario("ana", "Ana", null), Usuario("beto", "Beto", null), Usuario("carla", "Carla", null))
        val d = Reenvio.destinos(canales, usuarios, "ana") { if (it == "beto") "Beto Pérez" else it }
        assertEquals(listOf("general", "grp:1", "dm:ana:beto", null), d.map { it.canalId })
        assertEquals("carla", d.last().destinatario)
        assertEquals("👤  Beto Pérez", d[2].etiqueta)
    }

    // (v1.0.10, chat v1.5.0) Integrantes y candidatos para añadir a un grupo
    @Test fun detalleDeGrupo() {
        val d = DetalleGrupo.deJson(JSONObject("""{"canal_id":"grp:1","nombre":"Pescado","creado_por":"ana",
            "avatar":null,"puede_gestionar":true,"miembros":[{"username":"ana","nombre":"Ana","avatar":null,"es_creador":true},
            {"username":"beto","nombre":"Beto","avatar":"https://abc.supabase.co/b.jpg","es_creador":false}]}"""))
        assertTrue(d.puedeGestionar); assertEquals("ana", d.creadoPor); assertEquals(2, d.miembros.size)
        assertTrue(d.miembros[0].esCreador); assertNull(d.avatar)
        val c = d.candidatos(listOf(Usuario("Beto", "Beto", null), Usuario("dani", "Ángel", null), Usuario("carla", "carla", null)))
        assertEquals(listOf("carla", "dani"), c.map { it.username })
        assertFalse(DetalleGrupo.deJson(JSONObject("""{"canal_id":"grp:2"}""")).puedeGestionar)
    }

    // (v1.0.12, chat v1.6.0) La conversación de avisos de Control Pedidos
    @Test fun conversacionDeAvisos() {
        assertTrue(CuentaAvisos.esCanal("dm:ana:control-pedidos")); assertTrue(CuentaAvisos.esCanal("dm:control-pedidos:zoe"))
        assertFalse(CuentaAvisos.esCanal("dm:ana:beto")); assertFalse(CuentaAvisos.esCanal("general")); assertFalse(CuentaAvisos.esCanal(null))
    }

    // (v1.0.14) Vaciar y eliminar chats, y el estado del otro (chat v1.7.0)
    @Test fun vaciarYEliminar() {
        assertTrue(Conversaciones.puedeVaciar("general")); assertTrue(Conversaciones.puedeVaciar("grp:x")); assertFalse(Conversaciones.puedeVaciar(null))
        assertTrue(Conversaciones.puedeEliminar("dm:ana:beto")); assertTrue(Conversaciones.puedeEliminar("dm:ana:control-pedidos"))
        assertFalse(Conversaciones.puedeEliminar("general")); assertFalse(Conversaciones.puedeEliminar("grp:x")); assertFalse(Conversaciones.puedeEliminar(null))
    }

    @Test fun estadoDelOtro() {
        assertTrue(Conversaciones.esDestinoNoDisponible("usuario_desactivado")); assertTrue(Conversaciones.esDestinoNoDisponible("usuario_no_existe"))
        assertFalse(Conversaciones.esDestinoNoDisponible(null)); assertFalse(Conversaciones.esDestinoNoDisponible("otro"))
        assertTrue(Conversaciones.avisoEstado("desactivado", "Beto")!!.contains("temporalmente desactivado"))
        assertTrue(Conversaciones.avisoEstado("no_existe", "Beto")!!.contains("ya no existe"))
        assertEquals(null, Conversaciones.avisoEstado("activo", "Beto")); assertEquals(null, Conversaciones.avisoEstado(null, "Beto"))
        assertEquals("Usuario no disponible", Conversaciones.tituloDestino("usuario_no_existe"))
        assertEquals("Usuario desactivado", Conversaciones.tituloDestino("usuario_desactivado"))
        val c = Canal.deJson(JSONObject("""{"canal_id":"dm:ana:beto","tipo":"privado","otro_usuario":"beto","otro_estado":"no_existe"}"""))
        assertEquals("no_existe", c.otroEstado)
        assertEquals(null, Canal.deJson(JSONObject("""{"canal_id":"general","tipo":"general"}""")).otroEstado)
    }
}
