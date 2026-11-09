// Chat Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
            text = { Text(v.notas.replace(Regex("[#*`]"), "").trim().take(600).ifBlank { "Hay una versión nueva de Chat Princess." }) },
            confirmButton = { TextButton({ e.actualizacion.value = null; e.instalarActualizacion(v) }) { Text("Descargar e instalar") } },
            dismissButton = { TextButton({ e.actualizacion.value = null }) { Text("Más tarde") } },
        )
    }
}

@Composable
private fun Cabecera(titulo: String, subtitulo: String? = null, atras: (() -> Unit)? = null, corona: Boolean = false,
                     acciones: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (corona) {   // la corona de Princess en la pantalla principal
                    Image(painterResource(R.drawable.corona_blanca), contentDescription = "Princess",
                        modifier = Modifier.height(30.dp).padding(end = 10.dp))
                }
                Column {
                    Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
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
        Text("Chat Princess", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Colores.texto)
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
    Column(Modifier.fillMaxSize()) {
        Cabecera("Chat Princess", subtitulo = textoEstado(estado), corona = true) {
            IconButton({ e.cargarCanales() }) { Icon(Icons.Default.Refresh, "Actualizar") }
            IconButton({ e.pantalla.value = Pantalla.Ajustes }) { Icon(Icons.Default.Settings, "Ajustes") }
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(e.canales, key = { it.id }) { c ->
                    val titulo = e.titulo(c)
                    Row(Modifier.fillMaxWidth().clickable { e.abrirConversacion(c.id, null, titulo) }
                        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(if (c.tipo == "general") Colores.cabecera else Colores.ajeno, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(when (c.tipo) { "general" -> Icons.Default.Forum; "grupo" -> Icons.Default.Groups; else -> Icons.Default.Person },
                                null, tint = Colores.texto)
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
    BackHandler { e.cerrarConversacion() }

    fun preparar(uri: Uri?, nombre: String? = null) {
        if (uri == null) return
        try { pendiente = Archivos.preparar(ctx, uri, nombre) } catch (ex: Exception) { e.aviso.value = ex.message }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { preparar(it) }
    val documento = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { preparar(it) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) preparar(uriFoto, "foto_${System.currentTimeMillis()}.jpg")
    }

    LaunchedEffect(e.mensajes.size) { if (e.mensajes.isNotEmpty()) lista.animateScrollToItem(e.mensajes.size - 1) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Cabecera(p.titulo, subtitulo = if (estado == EstadoConexion.CONECTADO) null else textoEstado(estado), atras = { e.cerrarConversacion() })
        if (e.cargando.value && e.mensajes.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Colores.dorado)
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp), state = lista,
            contentPadding = PaddingValues(vertical = 8.dp)) {
            items(e.mensajes, key = { it.id ?: it.hashCode().toLong() }) { m ->
                Burbuja(m, propio = m.remitente.equals(e.chat.username, true), quien = e.chat.nombreDe(m.remitente),
                    mostrarNombre = !p.canalId.orEmpty().startsWith("dm:")) { e.abrirAdjunto(m) }
            }
        }
        if (emoticonos) PanelEmoticonos { t -> texto = Emoticonos.anadir(texto, t) }
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
    pendiente?.let { a ->
        AlertDialog(onDismissRequest = { pendiente = null },
            title = { Text("¿Enviar a «${p.titulo}»?") },
            text = { Text("${a.nombre}  (${a.datos.size / 1024} KB)") },
            confirmButton = { TextButton({ pendiente = null; e.enviarArchivo(a) }) { Text("Enviar") } },
            dismissButton = { TextButton({ pendiente = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun Burbuja(m: Mensaje, propio: Boolean, quien: String, mostrarNombre: Boolean, abrirAdjunto: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalAlignment = if (propio) Alignment.End else Alignment.Start) {
        if (!propio && mostrarNombre) Text(quien, fontSize = 11.sp, color = Colores.meta, modifier = Modifier.padding(start = 6.dp, bottom = 1.dp))
        Column(Modifier.widthIn(max = 300.dp)
            .background(if (propio) Colores.propio else Colores.ajeno, RoundedCornerShape(14.dp))
            .then(if (m.adjunto != null) Modifier.clickable { abrirAdjunto() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (m.adjunto != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (m.tipo == "imagen") Icons.Default.Image else Icons.Default.Description, null, tint = Colores.texto)
                    Spacer(Modifier.width(6.dp))
                    Text(m.adjunto.nombre, color = Colores.texto, fontWeight = FontWeight.Medium,
                        style = LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                }
            } else {
                Text(m.contenido, color = Colores.texto, fontSize = 15.sp)
            }
            Text(Fechas.corta(m.creadoEn), fontSize = 10.sp, color = Colores.texto.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.End))
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
    BackHandler { e.pantalla.value = Pantalla.Lista }
    val pm = ctx.getSystemService(PowerManager::class.java)
    val sinAhorro = pm?.isIgnoringBatteryOptimizations(ctx.packageName) == true
    Column(Modifier.fillMaxSize()) {
        Cabecera("Ajustes", atras = { e.pantalla.value = Pantalla.Lista })
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Sesión: ${e.chat.credenciales.nombre ?: ""} (${e.chat.credenciales.usuario ?: ""})", color = Colores.texto)
            HorizontalDivider(color = Colores.caja)
            Text("Avisos con la app cerrada", fontWeight = FontWeight.Bold, color = Colores.dorado)
            Text("La app mantiene la conexión con un aviso fijo «Chat conectado». Para que el móvil no la corte, quita el ahorro de batería para Chat Princess.",
                color = Colores.meta, fontSize = 13.sp)
            OutlinedButton({
                ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)))
            }, enabled = !sinAhorro) { Text(if (sinAhorro) "✓ Ahorro de batería desactivado" else "Quitar el ahorro de batería") }
            HorizontalDivider(color = Colores.caja)
            Text("Versión ${BuildConfig.VERSION_NAME}", color = Colores.texto)
            OutlinedButton({ e.buscarActualizacion(forzar = true) }) { Text("Buscar actualización") }
            HorizontalDivider(color = Colores.caja)
            Button({ confirmarSalir = true }, colors = ButtonDefaults.buttonColors(containerColor = Colores.error)) { Text("Cerrar sesión") }
            Spacer(Modifier.height(20.dp))
            Text("Idea y desarrollo de la aplicación: Víctor Alejandro Martín Andrade, para el Departamento de Central de Compras de Princess Hotels en Canarias.",
                fontSize = 11.sp, color = Colores.meta)
        }
    }
    if (confirmarSalir) AlertDialog(onDismissRequest = { confirmarSalir = false },
        title = { Text("Cerrar sesión") },
        text = { Text("Se borran el usuario y la contraseña guardados en este móvil y dejarás de recibir avisos.") },
        confirmButton = { TextButton({ confirmarSalir = false; e.salir() }) { Text("Cerrar sesión") } },
        dismissButton = { TextButton({ confirmarSalir = false }) { Text("Cancelar") } })
}
