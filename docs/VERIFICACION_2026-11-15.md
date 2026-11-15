# Verificación del prototipo de eventos

Se compiló la app Android y se ejecutaron las pruebas de la lógica compartida en el host Android:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest
```

Resultado: compilación correcta y cinco pruebas aprobadas, incluidas tres nuevas que comprueban los dos sentidos de la simulación, correlación de IDs, limpieza y rechazo del servidor como origen de cliente. El APK de depuración queda en `androidApp/build/outputs/apk/debug/`.

Se añadió una validación en `FakeEventRepository` para aceptar solo `APP` o `WEB` como origen. Las pruebas iOS ARM no se ejecutaron: esta computadora es Intel y la configuración del proyecto apunta al simulador ARM. No se verificó la pantalla en un emulador durante esta revisión.

La consola sigue siendo una simulación en memoria. No se ha probado HTTPS real ni comunicación con la web porque este repositorio no incluye la plataforma o el backend integrados y no se dispone de un contrato o endpoint real. Para completar el objetivo se necesitan esos componentes, acordar la acción y el contrato, implementar el transporte y comprobar el recorrido completo de ambos sentidos.
