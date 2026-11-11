# Guía para completar el trabajo práctico

La base incluye reglas iniciales de dominio, puertos, casos de uso de borrador,
solicitud y aceptación, pruebas unitarias y el esquema Flyway inicial. No
reemplaza el trabajo requerido por la consigna ni conecta todavía la UI con MySQL.

## Hito 1 · Diseño

- Revisar `docs/consigna/` junto con el PDF oficial.
- Convertir el modelo de `dominio/` y `aplicacion/` a un diagrama StarUML con
  relaciones y multiplicidades.
- Acordar cómo se define el cupo (por publicación/franja), política de
  reprogramación y porcentajes/anticipación de reintegros.
- Registrar cada decisión como problema → alternativa elegida → consecuencias
  → evidencia.

## Hito 2 · Núcleo

- Completar entidades/valores para perfiles, SkillCoins, reservas, calificaciones
  y notificaciones.
- Agregar los casos de uso y puertos faltantes: publicación, pago atómico,
  cancelar/reintegrar, reprogramar con aceptación de ambas partes, búsqueda
  paginada, recomendaciones, calificar y recalcular reputación.
- Cubrir reglas positivas y negativas con pruebas unitarias y dobles de puertos.
- Incorporar las cuatro variaciones/patrones de `docs/arquitectura.md` con tests.

## Hito 3 · Persistencia e infraestructura

- Implementar adaptadores JPA para los puertos de aplicación y configurar
  `persistence.xml`/`EntityManagerFactory` con credenciales desde variables de
  entorno; no guardar secretos en Git.
- Ejecutar la migración Flyway sobre una base local limpia y validar el esquema.
- Hacer que aceptación, pago y cancelación sean transacciones atómicas; proteger
  el cupo/saldo ante concurrencia y probar operaciones repetidas.
- Agregar datos demo reproducibles y pruebas de integración de persistencia.

## Hito 4 · Integración

- Completar los contratos de controllers con paginación, mensaje/franja,
  operaciones explícitas de contratación, perfil, recomendaciones y
  calificaciones.
- Implementar controllers delgados que reciban el `userId`, validen permisos y
  traduzcan errores de aplicación de forma uniforme.
- Adaptar mock y UI gradualmente; la UI debe depender solo de interfaces de
  controllers y modelos de vista.
- Probar el recorrido publicar → solicitar → aceptar → pagar → completar →
  calificar y documentar cómo ejecutar la demo.

## Criterios de terminado

- `mvn test` y `mvn compile` pasan con Java 17.
- El dominio y la aplicación pueden probarse sin Swing, MySQL ni Hibernate.
- Todas las búsquedas/listados se paginan; las mutaciones respetan permisos,
  transacciones e historial de estados.
- No se usa Spring, REST ni `hbm2ddl.auto=create`.
- El README describe los requisitos y comandos reproducibles.
