// Chat Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
package com.princesscanarias.chat.datos

/** Direcciones y constantes compartidas. Son las mismas que usa el Organizador de escritorio
 *  (app/services/chat_client.py): el login va a Control de Pedidos y el chat a su propio servicio,
 *  los dos a través de sus Workers de Cloudflare. No hay ninguna clave en el código. */
object Config {
    /** Servicio principal: solo para iniciar sesión (/api/bridge/login). */
    const val CONTROL_PEDIDOS_URL = "https://proxy.controlpedidosprincess-canarias.workers.dev"

    /** Servicio del chat: REST (rutas /api/chat/...) y Socket.IO. */
    const val CHAT_URL = "https://proxy-chat.controlpedidosprincess-canarias.workers.dev"

    /** Repositorio PÚBLICO con las Releases de la app (actualizaciones). */
    const val REPO_RELEASES = "controlpedidosprincesscanarias-coder/chat-movil-princess"

    const val USER_AGENT = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Mobile Safari/537.36 ChatPrincessAndroid"

    /** Como el servidor (ADJUNTO_MAX_BYTES): 10 MB por archivo. */
    const val ADJUNTO_MAX_BYTES = 10 * 1024 * 1024
    const val LADO_MAX_IMAGEN = 1600
    const val CALIDAD_JPEG = 85

    /** Los adjuntos solo se descargan de este almacén (igual que en el escritorio). */
    val HOSTS_ADJUNTOS = listOf(".supabase.co")
}
