# Famyrex — configuración Firebase

Esta carpeta contiene la infraestructura online que se activa cuando se conecta el proyecto Firebase real de Famyrex.

## Arquitectura

- **Adulto:** Google Sign-In → Firebase Authentication.
- **Segundo adulto:** su propia cuenta Google → invitación a la misma familia.
- **Dispositivo infantil:** identidad Firebase anónima invisible + código Famyrex; no necesita cuenta Google.
- **Firestore:** familias, miembros, dispositivos, invitaciones y alertas cifradas.
- **Cloud Functions 2nd gen:** creación y redención segura de invitaciones.
- **App Check / Play Integrity:** capa adicional contra abuso de los endpoints de emparejamiento.
- **FCM:** preparado para avisos; el contenido sensible no debe viajar en la notificación push.

## Antes de activar la conexión real

1. Crear/seleccionar el proyecto Firebase de Famyrex.
2. Registrar Android `com.famyrex.app`.
3. Añadir SHA-1 y SHA-256 de las firmas utilizadas por Famyrex.
4. Descargar el `google-services.json` actualizado y colocarlo en `app/`.
5. Aplicar el plugin `com.google.gms.google-services` en `app/build.gradle.kts`.
6. Habilitar **Google** en Firebase Authentication.
7. Habilitar **Anonymous** para los dispositivos infantiles.
8. Crear Cloud Firestore.
9. Registrar Famyrex en App Check con Play Integrity.
10. Desplegar `firebase/firestore.rules`, índices y Functions.

## Seguridad de emparejamiento

La invitación genera un token aleatorio de alta entropía y un código visible de 6 dígitos. El código nunca se considera suficiente para acceder directamente a Firestore. Las Functions aplican autenticación, App Check, expiración, uso único y limitación de intentos.

El token crudo solo se entrega al dispositivo adulto que creó la invitación; nunca se almacena en Firestore. Está reservado para la autorización fuerte de la vinculación y futuros flujos QR/deep-link.

## Datos sensibles

Las alertas que lleguen a Firestore deberán contener únicamente ciphertext y metadatos mínimos de entrega. No se debe subir texto bruto de notificaciones, mensajes, contactos o conversaciones.

## Coste

La arquitectura usa servicios gestionados de Firebase. La autenticación/Firestore/App Check/FCM deben revisarse con las cuotas y precios vigentes del proyecto antes de publicar. Cloud Functions requiere un proyecto configurado para su despliegue según las condiciones actuales de Firebase.
