// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.princesscanarias.chat.datos.Manual

/** (v1.0.20) Ajustes → 📖 Manual de uso: apartados que se abren y cierran, buscador y enlace al PDF completo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaManual(e: EstadoApp) {
    val ctx = LocalContext.current
    var consulta by remember { mutableStateOf("") }
    val abiertos = remember { mutableStateMapOf<String, Boolean>() }
    val resultado = remember(consulta) { Manual.buscar(consulta) }
    BackHandler { e.pantalla.value = Pantalla.Ajustes }
    Column(Modifier.fillMaxSize().background(Colores.fondo)) {
        TopAppBar(title = { Text("Manual de uso") },
            navigationIcon = { IconButton({ e.pantalla.value = Pantalla.Ajustes }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Colores.cabecera, titleContentColor = Colores.texto,
                navigationIconContentColor = Colores.texto))
        OutlinedTextField(consulta, { consulta = it }, singleLine = true, placeholder = { Text("Buscar en el manual") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (consulta.isNotEmpty()) IconButton({ consulta = "" }) { Icon(Icons.Default.Close, "Borrar la búsqueda") } },
            modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(20.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (resultado.isEmpty()) item {
                Text("No hay nada sobre «$consulta». Prueba con otra palabra o consulta el manual completo.",
                    color = Colores.meta, modifier = Modifier.padding(8.dp))
            }
            items(resultado, key = { it.titulo }) { a ->
                // Con una búsqueda escrita, los apartados encontrados salen abiertos
                val abierto = abiertos[a.titulo] ?: consulta.isNotBlank()
                Column(Modifier.fillMaxWidth().background(Colores.caja, RoundedCornerShape(12.dp))
                    .clickable { abiertos[a.titulo] = !abierto }.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.icono, fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(a.titulo, color = Colores.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Icon(if (abierto) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Colores.meta)
                    }
                    if (abierto) {
                        Spacer(Modifier.height(8.dp))
                        a.lineas.forEach { l ->
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Text("•", color = Colores.dorado, modifier = Modifier.padding(end = 8.dp))
                                Text(l, color = Colores.texto, fontSize = 15.sp, lineHeight = 21.sp)
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton({
                        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Manual.URL_PDF))) }
                        catch (ex: Exception) { e.aviso.value = "No hay ninguna app para abrir el enlace" }
                    }) { Text("📄 Manual completo (PDF)") }
                    Text("Es el manual de usuario del Organizador; el chat en el móvil está en el capítulo 8. Necesita conexión.",
                        color = Colores.meta, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp))
                }
            }
        }
    }
}
