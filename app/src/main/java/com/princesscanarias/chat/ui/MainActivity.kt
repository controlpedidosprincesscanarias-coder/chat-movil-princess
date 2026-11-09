// Chat Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.princesscanarias.chat.datos.Chat
import com.princesscanarias.chat.servicio.Avisos
import com.princesscanarias.chat.servicio.ServicioChat

class MainActivity : ComponentActivity() {
    private val estado: EstadoApp by viewModels()
    private val pedirAvisos = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) pedirAvisos.launch(Manifest.permission.POST_NOTIFICATIONS)
        abrirDesdeAviso(intent)
        setContent { TemaChat { AppChat(estado) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        abrirDesdeAviso(intent)
    }

    /** Al tocar el aviso de un mensaje se abre esa conversación. */
    private fun abrirDesdeAviso(intent: Intent?) {
        val canal = intent?.getStringExtra(Avisos.EXTRA_CANAL) ?: return
        estado.abrirConversacion(canal, null, intent.getStringExtra(Avisos.EXTRA_TITULO) ?: "Chat")
        intent.removeExtra(Avisos.EXTRA_CANAL)
    }

    override fun onResume() {
        super.onResume()
        val chat = Chat.de(this)
        chat.appEnPrimerPlano = true
        if (chat.credenciales.hayCredenciales) {
            ServicioChat.arrancar(this)
            estado.alVolver()
        }
    }

    override fun onPause() {
        Chat.de(this).appEnPrimerPlano = false
        super.onPause()
    }
}
