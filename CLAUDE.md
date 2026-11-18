# CLAUDE.md — Chat Dpto. Compras Princess (Android)

> **Autoría:** la idea y el desarrollo de la aplicación, incluidas sus pruebas, son de **Víctor Alejandro Martín Andrade**, para el Departamento de Central de Compras de Princess Hotels en Canarias. Ver `AUTORIA.md`.

Instrucciones para Claude Code en este repositorio. Responde siempre en **español**.

## Qué es
App Android (Kotlin + Jetpack Compose, minSdk 26, targetSdk 35) con el chat de Control de Pedidos. Usa el servicio de chat (repo `control-pedidos-chat`) igual que el Organizador de escritorio (`organizador-princess`, `app/services/chat_client.py`). Ver `README.md`.

## Forma de trabajar con Víctor (común a todas las aplicaciones)
> Esta sección es **la misma en los siete `CLAUDE.md`**: Control Pedidos, chat (`control-pedidos-chat`), chat móvil (`chat-movil-princess`), Organizador, Catálogo Asignaciones (`dali-sap-articulos-app`), Activo Operacional y Consola Registro Info. Si cambia, se cambia igual en todos (decisión de Víctor, 18 nov 2026). Lo propio de cada aplicación está en el resto de este archivo y manda sobre lo general.

**Las aplicaciones y cómo se publican** (todo el acceso público, a través de los Workers de Cloudflare; nunca directo a onrender.com):

| Repositorio | Qué es | Cómo llega a producción |
|---|---|---|
| `control-pedidos-princess` | Control Pedidos (web, Flask) | Render, al fusionar en `main` |
| `control-pedidos-chat` | Servidor del chat | Render, al fusionar en `main` |
| `dali-sap-articulos-app` | Catálogo Asignaciones (web) | Render (`autoDeploy`), al fusionar; migraciones SQL **a mano** |
| `activo-operacional-princess` | Activo Operacional (web) | Render (`autoDeploy`), al fusionar; migraciones SQL **a mano** |
| `organizador-princess` | Organizador (Windows) | Workflow `compilar.yml` a mano → Release en **borrador** → Víctor pulsa «Publish release» |
| `consola-registro-info` | Consola Registro Info (Windows) | Igual que el Organizador |
| `chat-movil-princess` | Chat en el móvil (Android) | Igual que el Organizador |

**Al empezar un hilo:** leer este archivo y el de los repositorios que toque. Si la tarea afecta a otra aplicación (contrato del chat o del puente, base de datos compartida, historial compartido), añadir ese repositorio al hilo en vez de suponer cómo está. Responder siempre en **español**, claro y sin jerga: Víctor no siempre ve los comandos ni su salida.

**Cómo se trabaja una petición:**
1. Investigar en el código real antes de proponer. Lo que solo puede decidir Víctor (un criterio de negocio, qué manda sobre qué) se le pregunta con opciones y una recomendada; lo demás se decide y se dice.
2. Rama nueva desde `main` al día, commits pequeños con la versión en el mensaje y cada error encontrado en su **propio commit**.
3. Cada entrega lleva, sin esperar a que se pida: versión (con la regla de numeración de cada repositorio), CHANGELOG, historial, README, pruebas al día y, si cambia una pantalla que sale en un manual, **regenerar el manual** con el generador del repositorio (`docs/manuales_fuente/` o el que diga su apartado), siempre con **datos inventados**.
4. **Historial compartido:** el mismo texto en `docs/HISTORIAL_CAMBIOS.md` de Control Pedidos y en `HISTORIAL_CAMBIOS.md` del Organizador (entrada nueva arriba, `## AAAA-MM-DD HH:MM — [Componente] Título (versión)`), aunque la entrega sea de otra aplicación: entonces hay PR también en esos dos repositorios. Comprobar que quedan idénticos.
5. Pruebas siempre en **local** (nunca contra Supabase, Render ni Control de Pedidos de producción). Decir siempre qué se ha probado y qué no (y qué queda por probar en Windows, en el móvil o con datos reales).
6. Abrir la PR con `gh api repos/controlpedidosprincesscanarias-coder/<repo>/pulls`. Cuerpo: qué cambia (con la petición de Víctor), «Probado» y «No probado», y las dependencias con otras PR o aplicaciones. Citar siempre las PR como enlace completo (`owner/repo#n`).
7. Mantener informado: una frase cuando un trabajo largo va por la mitad. Al terminar, resumen corto: qué cambia, enlace a la PR, qué queda por probar y qué tiene que hacer Víctor (aplicar una migración, lanzar una compilación, publicar una Release, comprobar un despliegue).

**Qué quiere decir Víctor con…**
- «Sí», «adelante», «prepara la PR»: hacer el trabajo y abrir la PR, **sin fusionar**.
- «Fusionar» / «fusionar todo»: fusionar las PR abiertas de lo hablado («todo»: todas las del hilo). Antes, comprobar `mergeable_state` (si sale `unknown`, esperar y volver a mirar); método `merge`; en orden de dependencia (la que contiene a otra, después; el servidor antes que los clientes). Después, decir qué se despliega solo y qué hay que compilar o publicar.
- «Regenerar»: los manuales afectados, con su generador.
- Un log o una captura de Render, GitHub, Cloudflare o cron-job.org: diagnosticarlo y decirle el paso concreto que tiene que dar en esa pantalla.

**Nunca sin que Víctor lo confirme:** push a `main`; publicar una Release (los workflows solo crean borradores) o borrar o renombrar Releases y etiquetas; cambiar datos de producción (y entonces idempotente, avisado y con ⚠️ en el historial); credenciales, tokens o correos de cuentas en el código, commits o documentos.

**Después de fusionar:** las apps de Render se despliegan solas (pedir que confirme «Live», o revisar el log si lo pega). Las de Windows y Android esperan a que Víctor lance la compilación y publique el borrador. Recordar las migraciones SQL que haya que aplicar a mano y anotar lo que quede pendiente (pruebas del lunes, logs que va a mandar…) en el apartado de pendientes del repositorio.

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
