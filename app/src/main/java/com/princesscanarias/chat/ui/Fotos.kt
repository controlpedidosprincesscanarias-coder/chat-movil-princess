// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import coil.compose.SubcomposeAsyncImage
import com.princesscanarias.chat.datos.Seguridad
import java.io.ByteArrayOutputStream

/** (v1.0.4) Fotos de perfil de personas y grupos. */
object Fotos {
    private val paleta = listOf(0xFF1F8F63, 0xFF2F6FB5, 0xFFB8913A, 0xFF8E44AD, 0xFFD35400, 0xFF16A085, 0xFFC0392B, 0xFF34495E)

    /** Color fijo para cada nombre (el mismo en todos los móviles). */
    fun color(clave: String): Color = Color(paleta[Math.floorMod(clave.lowercase().hashCode(), paleta.size)])

    /** Iniciales: «Ana Demo» → «AD», «compras» → «C». */
    fun iniciales(nombre: String): String {
        val partes = nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() && it[0].isLetterOrDigit() }
        return when {
            partes.isEmpty() -> "?"
            partes.size == 1 -> partes[0].take(1).uppercase()
            else -> (partes[0].take(1) + partes[1].take(1)).uppercase()
        }
    }

    /** Recorta al centro en cuadrado, reduce a 256 px y la pasa a JPEG (como el Organizador). */
    fun preparar(context: Context, uri: Uri): ByteArray {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("No se pudo leer la foto")
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
        var muestra = 1
        while (minOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= 256) muestra *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = muestra })
            ?: throw IllegalStateException("El archivo no es una imagen")
        val giro = when (ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val lado = minOf(bmp.width, bmp.height)
        val escala = 256f / lado
        val m = Matrix().apply { postScale(escala, escala); postRotate(giro) }
        val cuadrado = Bitmap.createBitmap(bmp, (bmp.width - lado) / 2, (bmp.height - lado) / 2, lado, lado, m, true)
        return ByteArrayOutputStream().also { cuadrado.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
    }
}

/** Foto redonda; si no hay (o no se puede cargar), las iniciales sobre un color, o un icono para los grupos. */
@Composable
fun Avatar(url: String?, nombre: String, tamano: Dp = 44.dp, icono: ImageVector? = null, modifier: Modifier = Modifier) {
    val fondo: @Composable () -> Unit = {
        Box(Modifier.size(tamano).background(Fotos.color(nombre), CircleShape), contentAlignment = Alignment.Center) {
            if (icono != null) Icon(icono, null, tint = Color.White, modifier = Modifier.size(tamano * 0.55f))
            else Text(Fotos.iniciales(nombre), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (tamano.value * 0.38f).sp)
        }
    }
    if (url != null && Seguridad.urlAdjuntoPermitida(url)) {
        SubcomposeAsyncImage(model = url, contentDescription = nombre, contentScale = ContentScale.Crop,
            modifier = modifier.size(tamano).clip(CircleShape), loading = { fondo() }, error = { fondo() })
    } else {
        Box(modifier) { fondo() }
    }
}

val IconoGrupo: ImageVector get() = Icons.Default.Groups
