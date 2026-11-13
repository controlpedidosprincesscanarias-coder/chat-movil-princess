# v1.0.14

- **Vaciar un chat:** menú **⋮** de la conversación (o mantener pulsado el chat en la lista) → **🧹 Vaciar chat**. Se borran de tu móvil y de tu Organizador los mensajes que hay ahora; la otra persona los sigue viendo.
- **Eliminar un chat completo** (chats privados): **🗑 Eliminar chat**. Desaparece de tu lista con todos sus mensajes. Si esa persona vuelve a escribirte, el chat aparece de nuevo, solo con lo nuevo. Ella no nota nada.
- **Usuarios desactivados:** si escribes a alguien desactivado en Control Pedidos, sale un aviso de que está **temporalmente desactivado** y el mensaje no se envía (el texto vuelve a la caja de escribir). En la lista y en la conversación se ve «⏸ Desactivado».
- **Usuarios que ya no existen:** si se eliminó de Control Pedidos, el aviso dice que **ya no existe** y ofrece **Eliminar chat**. En la lista se ve «⛔ Ya no existe».
- Necesita el servicio del chat v1.7.0.

# v1.0.12

- **Los avisos de Control Pedidos, en el chat:** los que hasta ahora llegaban por Telegram (pedidos sin entregar, techo de gastos, cambios de estado…) llegan también a una conversación nueva, **«Control Pedidos»**, con su notificación. Cada uno recibe solo los suyos.
- En esa conversación no se puede escribir. Un aviso se puede **reenviar** a un compañero (mantener pulsado → «↪ Reenviar…»).
- Necesita el servicio del chat v1.6.0 y Control Pedidos v12.34.22.

# v1.0.10

- **Grupos:** toca la foto o el nombre de un grupo (arriba) para ver quién está en él, con su foto, y quién lo gestiona.
  - **Quien creó el grupo** puede **➕ Añadir** compañeros, quitarlos (icono junto a cada uno), **✏ Renombrarlo** y **🗑 Eliminarlo** con todos sus mensajes.
  - **Cualquier miembro** puede **🚪 Salir del grupo**.
  - En los grupos de antes de esta versión no consta quién los creó: puede gestionarlos cualquier miembro, y el primero que lo hace queda como gestor.
  - Cada cambio queda anotado dentro del grupo («👥 Ana ha añadido a Carla»). Si te quitan o se elimina el grupo, se cierra solo.
- Necesita el servicio del chat v1.5.0.

# v1.0.8

- **Mantén pulsado un mensaje** y ahora también puedes:
  - **↩ Responder:** sale «Respondiendo a…» encima de la caja de escribir (✕ para quitarlo), y tu respuesta lleva el mensaje citado arriba, como en WhatsApp.
  - **↪ Reenviar…:** eliges la conversación o el compañero. Vale también para fotos y archivos. La copia lleva «↪ Reenviado».
  - **🗑 Eliminar** (solo tus mensajes): pide confirmación y queda «🚫 Mensaje eliminado» para todos, al momento.
- Necesita el servicio del chat v1.4.0.

# v1.0.6

- **Mantén pulsado un mensaje** para ver qué puedes hacer con él:
  - en un mensaje de texto, **Copiar** o **Compartir** (con WhatsApp, el correo…);
  - en un adjunto, **Abrir**, **Guardar en…** (eliges la carpeta: Descargas, Drive…) o **Compartir**.
- **Las imágenes se ven en la conversación**, en miniatura, en vez de solo su nombre. Un toque la abre.
- **Novedades de cada versión:** en Ajustes, pulsa «Versión… · ver novedades».
- **Descarga directa:** desde esta versión, el código QR del Organizador descarga la app sin pasar por la página de GitHub.

# v1.0.4

- **Fotos de perfil** (necesita el servicio del chat v1.3.0):
  - **Dónde se ven:** en la lista de conversaciones, en la cabecera de cada chat, junto a los mensajes de los grupos y del General, y al elegir compañeros.
  - **Sin foto:** quien no tenga sale con sus iniciales sobre un color fijo; los grupos sin foto, con un icono de grupo.
  - **Tu foto:** se pone desde Ajustes → «Mi foto», con la galería o la cámara (se recorta en cuadrado y se reduce sola), y desde ahí se quita.
  - **Foto del grupo:** dentro del grupo, con el botón 📷 de arriba. Cualquier miembro puede cambiarla.
  - **Mismo chat en todas partes:** son las mismas fotos que en el Organizador, y los cambios se ven al momento.

# v1.0.2

- **Nuevo nombre:** «Chat Dpto. Compras Princess» (antes «Chat Princess»), en el icono, la cabecera, la pantalla de acceso y las notificaciones. El archivo de instalación se sigue llamando `ChatPrincess_<versión>.apk`.
- **La corona de Princess** como icono de la app (en blanco sobre el azul marino de las apps de Compras), en la cabecera de la pantalla principal, en la pantalla de acceso y en las notificaciones.
- Es la primera versión que llega como actualización desde la app: al abrir Chat Dpto. Compras Princess 1.0.0 aparece el aviso «Nueva versión 1.0.2».

# v1.0.0

Primera versión de **Chat Dpto. Compras Princess** para Android: el chat de Control de Pedidos en el móvil.

- **Acceso** con el usuario y la contraseña de Control de Pedidos, como el Organizador. Se guardan cifrados en el móvil.
- **Varios dispositivos a la vez**: se puede estar conectado en el Organizador y en el móvil. Los mensajes llegan a los dos, también los que envías tú.
- **Conversaciones**:
  - General, privados y grupos, con los mensajes sin leer;
  - nueva conversación con un compañero o creación de un grupo.
- **Mensajes en tiempo real**, con emoticonos y las mismas respuestas rápidas que el Organizador («👍 Ok», «👌 Perfecto»…).
- **Adjuntos**:
  - envío de fotos de la cámara o de la galería (se reducen solas) y de documentos de hasta 10 MB;
  - un toque en un adjunto lo abre.
- **Avisos con la app cerrada**: un servicio mantiene la conexión con un aviso fijo «Chat conectado». Avisa con sonido de cada mensaje nuevo y vuelve a conectar al encender el móvil.
- **Actualizaciones** desde las Releases de GitHub, comprobando el SHA-256.

---
La idea y el desarrollo de la aplicación, incluidas sus pruebas, son de Víctor Alejandro Martín Andrade, para el Departamento de Central de Compras de Princess Hotels en Canarias.
