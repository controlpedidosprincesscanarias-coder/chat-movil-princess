// Chat Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat

import android.app.Application
import com.princesscanarias.chat.servicio.Avisos

class ChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Avisos.crearCanales(this)
    }
}
