# Famyrex — firma de producción

La configuración de Gradle y CI ya está preparada para firmar el APK y el AAB de producción sin guardar ninguna clave privada en el repositorio.

## Secrets necesarios en GitHub

Configurar estos cuatro **Repository secrets**:

- `FAMYREX_KEYSTORE_BASE64` — fichero `.jks`/`.keystore` codificado en Base64.
- `FAMYREX_KEYSTORE_PASSWORD` — contraseña del keystore.
- `FAMYREX_KEY_ALIAS` — alias de la clave de firma.
- `FAMYREX_KEY_PASSWORD` — contraseña de la clave.

No introducir estos valores en código, `gradle.properties`, commits, issues ni conversaciones.

## Flujo de CI

Cuando los cuatro secrets existen, GitHub Actions:

1. reconstruye el keystore únicamente en el runner temporal;
2. ejecuta `assembleRelease` y `bundleRelease` usando esa firma;
3. publica como artefactos `famyrex-release-apk` y `famyrex-release-bundle`.

Si los secrets no existen, el build continúa generando una versión release sin firmar para no bloquear las pruebas técnicas.

## Antes de publicar

Conservar una copia segura del keystore y de sus contraseñas. La misma clave de firma debe conservarse para futuras actualizaciones de la aplicación.

Para Google Play, el AAB firmado debe pasar la validación de Play Console y la prueba interna antes del lanzamiento público.

## Regla de seguridad

**Nunca subir el keystore al repositorio.** Si se pierde la clave de firma utilizada para una aplicación ya publicada, la capacidad de actualizarla puede quedar comprometida.
