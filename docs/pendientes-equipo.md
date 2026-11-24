# Pendientes para el equipo

Salieron de revisar el código contra la consigna (`docs/consigna/`) el 16/11. El resto de la consigna se cumple: arquitectura, reglas de negocio, patrones con tests, persistencia, los 10 casos de aceptación y los casos negativos.

Cada punto dice qué pasa, por qué importa y por dónde empezar.

## Roto

### 1. ~~No se puede calificar desde la UI~~ (resuelto: botón "Calificar" en las filas COMPLETADA)

- **Qué pasa:** la demo en vivo tiene que recorrer publicar → solicitar → aceptar → pagar → completar → **calificar** (`07-entregas-pruebas-y-evaluacion.md`), pero la pantalla de contrataciones no tiene un botón para calificar. Hoy el caso 10 solo se ve en `CasosDeAceptacionTest`.
- **Qué hacer:** agregar un botón **"Calificar"** en las filas `COMPLETADA` de `showContracts()` (`ui/SkillSwapView.java`). El botón pide un puntaje del 1 al 5 (y un comentario opcional) con `JOptionPane` y llama a `CalificacionController.calificar(userId, contratacionId, puntaje, comentario)` dentro de `intentar(...)`, que ya muestra los errores.
- **Cómo llega el controller a la vista:** `SkillSwapControllers` ya tiene `calificaciones()`. `SkillSwapView` hoy recibe `UiControllers`, así que hay que pasarle también el `CalificacionController` por constructor, desde `SkillSwapApplication`.
- **Prueba:** completar una contratación, calificar, ver que la reputación del perfil cambia y que un segundo intento muestra un error.

## Bug

### 2. ~~Las contrataciones RECHAZADAS muestran botones que siempre fallan~~ (resuelto: "Ver historial" en RECHAZADA y CANCELADA, y "Volver a solicitar" para el cliente en RECHAZADA)

- **Qué pasa:** en `showContracts()` los botones "Avanzar estado" y "Cancelar" se ocultan solo en `COMPLETADA` y `CANCELADA`. Una contratación `RECHAZADA` los muestra, y al tocarlos sale un error.
- **Qué hacer:** ocultarlos también para `RECHAZADA`.

## Mejorable

### 3. ~~Falta el orden "Más reciente" en el buscador~~ (resuelto)

El backend soporta los cuatro órdenes que pide la consigna (`OrdenBusqueda`, y `BusquedaControllerImpl.aOrden` ya reconoce el texto `"Más reciente"`), pero el combo de `showExploreWith()` solo ofrece tres. Basta con agregar `"Más reciente"` al arreglo del combo.

### 4. ~~Diagrama de clases en StarUML~~ (resuelto)

- [SkillSwap.mdj](SkillSwap.mdj) contiene el diagrama completo y cinco vistas por capa, generados desde el código. El diagrama completo se abre por defecto. Para cambiar de vista, hacé doble clic en un diagrama `Capa …` en el **Model Explorer** de StarUML. Si el panel está oculto, usá **View → Show Sidebar**. **View → Fit To Window** encuadra el diagrama activo.
- Los seis diagramas se exportaron desde StarUML y el verificador terminó con `TOTAL 0`. La incorporación del SVG al informe sigue en el punto 6.

### 5. ~~Historias de usuario~~ (resuelto: `docs/historias-de-usuario.md`)

El hito 1 pide "alcance, historias…", pero no hay un documento de historias. Un `docs/historias-de-usuario.md` corto alcanza: una historia por flujo (perfil, publicar, buscar, contratar, pagar, cancelar, calificar, notificaciones), con su criterio de aceptación enlazado al caso de `CasosDeAceptacionTest`.

### 6. Informe en PDF

La consigna pide el informe de decisiones en PDF. `docs/informe-decisiones.md` está completo, pero falta exportarlo (desde VS Code o Word) y entregarlo. El PDF no se versiona (los `.pdf` están en `.gitignore`).

### 7. ~~Comprobante de la contratación completada~~ (resuelto: botón "Comprobante" en las filas COMPLETADA)

En `COMPLETADA` la consigna menciona "consultar comprobante". `ContratacionDTO` ya trae el comprobante, pero la UI no lo muestra. Mostrarlo es opcional.

### 8. Tests de MySQL

Los 26 tests de `infrastructure/persistencia` usan Testcontainers: si Docker no anda, se omiten. En el CI de GitHub corren. Antes de la defensa conviene correr `mvn test` en una máquina con Docker funcionando y confirmar que pasan los 606.

## Estado al 23/11 (entrega: martes 24/11)

**Hecho**

- Todas las PRs (#4 a #31) están en `main`, con CI en verde: 606 tests, 0 fallos; dominio + aplicación con 99,6 % de líneas y 95,0 % de ramas.
- Puntos 1, 2, 3 y 5 resueltos (PR #26 y #27). La regla de "volver a solicitar" quedó en el dominio (`Contratacion.validarNuevaSolicitud` y `admiteNuevaSolicitud()` del State).
- UI: "Cambiar usuario" en la barra lateral (#29) y precio en el formulario de nueva publicación (#30).
- Revisión contra el PDF (#31): solicitar con franja, mensaje y objetivo (4.4); franjas y cupo cargados por el autor al crear la publicación; botones concretos en Contrataciones, más Rechazar, Reprogramar y Comprobante; listados de publicaciones propias y recomendaciones paginados; `correr.sh`.
- Diagrama de clases en [StarUML](SkillSwap.mdj), historias de usuario y la guía [`guia-levantar-proyecto.md`](guia-levantar-proyecto.md).

**Falta, en este orden**

1. **Informe en PDF** (punto 6): exportar `docs/informe-decisiones.md` y sumarle el diagrama de StarUML.
2. **Limpieza final del repo**: sacar `AGENTS.md`, la carpeta `.claude/` y `skills-lock.json`, que solo sirven para el desarrollo, y los enlaces que apuntan a ellos.

El punto 8 (tests de MySQL con Docker) es opcional: con Docker andando pasan los 606.
