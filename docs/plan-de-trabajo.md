# Guía para completar el trabajo práctico

Estado por hito al 16/11/2026. ✅ hecho · 🔶 en revisión (PR abierto) · ⬜ pendiente.

Los PRs están encadenados y se mergean en orden: #12, #13, #14 … #24. Cuando se mergea
uno, al siguiente se le cambia la base a `main` y se pasa a "listo para revisar".

## Hito 1 · Diseño

- ✅ Consigna transcripta en `docs/consigna/`.
- ✅ Decisiones de dominio acordadas: `AGENTS.md`.
- 🔶 Informe de decisiones completo (problema → alternativa → consecuencias → evidencia), con patrones, SOLID/GRASP, casos de aceptación y cobertura: `docs/informe-decisiones.md` (PR #23).
- 🔶 Diagrama de clases de dominio y aplicación: `docs/diagrama-clases.puml` y su PNG, Mermaid en `docs/diagramas.md` y el proyecto StarUML `docs/SkillSwap.mdj` (PR #24).

## Hito 2 · Núcleo

- ✅ Dominio completo: perfiles, publicaciones con franjas y promociones, contrataciones con estados, reintegro, reprogramación y calificaciones, wallet con retención en garantía, notificaciones (PRs #4 a #11).
- 🔶 Capa de aplicación completa, con los diez casos de aceptación y los seis patrones cubiertos por tests (PRs #12 a #18).
- 🔶 Publicaciones CERRADA y CANCELADA no admiten aceptar ni reprogramar (PR #19).

## Hito 3 · Persistencia e infraestructura

- ✅ Adapters simulados de email, push e identidad, e implementación en memoria (`BackendEnMemoria`, plan B de la demo).
- 🔶 Persistencia JPA (PR #20):
  - repositorios JPA mapeando con `reconstruir(...)`;
  - `BuscadorPublicacionesJpa` sin N+1;
  - `UnidadDeTrabajoJpa` con `EntityTransaction`;
  - `V1` alineada con el dominio, aplicada por Flyway al arrancar, y `hbm2ddl=validate`;
  - `@Version` y bloqueo optimista (caso 4 probado con dos hilos contra MySQL);
  - `docker-compose.yml` y configuración por `DB_URL`, `DB_USER` y `DB_PASSWORD`;
  - 24 tests de integración con Testcontainers.

## Hito 4 · Integración

- 🔶 Controllers reales sobre los Services, manejo uniforme de errores, interfaces del equipo para lo que la UI no expone, datos de demo y UI conectada con MySQL por defecto y `--memoria` como plan B (PR #21).
- 🔶 JaCoCo, mínimo de 80 % en dominio + aplicación y regla de arquitectura para la infraestructura (PR #22).
- 🔶 README reproducible y guion de la demo (`docs/demo.md`) (PR #23).

## Pendiente

- ⬜ Correr los 24 tests de MySQL en una máquina con Docker andando (en CI corren solos).
- ⬜ Revisión y merge de los PRs en orden.
- ⬜ Pasar `docs/informe-decisiones.md` a PDF para la entrega.

## Criterios de terminado

- ✅ `mvn test` y `mvn compile` pasan con Java 17 (564 tests, 0 fallos).
- ✅ El dominio y la aplicación se prueban sin Swing, MySQL ni Hibernate.
- ✅ Todas las búsquedas y listados se paginan; las mutaciones respetan permisos, transacciones e historial de estados.
- ✅ No se usa Spring, REST ni `hbm2ddl.auto=create`.
- ✅ El README describe los requisitos y comandos reproducibles.
