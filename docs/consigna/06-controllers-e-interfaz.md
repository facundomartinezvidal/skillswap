# Controllers e interfaz provista

> Fuente: `tpo-consignas.pdf`, secciones 9 y 10.

## Controllers mínimos

Los controllers son **clases Java normales**. Reciben comandos o parámetros desde la interfaz, invocan los casos de uso y devuelven resultados simples. **No se implementan endpoints, HTTP ni una API REST.**

| Controller y operación | Propósito |
|---|---|
| `PerfilController.crearPerfil(...)` | Crear perfil profesional. |
| `PerfilController.verPerfil(id)` | Ver perfil y reputación. |
| `PublicacionController.crearBorrador(...)` | Crear publicación en borrador. |
| `PublicacionController.cambiarEstado(...)` | Ejecutar una transición de estado. |
| `BusquedaController.buscar(filtros)` | Buscar, filtrar, ordenar y paginar. |
| `RecomendacionController.recomendar(usuario)` | Obtener recomendaciones para el usuario. |
| `ContratacionController.solicitar(...)` | Solicitar una publicación. |
| `ContratacionController.aceptar(id)` | Aceptar y reservar cupo. |
| `ContratacionController.pagar(id)` | Confirmar pago con SkillCoins. |
| `ContratacionController.cancelar(id)` | Cancelar aplicando la política de reintegro. |
| `ContratacionController.completar(id)` | Completar la contratación. |
| `CalificacionController.calificar(...)` | Registrar una calificación verificada. |
| `NotificacionController.listar(usuario)` | Listar las notificaciones del usuario. |

- Los nombres son orientativos.
- Los controllers deben ser **delgados**: **sin SQL ni reglas de negocio**, y con **manejo uniforme de los errores de aplicación**.
- Pueden usar **command objects** o modelos de entrada simples para evitar listas largas de parámetros.

## Interfaz provista ("Mock UI Kit")

La cátedra entrega una interfaz Java funcional (Swing o JavaFX; en este repo es Swing) que al principio usa un controller mock con datos en memoria. **No hay que rediseñar las pantallas**: hay que implementar los controllers reales y conectarlos mediante interfaces Java.

| Pantalla incluida | Responsabilidad del equipo |
|---|---|
| Inicio, buscador y filtros | Invocar `BusquedaController` y `RecomendacionController`. |
| Detalle de publicación | Invocar la solicitud y mostrar los errores de aplicación. |
| Panel del proveedor | Invocar cambios de estado, aceptación y cupos. |
| Wallet de SkillCoins | Consultar saldo y movimientos mediante controller. |
| Notificaciones | Consultar la bandeja y marcar como leídas. |
| Perfiles y calificaciones | Consultar perfil, reputación e historial. |

## Contrato de integración

- La interfaz y la lógica corren **dentro de la misma aplicación Java**.
- La UI conoce **interfaces de controllers**, **nunca repositorios ni entidades JPA**.
- Un **login simulado** entrega el `userId` activo a cada operación; no se implementa autenticación real.
