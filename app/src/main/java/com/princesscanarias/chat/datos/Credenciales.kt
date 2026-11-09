// Chat Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Usuario y contraseña de Control de Pedidos, guardados CIFRADOS en el propio móvil (Android
 *  Keystore), igual que el Organizador los guarda cifrados con DPAPI en cada PC. Hacen falta para
 *  volver a iniciar sesión solo cuando caduca la sesión diaria. «Cerrar sesión» los borra. */
class Credenciales(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context, "credenciales_cifradas",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    val usuario: String? get() = prefs.getString("usuario", null)
    val clave: String? get() = prefs.getString("clave", null)
    val nombre: String? get() = prefs.getString("nombre", null)
    val hayCredenciales: Boolean get() = !usuario.isNullOrBlank() && !clave.isNullOrBlank()

    fun guardar(usuario: String, clave: String, nombre: String) {
        prefs.edit().putString("usuario", usuario).putString("clave", clave).putString("nombre", nombre).apply()
    }

    fun borrar() = prefs.edit().clear().apply()
}
