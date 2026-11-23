# Guía para completar el trabajo práctico

Estado por hito al 23/11/2026 (entrega: martes 24/11). ✅ hecho · 🔶 en revisión (PR abierto) · ⬜ pendiente.

## Hito 1 · Diseño

- ✅ Consigna transcripta en `docs/consigna/`.
- 🔶 Historias de usuario con criterios de aceptación y tests: `docs/historias-de-usuario.md` (PR #27).
- ✅ Informe de decisiones (problema → alternativa → consecuencias → evidencia), con patrones, SOLID/GRASP, casos de aceptación y cobertura: `docs/informe-decisiones.md`.
- 🔶 Diagrama de clases UML de dominio y aplicación, con multiplicidades y patrones: `docs/uml/diagrama-clases-uml.png` (PR #27). Además, el diagrama generado desde el código (`docs/diagrama-clases.puml`) y los Mermaid de `docs/diagramas.md`.
- ⬜ Diagrama en StarUML, a partir de `docs/uml/diagrama-clases-uml.png`.

## Hito 2 · Núcleo

- ✅ Dominio completo: perfiles, publicaciones con franjas y promociones, contrataciones con estados, reintegro, reprogramación y calificaciones, wallet con retención en garantía, notificaciones (PRs #4 a #11).
- ✅ Capa de aplicación completa, con los diez casos de aceptación y los seis patrones cubiertos por tests (PRs #12 a #19).

## Hito 3 · Persistencia e infraestructura

- ✅ Adapters simulados de email, push e identidad, e implementación en memoria (`BackendEnMemoria`, plan B de la demo).
- ✅ Persistencia JPA (PR #20): repositorios que mapean con `reconstruir(...)`, búsqueda sin N+1, `UnidadDeTrabajoJpa`, `V1` aplicada por Flyway con `hbm2ddl=validate`, `@Version` y bloqueo optimista, configuración por `DB_URL`, `DB_USER` y `DB_PASSWORD`, y 26 tests de integración con Testcontainers.

## Hito 4 · Integración

- ✅ Controllers reales, manejo uniforme de errores, interfaces del equipo, datos de demo y UI conectada (PRs #21 y #25).
- ✅ JaCoCo con mínimo de 80 % en dominio + aplicación (PR #22).
- ✅ README reproducible y guion de la demo (PR #23); guía paso a paso en `docs/guia-levantar-proyecto.md` (PR #27).
- 🔶 UI: botón "Calificar", "Ver historial" y "Volver a solicitar" en las contrataciones terminadas, y orden "Más reciente" (PR #26).

## Pendiente para la entrega

- ⬜ Revisar y mergear los PRs #26 y #27.
- ⬜ Diagrama en StarUML.
- ⬜ Exportar `docs/informe-decisiones.md` a PDF, con el diagrama de StarUML.
- ⬜ Limpieza final del repo (ver `docs/pendientes-equipo.md`).

## Criterios de terminado

- ✅ `mvn verify` pasa con Java 17: 580 tests, 0 fallos; dominio + aplicación con 99,6 % de líneas cubiertas.
- ✅ El dominio y la aplicación se prueban sin Swing, MySQL ni Hibernate.
- ✅ Todas las búsquedas y listados se paginan; las mutaciones respetan permisos, transacciones e historial de estados.
- ✅ No se usa Spring, REST ni `hbm2ddl.auto=create`.
- ✅ El README describe los requisitos y comandos reproducibles.
