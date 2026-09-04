# Famyrex MVP 0.2

Base nativa Android para Famyrex, orientada a protección familiar transparente.

## Incluye
- Dashboard de protección.
- Perfil de adulto y perfil protegido.
- Generación y almacenamiento local de código de vinculación.
- Pantalla de alertas con lenguaje no diagnóstico.
- Base preparada para incorporar funciones autorizadas de Android.

## Próximo módulo
Uso de aplicaciones mediante UsageStatsManager, con explicación y activación explícita del permiso por parte del adulto.

## Nota
El proyecto no implementa vigilancia oculta, lectura secreta de chats de terceros ni grabación clandestina.

## MVP 1.0 — Configuración y puntuación de riesgo
- Umbral diario configurable.
- Horario nocturno configurable.
- Umbral de uso nocturno configurable.
- Umbral de aumento por aplicación configurable.
- Sensibilidad de evaluación (baja/normal/alta).
- Puntuación 0–100 con niveles NORMAL / ATTENTION / ELEVATED / IMPORTANT.
- Cada evaluación conserva las razones principales que la provocaron.
- La detección nocturna usa deltas entre snapshots acumulativos para evitar sumar varias veces el mismo uso.
- El nivel representa riesgo de uso digital, no un diagnóstico psicológico o médico.

## MVP 1.1 — Bienestar digital
- Objetivo diario configurable.
- Seguimiento del progreso del objetivo.
- Detección conservadora de pausas a partir de huecos entre muestras.
- Resumen de uso nocturno.
- Recomendaciones de bienestar basadas en señales de uso.
- No diagnostica estados psicológicos ni afirma actividad continua cuando los datos no la demuestran.

## MVP 1.2 — Vinculación familiar y dispositivos
- Perfil local de propietario/familia.
- Estructura para múltiples dispositivos.
- Estados UNLINKED / PENDING / LINKED.
- Código temporal de 6 dígitos con caducidad.
- Validación local del código y consumo de un solo uso.
- Preparado para sincronización segura posterior.
- Esta versión no afirma que dos dispositivos estén sincronizados: la vinculación remota real se implementará cuando añadamos el backend/sincronización.

## MVP 1.3 — Localización y zonas familiares
- Permisos de ubicación aproximada/precisa y preparación para segundo plano.
- Zonas familiares persistentes.
- Geofencing mediante la API oficial de Android/Google Play Services.
- Eventos de entrada/salida almacenados localmente.
- Radio mínimo conservador de 100 m al registrar una zona.
- La sincronización con otros dispositivos y las notificaciones remotas quedan para fases posteriores.

## MVP 1.4 — Seguridad del dispositivo
- Estado de seguridad local del dispositivo.
- Detección de bloqueo de pantalla seguro.
- Comprobación del acceso a estadísticas de uso.
- Comprobación de permisos de ubicación cuando existen zonas familiares.
- Inventario básico del número de aplicaciones visibles para Famyrex.
- Persistencia del último diagnóstico local.
- Preparado para actualizarse periódicamente con WorkManager.
- No intenta rootear, ocultarse, desactivar protecciones ni inspeccionar datos privados de otras aplicaciones.
- La presencia de aplicaciones potencialmente peligrosas o el estado de Play Protect se reservará para la integración oficial de Play Integrity/Play Console cuando Famyrex disponga de backend.

## MVP 1.5 — Seguridad web
- Motor local de listas permitidas/bloqueadas por dominio.
- Configuración persistente de protección web.
- WebView segura con JavaScript desactivado por defecto.
- Bloqueo de ventanas múltiples y contenido mixto.
- Integración con Google Safe Browsing de WebView para amenazas conocidas.
- No intercepta el navegador externo ni mensajes privados de otras apps.
- La clasificación integral por categorías (adulto, apuestas, etc.) requerirá una fuente de categorías fiable; no se inventan clasificaciones locales.

## MVP 1.6 — Análisis inteligente local
- Resumen automático de tendencias de uso.
- Detección explicable de cambios respecto al historial.
- Identificación de concentración de uso por aplicación.
- Priorización de señales ya detectadas por Famyrex.
- Nivel de confianza para cada observación.
- Análisis local y determinista, sin API de pago.
- No lee conversaciones privadas de otras aplicaciones.
- No intenta inferir emociones, intenciones ni diagnósticos psicológicos.
- Preparado para añadir un modelo de IA más avanzado cuando exista una fuente de datos/infraestructura adecuada.

## MVP 1.7 — Asistente familiar local
- Asistente de preguntas y respuestas sobre datos disponibles en Famyrex.
- Consultas sobre uso diario, aplicaciones, tendencias, alertas y bienestar.
- Respuestas basadas exclusivamente en datos locales disponibles.
- Sin lectura secreta de conversaciones ni acceso a contenido privado de otras aplicaciones.
- Preparado para evolucionar a un modelo de IA más avanzado en una fase posterior.

## MVP 1.8 — Informes
- Informes diario, semanal y mensual.
- Total y promedio de uso.
- Día de mayor uso.
- Aplicaciones con mayor consumo de tiempo.
- Comparación con el periodo anterior.
- Recuento de alertas e importantes.
- Narrativa automática explicable.
- Persistencia local.
- Generación mediante WorkManager.
- Sin envío de datos a servidores.
