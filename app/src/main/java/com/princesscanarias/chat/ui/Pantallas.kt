// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.princesscanarias.chat.ui

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.princesscanarias.chat.BuildConfig
import com.princesscanarias.chat.R
import com.princesscanarias.chat.datos.EstadoConexion
import com.princesscanarias.chat.datos.Fechas
import com.princesscanarias.chat.datos.Mensaje
import com.princesscanarias.chat.datos.DetalleGrupo
import com.princesscanarias.chat.datos.Notas
import com.princesscanarias.chat.datos.Reenvio
import com.princesscanarias.chat.datos.Seguridad
import coil.compose.AsyncImage

@Composable
fun AppChat(e: EstadoApp) {
    val snack = remember { SnackbarHostState() }
    val aviso by e.aviso
    LaunchedEffect(aviso) {
        aviso?.let { snack.showSnackbar(it, duration = SnackbarDuration.Short); e.aviso.value = null }
    }
    Scaffold(snackbarHost = { SnackbarHost(snack) }, containerColor = Colores.fondo) { relleno ->
        Box(Modifier.padding(relleno).fillMaxSize()) {
            when (val p = e.pantalla.value) {
                Pantalla.Acceso -> PantallaAcceso(e)
                Pantalla.Lista -> PantallaLista(e)
                is Pantalla.Conversacion -> PantallaConversacion(e, p)
                Pantalla.Nueva -> PantallaNueva(e)
                Pantalla.Ajustes -> PantallaAjustes(e)
            }
        }
    }
    e.actualizacion.value?.let { v ->
        AlertDialog(
            onDismissRequest = { e.actualizacion.value = null },
            title = { Text("Nueva versión ${v.version}") },
            text = { Text(v.notas.replace(Regex("[#*`]"), "").trim().take(600).ifBlank { "Hay una versión nueva de Chat Dpto. Compras Princess." }) },
            confirmButton = { TextButton({ e.actualizacion.value = null; e.instalarActualizacion(v) }) { Text("Descargar e instalar") } },
            dismissButton = { TextButton({ e.actualizacion.value = null }) { Text("Más tarde") } },
        )
    }
}

@Composable
private fun Cabecera(titulo: String, subtitulo: String? = null, atras: (() -> Unit)? = null, corona: Boolean = false,
                     tituloPequeno: Boolean = false, avatar: (@Composable () -> Unit)? = null,
                     alPulsar: (() -> Unit)? = null,
                     acciones: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = {
            // (v1.0.10) En los grupos, pulsar la foto o el nombre abre sus integrantes
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = if (alPulsar != null) Modifier.clickable { alPulsar() } else Modifier) {
                if (avatar != null) { avatar(); Spacer(Modifier.width(10.dp)) }
                if (corona) {   // la corona de Princess en la pantalla principal
                    Image(painterResource(R.drawable.corona_blanca), contentDescription = "Princess",
                        modifier = Modifier.height(30.dp).padding(end = 10.dp))
                }
                Column {
                    Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold,
                        fontSize = if (tituloPequeno) 17.sp else LocalTextStyle.current.fontSize)
                    if (subtitulo != null) Text(subtitulo, fontSize = 12.sp, color = Colores.meta)
                }
            }
        },
        navigationIcon = { if (atras != null) IconButton(atras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } },
        actions = acciones,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.cabecera, titleContentColor = Colores.texto,
            navigationIconContentColor = Colores.texto, actionIconContentColor = Colores.texto),
    )
}

