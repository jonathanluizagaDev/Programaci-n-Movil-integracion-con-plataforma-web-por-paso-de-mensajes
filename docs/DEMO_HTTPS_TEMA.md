# Demostración HTTPS: tema claro y oscuro

## Qué hace

Android y la página web pueden cambiar el modo claro/oscuro. Cada cambio envía `POST /api/events` por HTTPS. El servidor responde `202` con `requestId` y `eventId`, guarda el estado en memoria y los clientes consultan `GET /api/state` periódicamente. El destinatario aplica el tema y envía `POST /api/ack`; el servidor responde `200` y marca el evento `processed`. Si se elige otro tema antes de confirmar, la orden anterior pasa a `superseded`. No se usa WebSocket.

El servidor registra la solicitud, el código de respuesta y la confirmación con ambos IDs. Las pantallas muestran el estado y los eventos. Si el receptor no está abierto, el evento queda `accepted` hasta que se conecte o sea reemplazado.

## Preparación y ejecución

Requisitos: Python 3, OpenSSL, Android Studio con SDK y un emulador Android. Desde la raíz del repositorio:

```sh
./demo-web/setup-cert.sh
python3 -u demo-web/server.py
```

Deja la terminal abierta. El certificado y la clave se crean solo en `demo-web/certs/` y no se suben al repositorio. El script copia el certificado público al recurso **debug** de Android; ejecutarlo antes de la primera compilación o después de cambiar el certificado. Si se cambia, hay que recompilar e instalar nuevamente la app. No se debe usar este certificado para producción.

Abre `https://localhost:8443/` en el navegador de la misma computadora. El certificado es autofirmado, por lo que el navegador mostrará un aviso. Para esta demo local puedes usar la opción avanzada del navegador para continuar. Alternativamente puedes instalar de forma explícita el certificado local en el almacén de confianza del equipo. El servidor solo escucha en `127.0.0.1`.

En Android Studio abre la raíz del repositorio, selecciona `androidApp` y ejecuta la versión debug en el emulador. `10.0.2.2` es la dirección de la computadora anfitriona desde el emulador Android. Un teléfono físico necesita otra dirección, certificado y configuración de red; esta versión está preparada para el emulador local. Android debug confía en el certificado generado; release no contiene esta excepción.

## Prueba frente al docente

1. Con servidor, web y app abiertos, pulsa **Modo oscuro** en Android. La web debe ponerse oscura en unos segundos.
2. Comprueba que el evento pasa de `accepted` a `processed` y que el servidor muestra `REQUEST`, `RESPONSE 202` y `ACK ... RESPONSE 200` con los mismos IDs.
3. Pulsa **Modo claro** en la web. La app debe cambiar a claro y confirmar el evento.
4. Detén el servidor para ver el error de conexión. Al reiniciarlo, los clientes vuelven a consultar automáticamente.

Al reiniciar el servidor se pierden el estado y el historial. La demo admite una app y una página; no tiene cuentas, persistencia ni control de acceso. La implementación iOS conserva el prototipo simulado compartido; el cliente HTTPS implementado es Android. La configuración actual es de desarrollo local, no para exponer el servidor en internet.

## Archivos y cambios

- `demo-web/server.py`: servidor HTTPS, validación de JSON, creación de eventos, estado, registros y confirmaciones.
- `demo-web/index.html`: botones, aplicación del tema, consulta periódica y confirmación de eventos Android.
- `demo-web/setup-cert.sh`: genera el certificado local y lo copia al recurso debug Android.
- `demo-web/test_server.py`: pruebas HTTPS de respuestas, validaciones, confirmación e idempotencia.
- `androidApp/src/main/kotlin/org/ucb/appp1/MainActivity.kt`: pantalla Android real, solicitudes HTTPS en un hilo, consultas periódicas, cambio de tema y confirmación.
- `androidApp/src/main/AndroidManifest.xml`: permiso de internet.
- `androidApp/src/debug/AndroidManifest.xml` y `androidApp/src/debug/res/xml/demo_network_security.xml`: confianza del certificado local únicamente en debug.
- `androidApp/build.gradle.kts`: dependencias Compose para la nueva pantalla Android.
- `.gitignore`: excluye certificados y clave generados localmente.

El antiguo prototipo compartido sigue en `shared` y se usa en iOS. Se conserva para evitar presentar esa plataforma como integrada sin haber implementado y probado su cliente de red.

## Pruebas realizadas

- `./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest`: compilación Android y pruebas compartidas correctas.
- `python3 -m unittest discover -s demo-web -p 'test_*.py'`: pruebas HTTPS del servidor correctas.
- Emulador Android: servidor recibe una orden creada en la app; navegador Chrome ejecutado en modo headless recibe y confirma el evento. En el sentido inverso se envió una orden HTTPS con origen web, Android cambió el tema y confirmó. El clic de los botones de la página web no se automatizó.
- No se ejecutaron pruebas iOS: el simulador configurado es ARM y la computadora es Intel.
