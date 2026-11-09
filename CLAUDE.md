# CLAUDE.md — Chat Princess (Android)

> **Autoría:** la idea y el desarrollo de la aplicación, incluidas sus pruebas, son de **Víctor Alejandro Martín Andrade**, para el Departamento de Central de Compras de Princess Hotels en Canarias. Ver `AUTORIA.md`.

Instrucciones para Claude Code en este repositorio. Responde siempre en **español**.

## Qué es
App Android (Kotlin + Jetpack Compose, minSdk 26, targetSdk 35) con el chat de Control de Pedidos. Usa el servicio de chat (repo `control-pedidos-chat`) igual que el Organizador de escritorio (`organizador-princess`, `app/services/chat_client.py`). Ver `README.md`.

## Reglas
1. **Contrato con el servidor:** login en `CONTROL_PEDIDOS_URL/api/bridge/login`, cookie reenviada a `CHAT_URL` (REST, rutas `/api/chat/...`, y Socket.IO, eventos `nuevo_mensaje`, `error_chat` y `unirse_canal`). No cambiarlo sin adaptar a la vez el servidor del chat y el Organizador. Siempre a través de los Workers (`Config.kt`), nunca a onrender.com.
2. **Seguridad:**
   - Nunca escribir contraseñas, tokens ni claves en el código: el repositorio es **público** y la `.apk` se puede desmontar.
   - Las credenciales de cada persona van cifradas en su móvil (`Credenciales.kt`).
   - Los adjuntos solo se descargan de `*.supabase.co` por `https` (`Seguridad.kt`).
   - El servidor sanea nombres y tipos.
3. **Actualizaciones:** la app consulta `releases/latest` de este repositorio **sin iniciar sesión**. Por eso:
   - el repositorio nunca puede pasar a privado;
   - no se borran ni se renombran Releases ni etiquetas;
   - la etiqueta es `v<versionName>`, con número **par** al final;
   - cada Release lleva la `.apk` y su `.sha256`.
4. **Firma:** todas las versiones con la misma clave, que está en los secretos `FIRMA_*` del repositorio. Nunca subir un `.jks` ni `firma.properties` (están en `.gitignore`).
5. **Compilación:** solo a mano, con `.github/workflows/compilar.yml` («solo-descarga» o «borrador»). Nunca publicar una Release directamente desde el workflow.
6. **Cada entrega:**
   - `versionName` y `versionCode` en `app/build.gradle.kts`;
   - entrada `# vX.Y.Z` arriba en `CHANGELOG.md`;
   - «Versión actual» del README;
   - entrada en el historial compartido (`docs/HISTORIAL_CAMBIOS.md` de Control Pedidos y `HISTORIAL_CAMBIOS.md` del Organizador, componente «Chat móvil»).
7. **Forma de trabajar:** en rama y con PR; no hacer push a `main` sin confirmación de Víctor. Los errores encontrados se reparan en un commit propio.
8. **Pruebas:** `./gradlew testDebugUnitTest`. `PruebaIntegracion` solo contra servidores **locales** (ver README); nunca contra producción.
9. **Comentarios en Kotlin:** no escribir `/*` dentro de un comentario (por ejemplo «rutas /api/chat/*»): Kotlin anida los comentarios y deja el archivo sin cerrar.
10. **Autoría:** conservar siempre la línea de autoría en documentos y cabeceras, y añadirla en los nuevos.