// ── Acceso ─────────────────────────────────────────────────────────────────
@Composable
fun PantallaAcceso(e: EstadoApp) {
    var usuario by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var ver by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val cargando by e.cargando
    val entrar = { if (usuario.isNotBlank() && clave.isNotBlank()) e.entrar(usuario, clave) { error = it } }

    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).background(Colores.navy, CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.corona_blanca), contentDescription = "Princess", modifier = Modifier.height(52.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Chat Dpto. Compras", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Colores.texto)
        Text("Princess", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Colores.texto)
        Text("Central de Compras · Princess Canarias", fontSize = 13.sp, color = Colores.dorado)
        Spacer(Modifier.height(28.dp))
        Text("Entra con tu usuario y tu contraseña de Control de Pedidos.", color = Colores.meta, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(usuario, { usuario = it }, label = { Text("Usuario") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(clave, { clave = it }, label = { Text("Contraseña") }, singleLine = true,
            visualTransformation = if (ver) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton({ ver = !ver }) { Icon(if (ver) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Ver contraseña") } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { entrar() }),
            modifier = Modifier.fillMaxWidth())
        error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Colores.error, textAlign = TextAlign.Center) }
        Spacer(Modifier.height(20.dp))
        Button({ entrar() }, enabled = !cargando, modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Colores.dorado, contentColor = Colores.navy)) {
            if (cargando) CircularProgressIndicator(Modifier.size(22.dp), color = Colores.navy, strokeWidth = 2.dp)
            else Text("Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text("Puedes estar conectado a la vez en el Organizador y en el móvil.", fontSize = 12.sp, color = Colores.meta, textAlign = TextAlign.Center)
    }
}

// ── Conversaciones ────────────────────────────────────────────────────────
@Composable
fun PantallaLista(e: EstadoApp) {
    val estado by e.chat.estado.collectAsState()
    LaunchedEffect(Unit) { e.cargarCanales() }
    val fotos by e.chat.fotosCambiadas.collectAsState()
    LaunchedEffect(fotos) { if (fotos > 0) e.cargarCanales() }   // una foto nueva (también de grupo)
    Column(Modifier.fillMaxSize()) {
        Cabecera("Chat Dpto. Compras Princess", subtitulo = textoEstado(estado), corona = true, tituloPequeno = true) {
            IconButton({ e.cargarCanales() }) { Icon(Icons.Default.Refresh, "Actualizar") }
            IconButton({ e.pantalla.value = Pantalla.Ajustes }) { Icon(Icons.Default.Settings, "Ajustes") }
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(e.canales, key = { it.id }) { c ->
                    val titulo = e.titulo(c)
                    Row(Modifier.fillMaxWidth().clickable { e.abrirConversacion(c.id, null, titulo) }
                        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        when (c.tipo) {
                            "general" -> Box(Modifier.size(44.dp).background(Colores.cabecera, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Forum, null, tint = Colores.texto)
                            }
                            "grupo" -> Avatar(c.avatar, titulo, icono = IconoGrupo)
                            else -> Avatar(c.avatar ?: e.chat.fotoDe(c.otroUsuario), titulo)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(titulo, fontWeight = if (c.noLeidos > 0) FontWeight.Bold else FontWeight.Medium, color = Colores.texto,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            c.ultimoContenido?.let { Text(it, color = Colores.meta, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Fechas.corta(c.ultimoCreadoEn), color = Colores.meta, fontSize = 11.sp)
                            if (c.noLeidos > 0) {
                                Box(Modifier.padding(top = 4.dp).background(Colores.propio, CircleShape).padding(horizontal = 7.dp, vertical = 2.dp)) {
                                    Text("${c.noLeidos}", color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Colores.caja)
                }
            }
            ExtendedFloatingActionButton(
                onClick = { e.pantalla.value = Pantalla.Nueva },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Nueva conversación") },
                containerColor = Colores.propio, contentColor = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

private fun textoEstado(e: EstadoConexion) = when (e) {
    EstadoConexion.CONECTADO -> "🟢 Conectado"
    EstadoConexion.CONECTANDO -> "🟡 Conectando…"
    EstadoConexion.DESCONECTADO -> "🔴 Sin conexión"
    EstadoConexion.SIN_SESION -> "Sin sesión"
}

// ── Conversación ──────────────────────────────────────────────────────────
@Composable
fun PantallaConversacion(e: EstadoApp, p: Pantalla.Conversacion) {
    val ctx = LocalContext.current
    var texto by rememberSaveable(p.canalId) { mutableStateOf("") }
    var emoticonos by remember { mutableStateOf(false) }
    var menuAdjuntar by remember { mutableStateOf(false) }
    var pendiente by remember { mutableStateOf<ArchivoListo?>(null) }
    var uriFoto by remember { mutableStateOf<Uri?>(null) }
    val lista = rememberLazyListState()
    val estado by e.chat.estado.collectAsState()
    val fotos by e.chat.fotosCambiadas.collectAsState()
    BackHandler { e.cerrarConversacion() }
    val canal = remember(p.canalId, fotos, e.canales.size) { e.canal(p.canalId) }
    val esGrupo = p.canalId.orEmpty().startsWith("grp:")
    val fotoGrupo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { e.cambiarFoto(it, p.canalId) }

    fun preparar(uri: Uri?, nombre: String? = null) {
        if (uri == null) return
        try { pendiente = Archivos.preparar(ctx, uri, nombre) } catch (ex: Exception) { e.aviso.value = ex.message }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { preparar(it) }
    val documento = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { preparar(it) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) preparar(uriFoto, "foto_${System.currentTimeMillis()}.jpg")
    }

    // (v1.0.6) «Guardar en…»: el selector de Android elige dónde (Descargas, Drive…); no hace falta ningún permiso.
    var guardarEste by remember { mutableStateOf<Mensaje?>(null) }
    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        guardarEste?.let { e.guardarAdjunto(it, uri) }; guardarEste = null
    }
    var reenviarEste by remember { mutableStateOf<Mensaje?>(null) }
    var eliminarEste by remember { mutableStateOf<Mensaje?>(null) }
    val portapapeles = LocalClipboardManager.current
    val acciones = AccionesMensaje(
        abrir = { e.abrirAdjunto(it) },
        copiar = { portapapeles.setText(AnnotatedString(it.contenido)); e.aviso.value = "Mensaje copiado" },
        compartir = { if (it.adjunto != null) e.compartirAdjunto(it) else Archivos.compartirTexto(ctx, it.contenido) },
        guardar = { guardarEste = it; guardar.launch(Seguridad.nombreSeguro(it.adjunto?.nombre)) },
        responder = { e.respondiendo.value = it },
        reenviar = { e.cargarUsuarios(); reenviarEste = it },
        eliminar = { eliminarEste = it },
        nombreDe = { e.chat.nombreDe(it) },
        yo = e.chat.username,
    )

    LaunchedEffect(e.mensajes.size) { if (e.mensajes.isNotEmpty()) lista.animateScrollToItem(e.mensajes.size - 1) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Cabecera(p.titulo, subtitulo = if (estado == EstadoConexion.CONECTADO) null else textoEstado(estado), atras = { e.cerrarConversacion() },
            avatar = if (p.canalId == "general") null else {
                { Avatar(if (esGrupo) canal?.avatar else canal?.avatar ?: e.chat.fotoDe(canal?.otroUsuario ?: p.destinatario), p.titulo, 36.dp,
                    icono = if (esGrupo) IconoGrupo else null) }
            }, alPulsar = if (esGrupo) ({ e.verGrupo(p.canalId!!) }) else null) {
            if (esGrupo) IconButton({ fotoGrupo.launch("image/*") }) { Icon(Icons.Default.AddAPhoto, "Cambiar la foto del grupo") }
        }
        if (e.cargando.value && e.mensajes.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Colores.dorado)
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp), state = lista,
            contentPadding = PaddingValues(vertical = 8.dp)) {
            items(e.mensajes, key = { it.id ?: it.hashCode().toLong() }) { m ->
                if (m.tipo == "sistema") {     // (v1.0.10) «👥 Ana ha añadido a Carla»
                    Text("${m.contenido}  ·  ${Fechas.corta(m.creadoEn)}", fontSize = 12.sp, color = Colores.meta,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                    return@items
                }
                Burbuja(m, propio = m.remitente.equals(e.chat.username, true), quien = e.chat.nombreDe(m.remitente),
                    mostrarNombre = !p.canalId.orEmpty().startsWith("dm:"), foto = e.chat.fotoDe(m.remitente), acciones = acciones)
            }
        }
        if (emoticonos) PanelEmoticonos { t -> texto = Emoticonos.anadir(texto, t) }
        // (v1.0.8) «Respondiendo a …» encima de la caja de escribir
        e.respondiendo.value?.let { r ->
            Row(Modifier.fillMaxWidth().background(Colores.caja).padding(start = 12.dp, top = 6.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(32.dp).background(Colores.dorado))
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("Respondiendo a ${if (r.remitente.equals(e.chat.username, true)) "ti" else e.chat.nombreDe(r.remitente)}",
                        fontSize = 12.sp, color = Colores.dorado, fontWeight = FontWeight.Bold)
                    Text(textoCita(r), fontSize = 12.sp, color = Colores.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton({ e.respondiendo.value = null }) { Icon(Icons.Default.Close, "Quitar la respuesta", tint = Colores.meta) }
            }
        }
        Row(Modifier.fillMaxWidth().background(Colores.caja).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ emoticonos = !emoticonos }) { Text("😊", fontSize = 22.sp) }
            Box {
                IconButton({ menuAdjuntar = true }) { Icon(Icons.Default.AttachFile, "Adjuntar", tint = Colores.texto) }
                DropdownMenu(menuAdjuntar, { menuAdjuntar = false }) {
                    DropdownMenuItem({ Text("📷  Hacer una foto") }, {
                        menuAdjuntar = false; uriFoto = Archivos.uriFotoNueva(ctx); camara.launch(uriFoto!!)
                    })
                    DropdownMenuItem({ Text("🖼  Foto de la galería") }, { menuAdjuntar = false; galeria.launch("image/*") })
                    DropdownMenuItem({ Text("📄  Documento") }, { menuAdjuntar = false; documento.launch(arrayOf("*/*")) })
                }
            }
            OutlinedTextField(texto, { texto = it }, placeholder = { Text("Escribe un mensaje") }, modifier = Modifier.weight(1f),
                maxLines = 5, shape = RoundedCornerShape(20.dp))
            IconButton({ val t = texto.trim(); if (t.isNotEmpty()) { e.enviar(t); texto = ""; emoticonos = false } },
                enabled = texto.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = if (texto.isNotBlank()) Colores.propio else Colores.meta)
            }
        }
    }
    e.detalleGrupo.value?.let { d -> InfoGrupo(e, d) }
    // (v1.0.8) «Reenviar a…»: conversaciones que ya existen y compañeros sin privado
    reenviarEste?.let { m ->
        val destinos = remember(e.canales.size, e.usuarios.size) {
            Reenvio.destinos(e.canales, e.usuarios, e.chat.username) { e.chat.nombreDe(it) }
        }
        AlertDialog(onDismissRequest = { reenviarEste = null },
            title = { Text("Reenviar a…") },
            text = {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(destinos) { d ->
                        Text(d.etiqueta, color = Colores.texto, modifier = Modifier.fillMaxWidth()
                            .clickable { reenviarEste = null; e.reenviar(m, d.canalId, d.destinatario) }
                            .padding(vertical = 10.dp))
                    }
                }
            },
            confirmButton = { TextButton({ reenviarEste = null }) { Text("Cancelar") } })
    }
    eliminarEste?.let { m ->
        AlertDialog(onDismissRequest = { eliminarEste = null },
            title = { Text("¿Eliminar el mensaje?") },
            text = { Text("Se elimina para todos y queda «🚫 Mensaje eliminado». No se puede deshacer.") },
            confirmButton = { TextButton({ eliminarEste = null; e.eliminar(m) }) { Text("Eliminar", color = Colores.error) } },
            dismissButton = { TextButton({ eliminarEste = null }) { Text("Cancelar") } })
    }
    pendiente?.let { a ->
        AlertDialog(onDismissRequest = { pendiente = null },
            title = { Text("¿Enviar a «${p.titulo}»?") },
            text = { Text("${a.nombre}  (${a.datos.size / 1024} KB)") },
            confirmButton = { TextButton({ pendiente = null; e.enviarArchivo(a) }) { Text("Enviar") } },
            dismissButton = { TextButton({ pendiente = null }) { Text("Cancelar") } })
    }
}

/** (v1.0.10) Integrantes del grupo y, para quien lo creó, añadir, quitar, renombrar y eliminar. */
@Composable
private fun InfoGrupo(e: EstadoApp, d: DetalleGrupo) {
    var anadir by remember { mutableStateOf(false) }
    var renombrar by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf<String?>(null) }    // "salir" | "eliminar" | "quitar:<usuario>"
    AlertDialog(onDismissRequest = { e.detalleGrupo.value = null },
        title = { Text(d.nombre) },
        text = {
            Column {
                Text("${d.miembros.size} integrantes", color = Colores.meta, fontSize = 13.sp)
                LazyColumn(Modifier.heightIn(max = 320.dp).padding(top = 8.dp)) {
                    items(d.miembros) { m ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(m.avatar, m.nombre, 36.dp)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(m.nombre + if (m.username.equals(e.chat.username, true)) " (tú)" else "", color = Colores.texto)
                                if (m.esCreador) Text("Gestiona el grupo", fontSize = 11.sp, color = Colores.dorado)
                            }
                            if (d.puedeGestionar && !m.username.equals(e.chat.username, true))
                                IconButton({ confirmar = "quitar:${m.username}" }) {
                                    Icon(Icons.Default.PersonRemove, "Quitar a ${m.nombre}", tint = Colores.meta)
                                }
                        }
                    }
                }
                if (d.puedeGestionar) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    OutlinedButton({ anadir = true }) { Text("➕ Añadir") }
                    OutlinedButton({ renombrar = true }) { Text("✏ Renombrar") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    TextButton({ confirmar = "salir" }) { Text("🚪 Salir del grupo", color = Colores.error) }
                    if (d.puedeGestionar) TextButton({ confirmar = "eliminar" }) { Text("🗑 Eliminar", color = Colores.error) }
                }
            }
        },
        confirmButton = { TextButton({ e.detalleGrupo.value = null }) { Text("Cerrar") } })

    if (anadir) {
        val opciones = remember(e.usuarios.size, d.miembros.size) { d.candidatos(e.usuarios) }
        val elegidos = remember { mutableStateListOf<String>() }
        AlertDialog(onDismissRequest = { anadir = false },
            title = { Text("Añadir al grupo") },
            text = {
                if (opciones.isEmpty()) Text("Todos los compañeros están ya en el grupo.", color = Colores.meta)
                else LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(opciones) { u ->
                        Row(Modifier.fillMaxWidth().clickable {
                            if (u.username in elegidos) elegidos.remove(u.username) else elegidos.add(u.username)
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(u.username in elegidos, { v -> if (v) elegidos.add(u.username) else elegidos.remove(u.username) })
                            Text(u.nombre, color = Colores.texto)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ anadir = false; if (elegidos.isNotEmpty()) e.anadirAlGrupo(d.canalId, elegidos.toList()) },
                enabled = elegidos.isNotEmpty()) { Text("Añadir") } },
            dismissButton = { TextButton({ anadir = false }) { Text("Cancelar") } })
    }
    if (renombrar) {
        var nombre by remember { mutableStateOf(d.nombre) }
        AlertDialog(onDismissRequest = { renombrar = false },
            title = { Text("Renombrar el grupo") },
            text = { OutlinedTextField(nombre, { nombre = it }, singleLine = true, label = { Text("Nombre") }) },
            confirmButton = { TextButton({ renombrar = false; e.renombrarGrupo(d.canalId, nombre.trim()) },
                enabled = nombre.isNotBlank()) { Text("Guardar") } },
            dismissButton = { TextButton({ renombrar = false }) { Text("Cancelar") } })
    }
    confirmar?.let { accion ->
        val quien = accion.removePrefix("quitar:")
        val (titulo, texto) = when {
            accion == "salir" -> "¿Salir del grupo?" to "Dejarás de ver sus mensajes."
            accion == "eliminar" -> "¿Eliminar el grupo?" to "Se elimina para todos, con todos sus mensajes y archivos. No se puede deshacer."
            else -> "¿Quitar del grupo?" to "${d.miembros.firstOrNull { it.username == quien }?.nombre ?: quien} dejará de ver el grupo."
        }
        AlertDialog(onDismissRequest = { confirmar = null },
            title = { Text(titulo) }, text = { Text(texto) },
            confirmButton = { TextButton({
                confirmar = null
                when (accion) {
                    "salir" -> e.salirDelGrupo(d.canalId)
                    "eliminar" -> e.eliminarGrupo(d.canalId)
                    else -> e.quitarDelGrupo(d.canalId, quien)
                }
            }) { Text("Sí", color = Colores.error) } },
            dismissButton = { TextButton({ confirmar = null }) { Text("Cancelar") } })
    }
}

/** (v1.0.8) Texto corto de un mensaje para la barra «Respondiendo a…». */
private fun textoCita(m: Mensaje): String =
    m.adjunto?.let { (if (m.esImagen) "📷 " else "📎 ") + it.nombre } ?: m.contenido.replace('\n', ' ')

/** (v1.0.6) Lo que se puede hacer con un mensaje al mantenerlo pulsado. */
private class AccionesMensaje(
    val abrir: (Mensaje) -> Unit,
    val copiar: (Mensaje) -> Unit,
    val compartir: (Mensaje) -> Unit,
    val guardar: (Mensaje) -> Unit,
    // (v1.0.8, chat v1.4.0) responder citando, reenviar y eliminar
    val responder: (Mensaje) -> Unit = {},
    val reenviar: (Mensaje) -> Unit = {},
    val eliminar: (Mensaje) -> Unit = {},
    val nombreDe: (String) -> String = { it },
    val yo: String = "",
)

@Composable
private fun Burbuja(m: Mensaje, propio: Boolean, quien: String, mostrarNombre: Boolean, foto: String? = null, acciones: AccionesMensaje) {
    // (v1.0.4) En grupos y en el General, la foto de quien escribe a la izquierda
    if (!propio && mostrarNombre) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Avatar(foto, quien, 30.dp, modifier = Modifier.padding(end = 6.dp, bottom = 4.dp))
            Box(Modifier.weight(1f)) { BurbujaSola(m, false, quien, true, acciones) }
        }
    } else BurbujaSola(m, propio, quien, mostrarNombre, acciones)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BurbujaSola(m: Mensaje, propio: Boolean, quien: String, mostrarNombre: Boolean, acciones: AccionesMensaje) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalAlignment = if (propio) Alignment.End else Alignment.Start) {
        if (!propio && mostrarNombre) Text(quien, fontSize = 11.sp, color = Colores.meta, modifier = Modifier.padding(start = 6.dp, bottom = 1.dp))
        Box {
            Column(Modifier.widthIn(max = 300.dp)
                .background(if (propio) Colores.propio else Colores.ajeno, RoundedCornerShape(14.dp))
                // (v1.0.6) Un toque abre el adjunto; mantener pulsado abre el menú (copiar, compartir, guardar)
                .combinedClickable(onClick = { if (m.adjunto != null) acciones.abrir(m) },
                    onLongClick = { if (!m.eliminado) menu = true })
                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                // (v1.0.8) «↪ Reenviado» y la cita del mensaje al que responde
                if (m.reenviado) Text("↪ Reenviado", fontSize = 11.sp, color = Colores.texto.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic)
                m.respondeA?.let { c ->
                    Row(Modifier.padding(bottom = 4.dp).background(Colores.texto.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .height(IntrinsicSize.Min)) {
                        Box(Modifier.width(3.dp).fillMaxHeight().background(Colores.dorado))
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(if (c.remitente.equals(acciones.yo, true)) "Tú" else acciones.nombreDe(c.remitente),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Colores.dorado)
                            Text(c.contenido, fontSize = 12.sp, color = Colores.texto.copy(alpha = 0.8f), maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontStyle = if (c.tipo == "eliminado") FontStyle.Italic else FontStyle.Normal)
                        }
                    }
                }
                if (m.eliminado) {
                    Text(m.contenido, color = Colores.texto.copy(alpha = 0.6f), fontSize = 15.sp, fontStyle = FontStyle.Italic)
                } else if (m.adjunto != null) {
                    // (v1.0.6) Las imágenes se ven en miniatura (solo del almacén de adjuntos permitido)
                    if (m.esImagen && Seguridad.urlAdjuntoPermitida(m.adjunto.url)) {
                        AsyncImage(model = m.adjunto.url, contentDescription = m.adjunto.nombre,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.widthIn(max = 240.dp).heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(10.dp)).padding(bottom = 4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (m.esImagen) Icons.Default.Image else Icons.Default.Description, null, tint = Colores.texto)
                        Spacer(Modifier.width(6.dp))
                        Text(m.adjunto.nombre, color = Colores.texto, fontWeight = FontWeight.Medium,
                            style = LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                    }
                } else {
                    Text(m.contenido, color = Colores.texto, fontSize = 15.sp)
                }
                Text(Fechas.corta(m.creadoEn), fontSize = 10.sp, color = Colores.texto.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.End))
            }
            DropdownMenu(menu, { menu = false }) {
                if (m.id != null) {
                    DropdownMenuItem({ Text("↩  Responder") }, { menu = false; acciones.responder(m) })
                    DropdownMenuItem({ Text("↪  Reenviar…") }, { menu = false; acciones.reenviar(m) })
                }
                if (m.adjunto != null) {
                    DropdownMenuItem({ Text("📂  Abrir") }, { menu = false; acciones.abrir(m) })
                    DropdownMenuItem({ Text("💾  Guardar en…") }, { menu = false; acciones.guardar(m) })
                } else {
                    DropdownMenuItem({ Text("📋  Copiar") }, { menu = false; acciones.copiar(m) })
                }
                DropdownMenuItem({ Text("📤  Compartir") }, { menu = false; acciones.compartir(m) })
                if (propio && m.id != null)
                    DropdownMenuItem({ Text("🗑  Eliminar", color = Colores.error) }, { menu = false; acciones.eliminar(m) })
            }
        }
    }
}

@Composable
private fun PanelEmoticonos(elegir: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Colores.fondo).padding(6.dp)) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Emoticonos.lista.forEach { em ->
                Box(Modifier.size(42.dp).clickable { elegir(em) }, contentAlignment = Alignment.Center) { Text(em, fontSize = 24.sp) }
            }
        }
        Text("Respuestas rápidas", fontSize = 11.sp, color = Colores.meta, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Emoticonos.respuestas.forEach { r ->
                AssistChip(onClick = { elegir(r) }, label = { Text(r) })
            }
        }
    }
}

