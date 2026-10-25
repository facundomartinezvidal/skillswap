# AGENTS.md — SkillSwap

Guía para agentes de IA (Claude Code, Codex, Cursor, etc.) que trabajen en este repositorio.

## Qué es

Trabajo Práctico Obligatorio de **Procesos de Desarrollo de Software (UADE)**. SkillSwap es un marketplace donde profesionales ofrecen mentorías o servicios breves, las organizaciones publican desafíos y los usuarios contratan pagando con créditos ficticios (**SkillCoins**).

La cátedra entrega una interfaz Swing con datos mock. **El trabajo del equipo es el backend**: dominio, casos de uso, controllers, persistencia y pruebas.

## Consigna (leer antes de diseñar o implementar)

La consigna completa está transcripta en [`docs/consigna/`](docs/consigna/), con un archivo por tema. El original es `tpo-consignas.pdf`, que no se versiona (los PDF están en `.gitignore`): pedíselo al equipo y guardalo en la raíz del proyecto. Si algo no coincide, gana el PDF.

| Si vas a trabajar en… | Leé |
|---|---|
| Entender el producto, el alcance y qué queda afuera | [01-contexto-y-condiciones.md](docs/consigna/01-contexto-y-condiciones.md) |
| Perfiles, publicaciones, búsqueda, contrataciones, notificaciones | [02-alcance-funcional.md](docs/consigna/02-alcance-funcional.md) |
| Validaciones e invariantes del dominio | [03-reglas-de-negocio.md](docs/consigna/03-reglas-de-negocio.md) |
| Paquetes, capas, dependencias, patrones | [04-arquitectura-y-patrones.md](docs/consigna/04-arquitectura-y-patrones.md) |
| JPA, MySQL, Flyway, transacciones, concurrencia | [05-persistencia.md](docs/consigna/05-persistencia.md) |
| Controllers y conexión con la UI | [06-controllers-e-interfaz.md](docs/consigna/06-controllers-e-interfaz.md) |
| Tests, entregas, criterios de evaluación, casos de aceptación | [07-entregas-pruebas-y-evaluacion.md](docs/consigna/07-entregas-pruebas-y-evaluacion.md) |

## Reglas que no se negocian

- **Nada de Spring / Spring Boot.** Sin `@Service`, `@Autowired` ni `@Transactional` de Spring. La inyección de dependencias es manual y se arma en `main`.
- **Nada de HTTP / REST.** Los controllers son clases Java normales que la UI invoca dentro del mismo proceso.
- **Clean Architecture:** las dependencias apuntan hacia el dominio. El dominio es Java puro (sin JPA, sin Swing). La UI solo conoce interfaces de controllers, nunca repositorios ni entidades JPA.
- **Controllers delgados:** sin SQL ni reglas de negocio, con manejo uniforme de errores, y validan permisos con el `userId` recibido.
- **El dominio rechaza las transiciones de estado inválidas** y las guarda en un historial inmutable (fecha, actor y motivo).
- **Flyway** para el esquema; nunca `hbm2ddl.auto=create` en la entrega.
- Al menos **4 patrones de diseño**, cada uno justificado por una variación real y **cubierto por tests**.
- **No rediseñar las pantallas.** El diseño visual no se evalúa.

## Stack

| | |
|---|---|
| Lenguaje | Java 17 (`maven.compiler.release=17`). El IDE puede tener un JDK más nuevo, pero el código tiene que compilar con 17. |
| Build | Maven (`pom.xml`), sin wrapper |
| ORM | Hibernate ORM 7.2.1 (JPA) |
| Base de datos | MySQL (`mysql-connector-j` 9.7.0) |
| Migraciones | Flyway 12.8.1 (`flyway-mysql`) |
| Tests | JUnit Jupiter 6.1.0 |
| UI | Swing |

## Comandos

```bash
mvn compile
mvn test
mvn exec:java
mvn exec:java -Dexec.args="--screenshot out.png"
```

- `mvn exec:java` abre la UI (main: `ar.edu.uade.skillswap.SkillSwapApplication`).
- `--screenshot <archivo>` renderiza la pantalla de inicio a un PNG sin abrir la ventana.

