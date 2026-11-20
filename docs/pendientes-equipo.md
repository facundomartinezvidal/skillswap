# Pendientes para el equipo

Salieron de revisar el código contra la consigna (`docs/consigna/`) el 16/11. El resto de la consigna se cumple: arquitectura, reglas de negocio, patrones con tests, persistencia, los 10 casos de aceptación y los casos negativos.

Cada punto dice qué pasa, por qué importa y por dónde empezar.

## Roto

### 1. No se puede calificar desde la UI

- **Qué pasa:** la demo en vivo tiene que recorrer publicar → solicitar → aceptar → pagar → completar → **calificar** (`07-entregas-pruebas-y-evaluacion.md`), pero la pantalla de contrataciones no tiene un botón para calificar. Hoy el caso 10 solo se ve en `CasosDeAceptacionTest`.
- **Qué hacer:** agregar un botón **"Calificar"** en las filas `COMPLETADA` de `showContracts()` (`ui/SkillSwapView.java`). El botón pide un puntaje del 1 al 5 (y un comentario opcional) con `JOptionPane` y llama a `CalificacionController.calificar(userId, contratacionId, puntaje, comentario)` dentro de `intentar(...)`, que ya muestra los errores.
- **Cómo llega el controller a la vista:** `SkillSwapControllers` ya tiene `calificaciones()`. `SkillSwapView` hoy recibe `UiControllers`, así que hay que pasarle también el `CalificacionController` por constructor, desde `SkillSwapApplication`.
- **Prueba:** completar una contratación, calificar, ver que la reputación del perfil cambia y que un segundo intento muestra un error.

## Bug

### 2. Las contrataciones RECHAZADAS muestran botones que siempre fallan

- **Qué pasa:** en `showContracts()` los botones "Avanzar estado" y "Cancelar" se ocultan solo en `COMPLETADA` y `CANCELADA`. Una contratación `RECHAZADA` los muestra, y al tocarlos sale un error.
- **Qué hacer:** ocultarlos también para `RECHAZADA`.

## Mejorable

### 3. Falta el orden "Más reciente" en el buscador

El backend soporta los cuatro órdenes que pide la consigna (`OrdenBusqueda`, y `BusquedaControllerImpl.aOrden` ya reconoce el texto `"Más reciente"`), pero el combo de `showExploreWith()` solo ofrece tres. Basta con agregar `"Más reciente"` al arreglo del combo.

### 4. Diagrama de clases en StarUML (base lista: `docs/uml/diagrama-clases-uml.png`; falta pasarlo a StarUML)

- La consigna pide el diagrama de dominio y aplicación **en StarUML**, con relaciones, multiplicidades y patrones identificados.
- `docs/SkillSwap.mdj` se generó desde el código y el JSON es válido, pero **nunca se abrió en StarUML** (no había licencia) y los elementos quedaron acomodados en grilla.
- **Qué hacer:** abrirlo en StarUML, ordenar el layout por módulo, revisar las multiplicidades y marcar los patrones (State, Strategy, Simple Factory, Decorator, Observer, Adapter). Como referencia sirven `docs/diagrama-clases.png` y los Mermaid de `docs/diagramas.md`. Si es más fácil, rehacerlo a mano a partir de esos diagramas.

### 5. ~~Historias de usuario~~ (resuelto: `docs/historias-de-usuario.md`)

El hito 1 pide "alcance, historias…", pero no hay un documento de historias. Un `docs/historias-de-usuario.md` corto alcanza: una historia por flujo (perfil, publicar, buscar, contratar, pagar, cancelar, calificar, notificaciones), con su criterio de aceptación enlazado al caso de `CasosDeAceptacionTest`.

### 6. Informe en PDF

La consigna pide el informe de decisiones en PDF. `docs/informe-decisiones.md` está completo, pero falta exportarlo (desde VS Code o Word) y entregarlo. El PDF no se versiona (los `.pdf` están en `.gitignore`).

### 7. Comprobante de la contratación completada (opcional)

En `COMPLETADA` la consigna menciona "consultar comprobante". `ContratacionDTO` ya trae el comprobante, pero la UI no lo muestra. Mostrarlo es opcional.

### 8. Tests de MySQL

Los 24 tests de `infrastructure/persistencia` usan Testcontainers: si Docker no anda, se omiten. En el CI de GitHub corren. Antes de la defensa conviene correr `mvn test` en una máquina con Docker funcionando y confirmar que pasan los 564.

## Estado al 20/11 (entrega: martes 24/11)

**Hecho**

- Todas las PRs del backend (#12 a #25) están en `main`: 569 tests y CI en verde.
- PR #26: botón "Calificar", "Ver historial" y "Volver a solicitar" en las contrataciones RECHAZADA y CANCELADA, y el orden "Más reciente" (puntos 1, 2 y 3).
- PR #27: historias de usuario (punto 5), diagrama de clases UML base en `docs/uml/` y la guía [`guia-levantar-proyecto.md`](guia-levantar-proyecto.md).

**Falta, en este orden**

1. Revisar, aprobar y mergear #26 y #27 (con "Create a merge commit", base `main`).
2. **Diagrama en StarUML** (punto 4): rehacerlo en StarUML a partir de `docs/uml/diagrama-clases-uml.png`, con relaciones, multiplicidades y patrones.
3. **Informe en PDF** (punto 6): exportar `docs/informe-decisiones.md` y sumarle el diagrama de StarUML.
4. **Limpieza final del repo**, cuando todo lo anterior esté mergeado: sacar `AGENTS.md`, la carpeta `.claude/` y `skills-lock.json`, que solo sirven para el desarrollo, y los enlaces que apuntan a ellos.

Los puntos 7 (comprobante) y 8 (tests de MySQL con Docker) son opcionales.
