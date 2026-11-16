# Guía para completar el trabajo práctico

Estado por hito. ✅ hecho · 🔶 en revisión (PR abierto) · ⬜ pendiente.

## Hito 1 · Diseño

- ✅ Consigna transcripta en `docs/consigna/`.
- ✅ Decisiones de dominio acordadas (cupo por franja, reprogramación con aceptación de la otra parte, reintegro 100/50/0 % con 48 h y 24 h configurables): `AGENTS.md`.
- ✅ Decisiones de la capa de aplicación en formato problema → alternativa → consecuencias → evidencia: `docs/decisiones-capa-aplicacion.md`.
- ⬜ Diagrama de clases completo de dominio y aplicación en **StarUML**, con relaciones, multiplicidades y patrones identificados. Los PRs tienen un `classDiagram` Mermaid de cada parte, para partir de ahí.

## Hito 2 · Núcleo

- ✅ Dominio completo: perfiles, publicaciones con franjas y promociones, contrataciones con estados, reintegro, reprogramación y calificaciones, wallet con retención en garantía, notificaciones (PRs #4 a #11).
- 🔶 Capa de aplicación completa, con los diez casos de aceptación y los seis patrones cubiertos por tests (PRs #12 a #18, en cadena: se mergean en orden).
  - #12 contratos (puertos, DTOs, firmas) · #13 `reconstruir` y reglas de franja y cupo en el dominio · #14 contratación · #15 publicaciones, perfil y wallet · #16 notificaciones · #17 búsqueda y recomendaciones · #18 recorrido completo y documentación.
  - Cuando se mergea uno, al siguiente se le cambia la base a `main` y se pasa a "listo para revisar".

## Hito 3 · Persistencia e infraestructura

- ✅ Adapters simulados de email, push e identidad (`infrastructure/notificacion`, `infrastructure/identidad`).
- ✅ Implementación en memoria de todos los puertos y composition root `BackendEnMemoria` (plan B para la demo).
- ⬜ Adaptadores JPA para los puertos del dominio y de aplicación, mapeando con `reconstruir(...)`. `persistence.xml` y `EntityManagerFactory` con credenciales desde variables de entorno, sin secretos en Git.
- ⬜ `BuscadorPublicaciones` JPA con una sola consulta (join a perfiles), sin N+1.
- ⬜ `UnidadDeTrabajo` con `EntityTransaction`.
- ⬜ Alinear `V1` con el dominio (identidad por `userId`, Wallet de garantía sin perfil, tipo de perfil) y ejecutarla con Flyway sobre una base limpia.
- ⬜ `@Version` en publicación (cupo) y en la Wallet de garantía; probar operaciones repetidas y concurrentes.
- ⬜ Datos demo reproducibles y pruebas de integración de persistencia.
- ⬜ Composition root con JPA, igual a `BackendEnMemoria` pero con los repositorios JPA.

## Hito 4 · Integración

- ⬜ Implementar `UiControllers` sobre los Services: mapear DTOs a `Models` y traducir `DomainException` y `ApplicationException` de forma uniforme. Ver "Qué le falta al contrato actual" en `AGENTS.md`.
- ⬜ Conectar la UI en `SkillSwapApplication` (primero con `BackendEnMemoria`, después con JPA).
- ✅ Recorrido publicar → solicitar → aceptar → pagar → completar → calificar probado de punta a punta (`RecorridoCompletoTest`).
- ⬜ Documentar cómo ejecutar la demo en el README.

## Criterios de terminado

- `mvn test` y `mvn compile` pasan con Java 17.
- El dominio y la aplicación pueden probarse sin Swing, MySQL ni Hibernate (hoy: sí).
- Todas las búsquedas/listados se paginan; las mutaciones respetan permisos, transacciones e historial de estados.
- No se usa Spring, REST ni `hbm2ddl.auto=create`.
- El README describe los requisitos y comandos reproducibles.