// ── Nueva conversación ────────────────────────────────────────────────────
@Composable
fun PantallaNueva(e: EstadoApp) {
    val elegidos = remember { mutableStateListOf<String>() }
    var nombreGrupo by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { e.cargarUsuarios() }
    BackHandler { e.pantalla.value = Pantalla.Lista }
    Column(Modifier.fillMaxSize()) {
        Cabecera("Nueva conversación", subtitulo = "1 compañero → privado · 2 o más → grupo", atras = { e.pantalla.value = Pantalla.Lista })
        LazyColumn(Modifier.weight(1f)) {
            items(e.usuarios, key = { it.username }) { u ->
                val marcado = u.username in elegidos
                Row(Modifier.fillMaxWidth().clickable { if (marcado) elegidos.remove(u.username) else elegidos.add(u.username) }
                    .padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(marcado, { if (it) elegidos.add(u.username) else elegidos.remove(u.username) })
                    Avatar(u.avatar, u.nombre, 36.dp, modifier = Modifier.padding(end = 10.dp))
                    Column {
                        Text(u.nombre, color = Colores.texto)
                        u.rol?.let { Text(it, fontSize = 11.sp, color = Colores.meta) }
                    }
                }
            }
        }
        Button({
            if (elegidos.size == 1) {
                val u = e.usuarios.first { it.username == elegidos[0] }
                e.abrirConversacion(null, u.username, u.nombre)
            } else if (elegidos.size >= 2) nombreGrupo = ""
        }, enabled = elegidos.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(12.dp).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Colores.propio)) {
            Text(if (elegidos.size >= 2) "Crear grupo (${elegidos.size})" else "Abrir chat")
        }
    }
    nombreGrupo?.let { n ->
        AlertDialog(onDismissRequest = { nombreGrupo = null }, title = { Text("Nombre del grupo") },
            text = { OutlinedTextField(n, { nombreGrupo = it }, singleLine = true) },
            confirmButton = { TextButton({ if (n.isNotBlank()) { nombreGrupo = null; e.crearGrupo(n.trim(), elegidos.toList()) } }) { Text("Crear") } },
            dismissButton = { TextButton({ nombreGrupo = null }) { Text("Cancelar") } })
    }
}

