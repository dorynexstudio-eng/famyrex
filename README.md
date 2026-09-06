# Famyrex

Aplicación Android de protección y bienestar familiar, orientada a ayudar a las familias a comprender y gestionar el uso digital de sus dispositivos de forma segura, transparente y responsable.

## Principios del producto

- Protección visible y proporcional.
- Privacidad por defecto y procesamiento local siempre que sea posible.
- **⚪ Datos insuficientes** es distinto de “todo bien”.
- Las señales no se presentan como diagnósticos, culpabilidad ni intenciones.
- La supervisión no debe convertirse en vigilancia oculta.
- El menor no se presupone víctima ni responsable: las señales se interpretan con contexto.

## Funciones implementadas

### 1.0–1.4 — Protección familiar

- Dashboard de protección.
- Perfil de adulto y perfil protegido.
- Vinculación familiar local con código temporal de 6 dígitos.
- Control de tiempo de pantalla, horarios y restricciones.
- Alertas con ciclo de vida revisable.
- UsageStats y trabajos periódicos con WorkManager.
- Localización y geozonas mediante APIs oficiales.
- Diagnóstico local de estado de protección.
- Recuperación segura: Famyrex, Ajustes de Android y launcher no quedan atrapados por un bloqueo.

### 1.5 — Seguridad web

- Motor local de listas permitidas/bloqueadas por dominio.
- Reglas de bloqueo con límites de dominio correctos.
- WebView endurecido: JavaScript y acceso a archivos desactivados por defecto, sin ventanas múltiples ni contenido mixto.
- Safe Browsing cuando está disponible.
- Navegación no web tratada como advertencia.
- No intercepta el navegador externo ni mensajes de otras aplicaciones.

### 1.6 — Inteligencia familiar explicable

- Tendencias de uso.
- Detección conservadora de anomalías.
- Evidencia estructurada para cada explicación.
- Priorización de datos insuficientes y señales de riesgo.
- Lenguaje neutral que no asigna intenciones ni culpabilidad.
- Recomendaciones vinculadas a señales reales y a su ciclo de vida.

### 1.7 — Asistente familiar local

- Consultas sobre uso, alertas, tendencias y bienestar.
- Respuestas basadas en datos disponibles localmente.
- Sin lectura secreta de conversaciones privadas.

### 1.8 — Informes

- Informes diario, semanal y mensual.
- Uso total y promedio.
- Día de mayor uso y aplicaciones con mayor consumo.
- Comparación con periodos anteriores.
- Recuento de alertas.
- Narrativa automática explicable.
- Corrección para alertas almacenadas con fecha y hora.

### 2.0 — Producto final en preparación

- Target Android 16 / API 36.
- Política de privacidad visible dentro de la aplicación.
- Política de privacidad versionada en `docs/PRIVACY_POLICY.md`.
- Checklist de publicación y Data safety en `docs/PLAY_STORE_CHECKLIST.md`.
- Revisión final de permisos, UX, rendimiento, compatibilidad y publicación pendiente antes del lanzamiento.

## Privacidad

Famyrex no dispone de un backend propio en esta versión y no utiliza publicidad personalizada. El funcionamiento principal es local. Las funciones de navegación web pueden comunicarse con Internet y Safe Browsing; la ubicación puede utilizar Android/Google Play Services para las funciones de geofencing.

La política completa está en `docs/PRIVACY_POLICY.md`.

## Desarrollo y validación

El CI ejecuta tests unitarios y compilación del APK debug. La versión de producción no se considera terminada hasta completar la revisión final y las pruebas en dispositivo físico.
