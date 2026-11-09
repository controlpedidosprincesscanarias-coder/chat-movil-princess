// Chat Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class VersionNueva(val version: String, val notas: String, val urlApk: String, val urlSha256: String?, val tamano: Long)

/**
 * Actualizaciones desde las Releases PÚBLICAS de GitHub (como el Organizador): se consulta la última
 * versión publicada, se descarga la .apk, se comprueba su SHA-256 si la Release lo publica y se abre el
 * instalador de Android. Los borradores no se ven: solo lo que se publica con «Publish release».
 */
object Actualizaciones {
    private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()

    fun buscar(versionActual: String): VersionNueva? {
        val req = Request.Builder().url("https://api.github.com/repos/${Config.REPO_RELEASES}/releases/latest")
            .header("Accept", "application/vnd.github+json").header("User-Agent", "ChatPrincessAndroid").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val o = JSONObject(r.body?.string().orEmpty())
            val tag = o.texto("tag_name") ?: return null
            if (!Seguridad.versionMayor(tag, versionActual)) return null
            val assets = o.optJSONArray("assets") ?: return null
            var apk: JSONObject? = null; var sha: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val n = a.optString("name")
                if (n.endsWith(".apk")) apk = a
                if (n.endsWith(".apk.sha256")) sha = a.texto("browser_download_url")
            }
            apk ?: return null
            return VersionNueva(tag.removePrefix("v"), o.texto("body") ?: "", apk.optString("browser_download_url"), sha, apk.optLong("size"))
        }
    }

    /** Descarga y comprueba la .apk. Devuelve el archivo listo para instalar. */
    fun descargar(context: Context, v: VersionNueva): File {
        val dir = File(context.cacheDir, "actualizaciones").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val destino = File(dir, "ChatPrincess_${v.version}.apk")
        http.newCall(Request.Builder().url(v.urlApk).header("User-Agent", "ChatPrincessAndroid").build()).execute().use { r ->
            if (!r.isSuccessful) throw ErrorChat("No se pudo descargar la actualización (${r.code})")
            destino.outputStream().use { out -> r.body!!.byteStream().copyTo(out) }
        }
        v.urlSha256?.let { url ->
            val esperado = http.newCall(Request.Builder().url(url).header("User-Agent", "ChatPrincessAndroid").build())
                .execute().use { it.body?.string().orEmpty() }.trim().split(Regex("\\s+")).firstOrNull()?.lowercase()
            val real = MessageDigest.getInstance("SHA-256").digest(destino.readBytes()).joinToString("") { "%02x".format(it) }
            if (esperado.isNullOrBlank() || esperado != real) {
                destino.delete()
                throw ErrorChat("La descarga no coincide con la publicada (SHA-256). Vuelve a intentarlo.")
            }
        }
        return destino
    }

    /** Abre el instalador. Si Android aún no deja instalar desde esta app, abre ese ajuste. */
    fun instalar(context: Context, apk: File) {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            throw ErrorChat("Permite «Instalar apps desconocidas» para Chat Princess y vuelve a pulsar Instalar.")
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".archivos", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