// ── Ajustes ───────────────────────────────────────────────────────────────
@Composable
fun PantallaAjustes(e: EstadoApp) {
    val ctx = LocalContext.current
    var confirmarSalir by remember { mutableStateOf(false) }
    var verNotas by remember { mutableStateOf(false) }
    BackHandler { e.pantalla.value = Pantalla.Lista }
    val pm = ctx.getSystemService(PowerManager::class.java)
    val sinAhorro = pm?.isIgnoringBatteryOptimizations(ctx.packageName) == true
    val fotos by e.chat.fotosCambiadas.collectAsState()
    val miFoto = remember(fotos) { e.chat.miFoto }
    var uriPerfil by remember { mutableStateOf<Uri?>(null) }
    val galeriaPerfil = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { e.cambiarFoto(it) }
    val camaraPerfil = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) e.cambiarFoto(uriPerfil) }
    Column(Modifier.fillMaxSize()) {
        Cabecera("Ajustes", atras = { e.pantalla.value = Pantalla.Lista })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Sesión: ${e.chat.credenciales.nombre ?: ""} (${e.chat.credenciales.usuario ?: ""})", color = Colores.texto)
            HorizontalDivider(color = Colores.caja)
            Text("Mi foto", fontWeight = FontWeight.Bold, color = Colores.dorado)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(miFoto, e.chat.credenciales.nombre ?: "?", 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ galeriaPerfil.launch("image/*") }) { Text("🖼 Galería") }
                        OutlinedButton({ uriPerfil = Archivos.uriFotoNueva(ctx); camaraPerfil.launch(uriPerfil!!) }) { Text("📷 Cámara") }
                    }
                    if (miFoto != null) TextButton({ e.quitarMiFoto() }) { Text("Quitar la foto", color = Colores.error) }
                }
            }
            Text("La ven tus compañeros en el chat del móvil y del Organizador.", color = Colores.meta, fontSize = 12.sp)
            HorizontalDivider(color = Colores.caja)
            Text("Avisos con la app cerrada", fontWeight = FontWeight.Bold, color = Colores.dorado)
            Text("La app mantiene la conexión con un aviso fijo «Chat conectado». Para que el móvil no la corte, quita el ahorro de batería para Chat Dpto. Compras Princess.",
                color = Colores.meta, fontSize = 13.sp)
            OutlinedButton({
                ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)))
            }, enabled = !sinAhorro) { Text(if (sinAhorro) "✓ Ahorro de batería desactivado" else "Quitar el ahorro de batería") }
            HorizontalDivider(color = Colores.caja)
            // (v1.0.6) Al pulsar la versión se ven sus notas (CHANGELOG.md dentro de la .apk)
            Text("Versión ${BuildConfig.VERSION_NAME}  ·  ver novedades", color = Colores.dorado,
                modifier = Modifier.clickable { verNotas = true },
                style = LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
            OutlinedButton({ e.buscarActualizacion(forzar = true) }) { Text("Buscar actualización") }
            HorizontalDivider(color = Colores.caja)
            Button({ confirmarSalir = true }, colors = ButtonDefaults.buttonColors(containerColor = Colores.error)) { Text("Cerrar sesión") }
            Spacer(Modifier.height(20.dp))
            Text("Idea y desarrollo de la aplicación: Víctor Alejandro Martín Andrade, para el Departamento de Central de Compras de Princess Hotels en Canarias.",
                fontSize = 11.sp, color = Colores.meta)
        }
    }
    if (verNotas) {
        val notas = remember {
            val md = try { ctx.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() } } catch (ex: Exception) { "" }
            Notas.de(BuildConfig.VERSION_NAME, md)?.let { Notas.legible(it) }
                ?: "No se han encontrado las notas de esta versión."
        }
        AlertDialog(onDismissRequest = { verNotas = false },
            title = { Text("Novedades de la versión ${BuildConfig.VERSION_NAME}") },
            text = { Text(notas, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton({ verNotas = false }) { Text("Cerrar") } })
    }
    if (confirmarSalir) AlertDialog(onDismissRequest = { confirmarSalir = false },
        title = { Text("Cerrar sesión") },
        text = { Text("Se borran el usuario y la contraseña guardados en este móvil y dejarás de recibir avisos.") },
        confirmButton = { TextButton({ confirmarSalir = false; e.salir() }) { Text("Cerrar sesión") } },
        dismissButton = { TextButton({ confirmarSalir = false }) { Text("Cancelar") } })
}
