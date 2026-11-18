// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.princesscanarias.chat.datos.Mensaje
import com.princesscanarias.chat.datos.Voz
import kotlinx.coroutines.delay
import java.io.File

/**
 * (v1.0.16, chat v1.8.0) Mensajes de voz.
 * - [Grabadora]: graba en .m4a (AAC, mono) en la caché de la app.
 * - [Reproductor]: un solo audio a la vez; la burbuja que suena muestra ⏸ y su avance.
 * - [BotonMicrofono]: mantener pulsado para grabar, soltar para enviar, deslizar a la izquierda para cancelar.
 * - [BurbujaVoz]: ▶/⏸, barra de avance y duración.
 * La lógica sin Android (duración, cuándo se envía o se cancela) está en datos/Voz.kt.
 */
class Grabadora(private val context: Context) {
    private var grabadora: MediaRecorder? = null
    private var archivo: File? = null
    private var inicio = 0L

    val grabando: Boolean get() = grabadora != null
    fun transcurridoMs(): Long = if (grabando) System.currentTimeMillis() - inicio else 0L

    /** Empieza a grabar. false si el micrófono no está disponible (otra app lo usa, sin permiso…). */
    fun empezar(): Boolean {
        if (grabando) return true
        val dir = File(context.cacheDir, "voz_grabada").apply { mkdirs() }
        val f = File(dir, Voz.nombreArchivo(System.currentTimeMillis()))
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(44100)
            r.setAudioEncodingBitRate(48000)      // ~360 KB por minuto
            r.setMaxDuration(Voz.MAX_MS.toInt())
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            grabadora = r; archivo = f; inicio = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            r.release(); f.delete()
            false
        }
    }

    /** Para la grabación. Devuelve el archivo y su duración, o null si no se pudo guardar nada. */
    fun terminar(): Pair<File, Long>? {
        val r = grabadora ?: return null
        val f = archivo
        val ms = transcurridoMs().coerceAtMost(Voz.MAX_MS)
        grabadora = null; archivo = null
        val ok = try { r.stop(); true } catch (e: Exception) { false }   // stop() falla si duró casi nada
        r.release()
        if (!ok || f == null || f.length() == 0L) { f?.delete(); return null }
        return f to ms
    }

    fun cancelar() { terminar()?.first?.delete() }
}

/** Un solo audio a la vez para toda la app. */
object Reproductor {
    var sonando by mutableStateOf<Long?>(null)        // id del mensaje que suena (o está en pausa)
        private set
    var enPausa by mutableStateOf(false)
        private set
    var posicionMs by mutableStateOf(0L)
        private set
    private var mp: MediaPlayer? = null

    fun alternar(id: Long, archivo: File) {
        val actual = mp
        if (sonando == id && actual != null) {
            if (actual.isPlaying) { actual.pause(); enPausa = true } else { actual.start(); enPausa = false }
            return
        }
        parar()
        try {
            val nuevo = MediaPlayer().apply {
                setDataSource(archivo.absolutePath)
                setOnCompletionListener { parar() }
                prepare()
                start()
            }
            mp = nuevo; sonando = id; enPausa = false; posicionMs = 0L
        } catch (e: Exception) {
            parar()
        }
    }

    /** La llama la burbuja que suena, unas veces por segundo, para mover la barra. */
    fun actualizarPosicion() { mp?.let { if (it.isPlaying) posicionMs = it.currentPosition.toLong() } }

    fun parar() {
        mp?.let { try { it.stop() } catch (_: Exception) { }; it.release() }
        mp = null; sonando = null; enPausa = false; posicionMs = 0L
    }
}

/**
 * Botón 🎤: mantener pulsado para grabar. Mientras graba, [alCambiar] recibe (ms transcurridos, cancelará al soltar)
 * para pintar el aviso en lugar de la caja de escribir; con null, ya no graba.
 */
