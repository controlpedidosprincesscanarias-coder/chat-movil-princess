# Chat Dpto. Compras Princess (Android)

> **Autoría:** la idea y el desarrollo de la aplicación, incluidas sus pruebas, son de **Víctor Alejandro Martín Andrade**, para el Departamento de Central de Compras de Princess Hotels en Canarias. Ver `AUTORIA.md`.

**Versión actual: v1.0.6**

El chat de Control de Pedidos en el móvil. Es el mismo chat que el del Organizador de escritorio: las mismas conversaciones, el mismo servidor y la misma cuenta. Se puede estar conectado a la vez en el PC y en el móvil.

## Para quien la usa

1. **Instalar.** Descarga `ChatPrincess_<versión>.apk` de la última Release de este repositorio y ábrela en el móvil. Desde el Organizador (v4.18.24), el botón **📱 Chat en el móvil** del chat muestra un código QR que lleva a esa página, y el capítulo 8 de su Manual de usuario lo explica paso a paso.
   - La primera vez, Android pide permitir «Instalar apps desconocidas» al navegador o a la app de archivos.
   - Si el navegador avisa de que el archivo «puede ser dañino»: «Descargar de todas formas».
   - Si aparece **Google Play Protect** («App no segura», «App desconocida»): «Más detalles» → «Instalar de todas formas» (en algunos móviles, «Instalar sin analizar»). Avisa de cualquier app que no viene de Google Play y pide permisos de segundo plano, arranque y autoinstalación, que esta app necesita para avisar con la app cerrada y actualizarse sola. Puede volver a salir al actualizar.
   - Descargarla solo desde la Release oficial (o el QR del Organizador), nunca de una copia reenviada.
   - Necesita Android 8 o posterior.
2. **Entrar** con el usuario y la contraseña de **Control de Pedidos**.
3. **Permitir las notificaciones** cuando lo pida.
4. En **Ajustes**, pulsar **«Quitar el ahorro de batería»** para que el móvil no corte la conexión. En algunos móviles (Xiaomi, Huawei, Oppo…) hay que permitir además el «inicio automático».
5. Mientras la app tiene la conexión abierta se ve un aviso fijo, **«Chat conectado»**: es lo que permite recibir los mensajes con la app cerrada.

**Actualizaciones:** la app las busca sola al abrirse; también se pueden buscar desde Ajustes → «Buscar actualización». Descarga la versión nueva, comprueba que es la publicada (SHA-256) y abre el instalador.

**Cerrar sesión** (Ajustes) borra el usuario y la contraseña guardados en el móvil y deja de recibir avisos.

## Cómo funciona

| Pieza | Qué hace |
|---|---|
| `datos/ApiChat.kt` | Inicia sesión en Control de Pedidos (`/api/bridge/login`) y lleva esa cookie al servicio del chat (rutas `/api/chat/...`). Si la sesión caduca (cada día), vuelve a entrar sola una vez. Es el mismo contrato que `app/services/chat_client.py` del Organizador. |
| `datos/Chat.kt` | Conexión en tiempo real (Socket.IO, eventos `nuevo_mensaje`, `error_chat` y `unirse_canal`), caché de nombres y estado de la conexión. |
| `datos/Credenciales.kt` | Usuario y contraseña cifrados en el propio móvil (Android Keystore). |
| `servicio/ServicioChat.kt` | Servicio en marcha que mantiene la conexión con la app cerrada y avisa de cada mensaje de otra persona. |
| `servicio/AlArrancar.kt` | Vuelve a conectar al encender el móvil o tras actualizar la app. |
| `datos/Actualizaciones.kt` | Busca la última Release publicada, descarga la `.apk`, comprueba el SHA-256 y abre el instalador. |
| `ui/` | Pantallas (Jetpack Compose): acceso, conversaciones, chat con emoticonos y adjuntos, nueva conversación y ajustes. |

Las direcciones son las de los Workers de Cloudflare (`Config.kt`), nunca las de Render. El código no lleva ninguna clave ni contraseña.

## Compilar y publicar

La compilación se hace en GitHub, en **Actions → «Compilar Chat Dpto. Compras Princess» → Run workflow**:

- **solo-descarga**: deja la `.apk` y su `.sha256` como descarga del propio workflow, para probar.
- **borrador**: además crea una Release en **borrador**. Los móviles no la ven hasta que se pulsa **«Publish release»**.

En cada versión:

- subir `versionName` (terminado en número **par**) y `versionCode` en `app/build.gradle.kts`;
- añadir la entrada `# vX.Y.Z` arriba en `CHANGELOG.md`;
- actualizar la línea «Versión actual» de este README.

**Firma.** Todas las versiones deben firmarse con **la misma clave**: si no, Android no deja instalar una encima de otra. La clave está en los secretos del repositorio (Settings → Secrets and variables → Actions):

- `FIRMA_ALMACEN_B64`: el archivo `.jks` en base64;
- `FIRMA_CLAVE_ALMACEN`;
- `FIRMA_ALIAS`;
- `FIRMA_CLAVE_ALIAS`.

Hay que guardar una copia del `.jks` y de sus claves fuera de GitHub: los secretos no se pueden volver a leer.

**En local** (opcional): Android SDK y JDK 17.

```bash
./gradlew testDebugUnitTest assembleDebug
```

Para la versión firmada en local, se crea un `firma.properties` (no se sube) con `almacen`, `clave_almacen`, `alias` y `clave_alias`.

## Pruebas

- `PruebasLogica`: lectura de los mensajes y canales del servidor, seguridad de los adjuntos (solo `https` del almacén y nombres limpios), comparación de versiones, emoticonos y fechas.
- `PruebaIntegracion`: de punta a punta contra Control de Pedidos y el chat arrancados **en local**, nunca contra producción. Comprueba:
  - el login y el inicio de sesión automático;
  - un privado de un compañero que llega al móvil y al Organizador del mismo usuario;
  - la respuesta desde el móvil que aparece en el escritorio;
  - el historial con emoticonos y la creación de un grupo.

  Se omite si no se dan las variables:

```bash
INTEGRACION_CP_URL=http://127.0.0.1:5081 INTEGRACION_CHAT_URL=http://127.0.0.1:5082 INTEGRACION_CLAVE=... ./gradlew testDebugUnitTest
```

  Los dos servicios se arrancan con la misma `SECRET_KEY` y una base de datos local de pruebas (`tests/huella_bd.py` de Control Pedidos crea los usuarios «admin prueba», «compras prueba» y «hotel prueba»).

## Relación con las otras aplicaciones

- **Servicio del chat** (repo `control-pedidos-chat`): no se ha cambiado nada; la app usa sus rutas y eventos tal cual. No cambiar el contrato (rutas, campos JSON y eventos) sin adaptar a la vez el Organizador y esta app.
- **Organizador** (repo `organizador-princess`): los emoticonos y las respuestas rápidas son los mismos (`app/ui/emoticonos.py`).
- **Historial compartido**: cada cambio se anota, componente «Chat móvil», en `docs/HISTORIAL_CAMBIOS.md` de Control Pedidos y en `HISTORIAL_CAMBIOS.md` del Organizador.