## Estado actual del código

```
src/main/java/ar/edu/uade/skillswap/
├── SkillSwapApplication.java          # main: instala Theme, crea MockSkillSwapControllers y abre la ventana
├── controllers/UiControllers.java     # CONTRATO entre la UI y el backend (interfaces de controllers)
├── mock/MockSkillSwapControllers.java # implementación en memoria con datos de demo
├── model/Models.java                  # records/enums que viajan entre la UI y los controllers
└── ui/                                # SkillSwapView (pantallas) y Theme (colores y fuentes)
src/test/java/ar/edu/uade/skillswap/mock/MockSkillSwapControllersTest.java
```

- `UiControllers` agrupa las sub-interfaces `BusquedaController`, `PublicacionController`, `ContratacionController`, `WalletController`, `NotificacionController` y `PerfilController`.
- `Models` son **modelos de vista** (records inmutables): `Perfil`, `Publicacion`, `Contratacion`, `Movimiento`, `Notificacion`, `FiltrosBusqueda` y los enums `Modalidad`, `EstadoPublicacion` y `EstadoContratacion`. **No** son entidades de dominio ni entidades JPA, y no deben tener lógica de negocio.
- `Modalidad` tiene `HIBRIDA` además de `REMOTA` y `PRESENCIAL`, aunque la consigna solo menciona remota y presencial.
- Todavía **no existen** el dominio, los casos de uso, la persistencia, `persistence.xml` ni las migraciones.
- El usuario activo está hardcodeado en la UI (`USER_ID = "user-mateo"`), simulando el login mock.

### Qué le falta al contrato actual respecto de la consigna

Al implementar, hay que extender `UiControllers` (o agregar interfaces) para cubrir:

- `ContratacionController`: `aceptar`, `rechazar`, `pagar`, `iniciar`, `completar` y reprogramar. Hoy solo existen un `avanzar` genérico y `cancelar`.
- Un `RecomendacionController` separado, con estrategias intercambiables.
- `CalificacionController.calificar(...)`.
- `PerfilController.crearPerfil(...)` y la edición del perfil (zona horaria, intereses, disponibilidad).
- **Paginación** en todos los listados de consulta.
- Filtros de búsqueda faltantes: rango de precio (hoy solo hay máximo), habilidad, reputación mínima y orden por fecha.
- Datos de publicación faltantes: cupo, franjas de disponibilidad, tipo (oferta o desafío) y motivo de cancelación.
- Solicitud con mensaje y franja elegida (hoy solo recibe el objetivo).
- Preferencia de canal de notificación por usuario.

Si se cambia el contrato, hay que adaptar `MockSkillSwapControllers` y `SkillSwapView` para que la app siga compilando y abriendo.

## Convenciones

- Idioma del dominio y del código: **español** (`Publicacion`, `Contratacion`, `crearBorrador`, `EstadoPublicacion`), igual que el código existente. Hay que mantener ese lenguaje ubicuo.
- Paquete base: `ar.edu.uade.skillswap`.
- Preferir `record` para value objects y DTOs inmutables.
- Al tocar la UI o el mock, la app tiene que seguir ejecutándose con `mvn exec:java`.
- Cualquier integrante tiene que poder explicar el código en la defensa: preferir soluciones claras antes que ingeniosas.
- Las decisiones de diseño van al informe PDF con el formato problema → alternativa elegida → consecuencias → evidencia.
- Documentación nueva del proyecto: en `docs/`.

## Skills disponibles

Instaladas en `.claude/skills/` y registradas en `skills-lock.json`:

- `clean-architecture` ([facundomartinezvidal/skills](https://github.com/facundomartinezvidal/skills)): revisa, diseña y explica la estructura del software según *Clean Architecture* (regla de dependencias, capas, puertos y adaptadores, SOLID y principios de componentes).

Para reinstalarla o actualizarla:

```bash
npx skills add facundomartinezvidal/skills --skill clean-architecture -a claude-code
```
