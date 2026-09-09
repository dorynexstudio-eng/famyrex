# Política de privacidad de Famyrex

**Última actualización: 10 de septiembre de 2026**

Famyrex es una aplicación de protección y bienestar familiar diseñada con un principio de privacidad por defecto. La mayor parte de la información se procesa y almacena localmente en el dispositivo.

## 1. Responsable y contacto

Aplicación: **Famyrex**  
Desarrollador: **Dorynex Studio**  
Contacto de privacidad: el canal de contacto publicado por el desarrollador en Google Play.

## 2. Qué información puede utilizar Famyrex

Según las funciones que el adulto active y los permisos que conceda, Famyrex puede procesar localmente:

- Datos de uso de aplicaciones obtenidos mediante Android UsageStats, como tiempo de uso y aplicaciones observadas.
- Estado de determinadas protecciones del dispositivo y permisos necesarios para que funcionen.
- Alertas y eventos generados por las reglas locales de protección.
- Información de vinculación familiar y perfiles locales.
- Configuración de límites, horarios, aplicaciones restringidas y preferencias de protección.
- Zonas familiares y eventos de geovalla cuando el usuario activa la función de ubicación.
- Observaciones de notificaciones cuando el usuario activa expresamente el servicio de análisis de comunicación. Famyrex no presenta esta función como lectura secreta de chats ni atribuye intenciones o culpabilidad.
- Historial y resúmenes locales utilizados para informes, tendencias y explicaciones.
- Navegación realizada dentro del navegador web integrado de Famyrex cuando el usuario utiliza esa función.

Cuando un dispositivo infantil está vinculado y la familia activa la función correspondiente, Famyrex también puede enviar a Firebase/Firestore la **última ubicación conocida del dispositivo infantil** (latitud, longitud, precisión aproximada y momento de captura) para que el adulto autorizado pueda verla y abrirla en Google Maps. Se mantiene únicamente el último punto; Famyrex no crea un historial remoto de recorridos.

Famyrex no solicita ni pretende obtener contraseñas, contenido privado de chats mediante técnicas ocultas, micrófono, cámara o identificadores persistentes del dispositivo para fines publicitarios.

## 3. Dónde se procesan los datos

Los datos funcionales de Famyrex se almacenan localmente siempre que la función lo permite. Para determinadas funciones de vinculación y supervisión, Famyrex utiliza Firebase/Firestore como servicio de infraestructura para transportar y almacenar de forma restringida el estado necesario entre los dispositivos de una misma familia.

La ubicación sincronizada se limita al último punto conocido del dispositivo infantil y está protegida por las reglas de acceso de la familia: el dispositivo infantil puede escribir su propio punto y el adulto autorizado puede leerlo.

La función de navegación segura constituye otra excepción técnica: el WebView puede comunicarse con Internet para cargar las páginas solicitadas y, cuando está disponible, utilizar Safe Browsing para comprobar amenazas conocidas.

## 4. Para qué se utilizan los datos

Los datos se utilizan exclusivamente para prestar las funciones activadas por el usuario: control parental, bienestar digital, alertas, explicaciones, informes, geozonas, protección web, asistencia local y localización familiar.

Las evaluaciones de riesgo son deterministas y explicables. Un nivel de riesgo representa una señal de uso o protección y no constituye un diagnóstico médico, psicológico ni una acusación de conducta ilícita.

## 5. Compartición y publicidad

Famyrex no vende datos personales, no utiliza publicidad personalizada y no crea perfiles publicitarios a partir de los datos de protección familiar.

Los datos de ubicación sincronizados y el estado necesario para la vinculación pueden pasar por Firebase/Firestore para permitir la función de familia conectada. No se utilizan para publicidad.

## 6. Conservación y eliminación

Los datos locales permanecen en el dispositivo mientras sean necesarios para las funciones configuradas o hasta que el usuario los elimine, restablezca la aplicación o desinstale Famyrex.

El último punto de ubicación sincronizado se sustituye cuando el dispositivo infantil publica una nueva ubicación y deja de estar disponible cuando se elimina el documento remoto de la familia. Los datos de infraestructura de Firebase pueden estar sujetos a sus propias políticas y retenciones técnicas.

## 7. Permisos

Famyrex solicita permisos solo para funciones que los necesitan. El usuario puede revocar permisos desde Android. Algunas protecciones pueden quedar en estado **⚪ Datos insuficientes** cuando los permisos o datos necesarios no están disponibles; Famyrex no presenta esa situación como si todo estuviera correcto.

## 8. Seguridad

Los secretos de vinculación familiar se protegen mediante almacenamiento cifrado basado en Android Keystore. El acceso a la ubicación sincronizada está limitado mediante reglas de seguridad de Firestore a los miembros autorizados de la familia.

## 9. Menores

Famyrex está diseñado para ayudar a familias a proteger y acompañar a menores, sin vigilancia oculta. Las funciones de supervisión requieren activación y permisos visibles. La aplicación evita presentar inferencias como hechos y diferencia entre evidencia suficiente y ausencia de datos.

## 10. Cambios en esta política

Si cambia de forma material la forma en que Famyrex trata los datos, esta política se actualizará y se indicará una nueva fecha de actualización.

## 11. Resumen claro

**Famyrex está diseñado para proteger, explicar y acompañar; no para espiar.** El funcionamiento principal es local, no incluye publicidad personalizada y la localización conectada conserva únicamente el último punto necesario para que el adulto autorizado pueda encontrar al dispositivo infantil.
