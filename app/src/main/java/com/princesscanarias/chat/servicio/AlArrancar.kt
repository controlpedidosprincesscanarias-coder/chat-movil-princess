// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.servicio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Al encender el móvil (o tras actualizar la app) vuelve a conectar el chat si hay sesión guardada. */
class AlArrancar : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ServicioChat.arrancar(context)
        }
    }
}
