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

### 2.1 — Arquitectura familiar conectada en preparación

- Google Sign-In para adultos mediante Firebase Authentication.
- Más de un adulto por familia, cada uno con su propia identidad.
- **Identidad de miembro separada de identidad de dispositivo.**
- La cuenta Google del menor queda contemplada como identidad familiar de primera clase para permitir una integración más profunda con el ecosistema Android/Google cuando las APIs y políticas aplicables lo permitan.
- La cuenta Google no se utiliza como sustituto de los mecanismos técnicos de control de Android: los permisos y capacidades reales dependen de las APIs y autorizaciones disponibles en cada versión/dispositivo.
- La identidad técnica de Firebase puede ser anónima durante el emparejamiento y posteriormente vincularse al dispositivo/membresía sin convertirse en la identidad de la persona.
- Firestore preparado para familias, miembros, dispositivos, invitaciones y alertas cifradas.
- Cloud Functions 2nd gen para emparejamiento seguro.
- App Check / Play Integrity preparado para reducir abuso de los endpoints de emparejamiento.
- FCM preparado para avisos sin transportar contenido sensible.
- Modelo local preparado para políticas de aplicaciones: bloqueo/permiso, límites, aprobación y sincronización de políticas.
- Modelo local preparado para acciones remotas: bloqueo/desbloqueo, tiempo adicional, límites, horarios, aplicaciones, ubicación y política web.

La conexión al proyecto Firebase real y el `google-services.json` siguen siendo el punto externo pendiente de activar la capa conectada.

## Privacidad

La aplicación mantiene el análisis y la generación de señales en el dispositivo siempre que sea posible. La arquitectura online en preparación solo transportará los datos funcionales estrictamente necesarios para sincronizar la familia y entregar alertas estructuradas; las alertas sensibles deberán cifrarse antes de salir del dispositivo infantil.

La política completa está en `docs/PRIVACY_POLICY.md`.

## Desarrollo y validación

El CI ejecuta tests unitarios, validación de Cloud Functions y compilación del APK debug/release. La versión de producción no se considera terminada hasta completar la revisión final y las pruebas en dispositivo físico.
