// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

/**
 * (v1.0.6) Notas de una versión, para verlas al pulsar «Versión x.y.z» en Ajustes. Salen del CHANGELOG.md del
 * repositorio, que se copia dentro de la .apk al compilar (assets/CHANGELOG.md, ver build.gradle.kts), así que
 * se ven sin Internet. Sin Android: se prueba en PruebasLogica.
 */
object Notas {
    private val cabecera = Regex("""^#\s+v?(\d+(?:\.\d+)+)\s*$""")

    private fun numeros(v: String) = Regex("\\d+").findAll(v).map { it.value.toInt() }.toList()

    /** El apartado de `version` (desde «# vX.Y.Z» hasta el siguiente «# v…»), o null si no está. */
    fun de(version: String, texto: String): String? {
        val buscada = numeros(version)
        if (buscada.isEmpty()) return null
        val lineas = texto.lines()
        var inicio = -1
        var fin = lineas.size
        for ((i, l) in lineas.withIndex()) {
            val m = cabecera.find(l.trim()) ?: continue
            if (inicio >= 0) { fin = i; break }
            if (numeros(m.groupValues[1]) == buscada) inicio = i
        }
        if (inicio < 0) return null
        return lineas.subList(inicio + 1, fin).joinToString("\n").trim().ifEmpty { null }
    }

    /** Markdown sencillo a texto para leer en el móvil: sin «**», «`» ni almohadillas. */
    fun legible(md: String): String = md.lines().joinToString("\n") { l ->
        l.replace("**", "").replace("`", "").replace(Regex("^#{1,6}\\s+"), "").replace(Regex("^(\\s*)- "), "$1• ")
    }.trim()
}
