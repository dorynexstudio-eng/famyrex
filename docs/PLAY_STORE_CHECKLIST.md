# Famyrex — checklist de publicación en Google Play

## Estado técnico

- [x] `targetSdk = 36`.
- [x] `compileSdk = 36`.
- [x] `minSdk = 26`.
- [x] Versión de producto 2.0.0 / versionCode 20.
- [x] CI ejecuta tests unitarios y compilación debug.
- [x] No hay backend propio de Famyrex.
- [x] No hay publicidad personalizada.
- [x] Política de privacidad redactada y versionada en `docs/PRIVACY_POLICY.md`.

## Declaración de datos en Play Console

La declaración final debe reflejar exactamente la versión que se publique y todos los SDK incluidos. Revisar especialmente:

- Datos de uso de aplicaciones y dispositivo utilizados por las funciones de protección.
- Ubicación cuando el usuario activa geozonas.
- Inventario de aplicaciones necesario para determinadas funciones de protección.
- Datos derivados de notificaciones si se activa el análisis de comunicación.
- Navegación web dentro del WebView y servicios de Safe Browsing.
- Que Famyrex no vende datos ni utiliza publicidad personalizada.
- Que el procesamiento funcional se realiza principalmente en el dispositivo.

## Familias y menores

Revisar en Play Console la audiencia objetivo y las declaraciones de Families antes de publicar. No afirmar que Famyrex es una app de entretenimiento infantil: su finalidad es protección familiar y supervisión transparente.

## Privacidad

La política definitiva debe publicarse en una URL HTTPS pública, estable y no editable, y la misma información debe mantenerse coherente con la sección Data safety de Play Console.

## Acceso sensible

Preparar las explicaciones y vídeos/capturas requeridos por Play Console para:

- UsageStats.
- AccessibilityService.
- NotificationListenerService.
- Ubicación y geofencing.

Cada permiso debe corresponder a una función visible y demostrable dentro de la aplicación.

## Lanzamiento

- [ ] Completar ficha de Play Console.
- [ ] Publicar la política de privacidad en URL pública.
- [ ] Completar Data safety.
- [ ] Completar Audience / Families.
- [ ] Añadir capturas reales de la versión final.
- [ ] Ejecutar prueba interna.
- [ ] Instalar y probar el AAB firmado en un dispositivo físico.
- [ ] Revisar todos los permisos después de instalación limpia.
- [ ] Revisar recuperación de límites, Ajustes y launcher.
- [ ] Verificar navegación web y Safe Browsing.
- [ ] Verificar informes y alertas.
- [ ] Solo después: preparar publicación de producción.