@Composable
fun BotonMicrofono(
    grabadora: Grabadora,
    tienePermiso: () -> Boolean,
    pedirPermiso: () -> Unit,
    alCambiar: (Pair<Long, Boolean>?) -> Unit,
    alTerminar: (File, Long) -> Unit,
    alAvisar: (String) -> Unit,
) {
    val densidad = LocalDensity.current
    var activo by remember { mutableStateOf(false) }
    var desplazamientoDp by remember { mutableFloatStateOf(0f) }

    // Reloj de la grabación y límite de 3 minutos (al llegar, se envía sola)
    LaunchedEffect(activo) {
        while (activo) {
            val ms = grabadora.transcurridoMs()
            if (ms >= Voz.MAX_MS) {
                activo = false
                grabadora.terminar()?.let { (f, d) -> alTerminar(f, d) }
                alCambiar(null)
                alAvisar("Se ha enviado: un mensaje de voz dura como mucho ${Voz.textoDuracion(Voz.MAX_MS)}")
                break
            }
            alCambiar(ms to (desplazamientoDp <= -Voz.CANCELAR_DP))
            delay(200)
        }
    }
    DisposableEffect(Unit) { onDispose { if (grabadora.grabando) grabadora.cancelar() } }

    Box(
        Modifier.size(48.dp).background(if (activo) Colores.error else Colores.propio, CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val abajo = awaitFirstDown()
                    if (!tienePermiso()) { pedirPermiso(); return@awaitEachGesture }
                    if (!grabadora.empezar()) { alAvisar("No se puede usar el micrófono ahora"); return@awaitEachGesture }
                    desplazamientoDp = 0f
                    activo = true
                    var pulsado = true
                    while (pulsado) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == abajo.id } ?: break
                        desplazamientoDp = with(densidad) { (c.position.x - abajo.position.x).toDp().value }
                        pulsado = c.pressed
                        c.consume()
                    }
                    if (!activo) return@awaitEachGesture          // ya se envió al llegar a 3 minutos
                    activo = false
                    val ms = grabadora.transcurridoMs()
                    when (Voz.alSoltar(ms, desplazamientoDp)) {
                        Voz.AlSoltar.ENVIAR -> grabadora.terminar()?.let { (f, d) -> alTerminar(f, d) }
                            ?: alAvisar("No se pudo grabar el mensaje de voz")
                        Voz.AlSoltar.DEMASIADO_CORTO -> { grabadora.cancelar(); alAvisar("Mantén pulsado el micrófono para grabar") }
                        Voz.AlSoltar.CANCELADO -> { grabadora.cancelar(); alAvisar("Mensaje de voz cancelado") }
                    }
                    alCambiar(null)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Mic, "Mantén pulsado para grabar un mensaje de voz", tint = Colores.texto)
    }
}

/** Lo que se ve en lugar de la caja de escribir mientras se graba. */
@Composable
fun AvisoGrabando(ms: Long, cancelara: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🔴", fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(Voz.textoDuracion(ms), color = Colores.texto, fontSize = 16.sp)
        Spacer(Modifier.width(14.dp))
        Text(if (cancelara) "Suelta para cancelar" else "‹ Desliza para cancelar",
            color = if (cancelara) Colores.error else Colores.meta, fontSize = 13.sp)
    }
}

/** Contenido de la burbuja de un mensaje de voz. [audio] descarga (una vez) el archivo y lo pasa al reproductor. */
@Composable
fun BurbujaVoz(m: Mensaje, audio: (Mensaje, (File?) -> Unit) -> Unit) {
    val id = m.id ?: -1L
    val suena = Reproductor.sonando == id
    var cargando by remember(id) { mutableStateOf(false) }
    LaunchedEffect(suena, Reproductor.enPausa) {
        while (suena && !Reproductor.enPausa) { Reproductor.actualizarPosicion(); delay(200) }
    }
    val duracion = m.adjunto?.duracionMs
    Row(Modifier.widthIn(min = 200.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton({
            if (cargando) return@IconButton
            cargando = true
            audio(m) { f -> cargando = false; if (f != null) Reproductor.alternar(id, f) }
        }) {
            if (cargando) CircularProgressIndicator(Modifier.size(20.dp), color = Colores.texto, strokeWidth = 2.dp)
            else Icon(if (suena && !Reproductor.enPausa) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (suena && !Reproductor.enPausa) "Pausar" else "Escuchar", tint = Colores.texto)
        }
        Column(Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { if (suena) Voz.avance(Reproductor.posicionMs, duracion) else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp), color = Colores.dorado,
                trackColor = Colores.texto.copy(alpha = 0.25f))
            Spacer(Modifier.height(4.dp))
            Text("🎤 " + if (suena) "${Voz.textoDuracion(Reproductor.posicionMs)} / ${Voz.textoDuracion(duracion)}"
                         else Voz.textoDuracion(duracion), fontSize = 12.sp, color = Colores.texto.copy(alpha = 0.8f))
        }
    }
}
