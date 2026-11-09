// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.princesscanarias.chat.datos.Config
import com.princesscanarias.chat.datos.Seguridad
import java.io.ByteArrayOutputStream
import java.io.File

data class ArchivoListo(val nombre: String, val mime: String, val datos: ByteArray)

/** Leer, preparar y abrir archivos del chat. */
object Archivos {
    fun nombreDe(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.let { return Seguridad.nombreSeguro(it) }
        }
        return Seguridad.nombreSeguro(uri.lastPathSegment)
    }

    /** Lee el archivo elegido. Las imágenes se reducen a 1600 px y se pasan a JPEG (como en el escritorio). */
    fun preparar(context: Context, uri: Uri, nombreForzado: String? = null): ArchivoListo {
        val nombre = nombreForzado ?: nombreDe(context, uri)
        val mime = context.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(nombre.substringAfterLast('.', "").lowercase())
            ?: "application/octet-stream"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("No se pudo leer el archivo")
        if (mime.startsWith("image/") && !mime.contains("gif")) {
            reducirImagen(bytes)?.let { jpg ->
                val base = nombre.substringBeforeLast('.', nombre)
                return ArchivoListo("$base.jpg", "image/jpeg", jpg)
            }
        }
        if (bytes.size > Config.ADJUNTO_MAX_BYTES) throw IllegalStateException("El archivo pasa de 10 MB.")
        return ArchivoListo(nombre, mime, bytes)
    }

    private fun reducirImagen(bytes: ByteArray): ByteArray? = try {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
        var muestra = 1
        while (maxOf(opciones.outWidth, opciones.outHeight) / (muestra * 2) >= Config.LADO_MAX_IMAGEN) muestra *= 2
        var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = muestra })
        val giro = when (ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val lado = maxOf(bmp.width, bmp.height)
        if (lado > Config.LADO_MAX_IMAGEN || giro != 0f) {
            val escala = minOf(1f, Config.LADO_MAX_IMAGEN.toFloat() / lado)
            val m = Matrix().apply { postScale(escala, escala); postRotate(giro) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, Config.CALIDAD_JPEG, it) }.toByteArray()
    } catch (e: Exception) { null }

    /** Fichero vacío donde la cámara deja la foto. */
    fun uriFotoNueva(context: Context): Uri {
        val dir = File(context.cacheDir, "fotos").apply { mkdirs() }
        val f = File(dir, "foto_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, context.packageName + ".archivos", f)
    }

    /** Guarda un adjunto descargado y lo abre con la app que corresponda. */
    fun abrir(context: Context, nombre: String, mime: String?, datos: ByteArray) {
        val dir = File(context.cacheDir, "adjuntos").apply { mkdirs() }
        val f = File(dir, Seguridad.nombreSeguro(nombre))
        f.writeBytes(datos)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".archivos", f)
        val tipo = mime ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, tipo)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(i) } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No hay ninguna app para abrir este archivo", Toast.LENGTH_LONG).show()
        }
    }
}
