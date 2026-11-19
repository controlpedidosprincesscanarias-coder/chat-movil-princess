// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.princesscanarias.chat.datos.Enlaces
import com.princesscanarias.chat.datos.Mensaje
import com.princesscanarias.chat.datos.Seguridad
import com.princesscanarias.chat.datos.TarjetaEnlace

/*
 * (v1.0.20, chat v1.9.0) Petición de Víctor (19 nov 2026): poder ver los vídeos que se envían por el chat.
 *  - Los enlaces de los mensajes se pueden pulsar (se abren en su app o en el navegador).
 *  - Bajo un enlace de TikTok, YouTube o Instagram sale una tarjeta con miniatura, título y autor.
 *  - Un vídeo adjunto se reproduce dentro del chat al pulsarlo (Media3), nunca solo.
 */

/** Abre un enlace http/https con la app que lo tenga (TikTok, YouTube…) o el navegador. */
fun abrirEnlace(context: Context, url: String) {
    val u = Uri.parse(url)
    if (u.scheme?.lowercase() !in setOf("http", "https")) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) { /* sin navegador: no hay nada que hacer */ }
}

/** El texto del mensaje con los enlaces subrayados y pulsables. */
@Composable
fun TextoConEnlaces(texto: String, color: Color, fontSize: Int = 15) {
    val ctx = LocalContext.current
    val trozos = remember(texto) { Enlaces.trozos(texto) }
    if (trozos.none { it.url != null }) {
        Text(texto, color = color, fontSize = fontSize.sp)
        return
    }
    val anotado = remember(texto, color) {
        buildAnnotatedString {
            for (t in trozos) {
                val url = t.url
                if (url == null) append(t.texto)
                else withLink(LinkAnnotation.Url(url,
                    TextLinkStyles(SpanStyle(color = Colores.dorado, textDecoration = TextDecoration.Underline))) {
                    abrirEnlace(ctx, url)
                }) { append(t.texto) }
            }
        }
    }
    Text(anotado, color = color, fontSize = fontSize.sp)
}

/** Tarjeta del primer enlace de vídeo del mensaje. Mientras llega (o si el servidor no la da), solo el sitio. */
@Composable
fun TarjetaVideoWeb(url: String, tarjeta: TarjetaEnlace?, pedir: (String) -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(url) { pedir(url) }
    val sitio = tarjeta?.sitio ?: when (Enlaces.sitioDeVideo(url)) {
        "tiktok" -> "TikTok"; "youtube" -> "YouTube"; "instagram" -> "Instagram"; else -> "Vídeo"
    }
    val imagen = remember(tarjeta?.miniatura) {
        tarjeta?.bytesMiniatura()?.let { b -> BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }
    }
    Column(Modifier.padding(top = 6.dp).widthIn(max = 260.dp).clip(RoundedCornerShape(10.dp))
        .background(Colores.texto.copy(alpha = 0.08f)).clickable { abrirEnlace(ctx, url) }) {
        Box(Modifier.fillMaxWidth().heightIn(min = 70.dp).background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center) {
            if (imagen != null) Image(imagen, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp))
            Box(Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, "Abrir el vídeo", tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(sitio, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Colores.dorado)
            tarjeta?.titulo?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 13.sp, color = Colores.texto, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            tarjeta?.autor?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 11.sp, color = Colores.texto.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Vídeo adjunto en la burbuja: ▶ con el nombre; al pulsarlo se abre el reproductor dentro del chat. */
@Composable
fun BurbujaVideo(m: Mensaje, tamano: String?) {
    val adj = m.adjunto ?: return
    var viendo by remember { mutableStateOf(false) }
    Column(Modifier.widthIn(max = 240.dp).clip(RoundedCornerShape(10.dp)).clickable { viendo = true }) {
        Box(Modifier.size(width = 240.dp, height = 135.dp).background(Color.Black), contentAlignment = Alignment.Center) {
            Box(Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, "Ver el vídeo", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Movie, null, tint = Colores.texto, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(adj.nombre + (tamano?.let { " · $it" } ?: ""), color = Colores.texto, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (viendo) ReproductorVideo(adj.url) { viendo = false }
}

/** Reproductor a pantalla completa dentro de la app (solo vídeos del almacén de adjuntos). */
@Composable
fun ReproductorVideo(url: String, cerrar: () -> Unit) {
    val ctx = LocalContext.current
    if (!Seguridad.urlAdjuntoPermitida(url)) { LaunchedEffect(Unit) { cerrar() }; return }
    val reproductor = remember(url) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true          // empieza al abrirlo, que es cuando el usuario lo ha pedido
        }
    }
    DisposableEffect(reproductor) { onDispose { reproductor.release() } }
    Dialog(onDismissRequest = cerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView({ PlayerView(it).apply { player = reproductor } }, Modifier.fillMaxSize())
            IconButton(cerrar, Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
            }
        }
    }
}

/** «1,2 MB» / «850 KB». */
fun tamanoLegible(bytes: Long?): String? {
    val b = bytes ?: return null
    return if (b >= 1024 * 1024) String.format(java.util.Locale("es", "ES"), "%.1f MB", b / 1048576.0) else "${(b + 1023) / 1024} KB"
}
