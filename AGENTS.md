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
| Build | Maven (`pom.xml`), sin wrapper; JaCoCo 0.8.13 para cobertura |
| ORM | Hibernate ORM 7.2.1 (JPA) |
| Base de datos | MySQL (`mysql-connector-j` 9.7.0) |
| Migraciones | Flyway 12.8.1 (`flyway-mysql`) |
| Tests | JUnit Jupiter 6.1.0 y Testcontainers (MySQL) |
| UI | Swing |

## Comandos

```bash
docker compose up -d                                   # MySQL de desarrollo en el puerto 3307
mvn compile
mvn test                                               # reporte de cobertura en target/site/jacoco
mvn verify                                             # además exige 80 % de líneas en dominio + aplicación
mvn compile exec:java                                  # UI con BackendJpa (MySQL, migra y carga la demo)
mvn compile exec:java -Dexec.args="--memoria"          # UI con BackendEnMemoria (sin MySQL)
mvn compile exec:java -Dexec.args="--memoria --screenshot out.png"
```

- `mvn exec:java` abre la UI (main: `ar.edu.uade.skillswap.SkillSwapApplication`).
- `--screenshot <archivo>` renderiza la pantalla de inicio a un PNG sin abrir la ventana.
- Detalle en el `README.md` y guion de la demo en `docs/demo.md`.

## Estado actual del código

```
src/main/java/ar/edu/uade/skillswap/
├── SkillSwapApplication.java          # main: elige BackendJpa o --memoria, carga DatosDemo y abre la UI
├── domain/                            # Dominio Java puro (completo), con los puertos de repositorio
├── application/                       # Casos de uso: un Service por flujo, DTOs y puertos (ver docs/arquitectura.md)
├── infrastructure/                    # memoria/ (repos y unidad de trabajo en memoria), notificacion/ (adapters),
│                                      # identidad/ (simulada), persistencia/ (JPA: entidades, mappers, repositorios,
│                                      # UnidadDeTrabajoJpa, Migraciones), configuracion/ (BackendEnMemoria,
│                                      # BackendJpa y DatosDemo)
├── controllers/UiControllers.java     # CONTRATO de la cátedra entre la UI y el backend (no se modifica)
├── controllers/*Operaciones, RecomendacionController, CalificacionController   # interfaces del equipo
├── controllers/impl/                  # *ControllerImpl, SkillSwapControllers, ManejoDeErrores, ModelosMapper
├── mock/MockSkillSwapControllers.java # mock original de la cátedra (main ya no lo usa)
├── model/Models.java                  # records/enums que viajan entre la UI y los controllers
└── ui/                                # SkillSwapView (pantallas) y Theme (colores y fuentes)
src/main/resources/db/migration/V1__esquema_inicial.sql   # esquema Flyway, alineado con el dominio
docker-compose.yml                                        # MySQL 8 de desarrollo (puerto 3307, skillswap/skillswap)
src/test/java/ar/edu/uade/skillswap/
├── ArquitecturaTest.java              # reglas de dependencias de domain, application, controllers e infrastructure
├── RecorridoCompletoTest.java         # la demo de punta a punta con BackendEnMemoria
├── domain/  application/  infrastructure/  controllers/   # controllers/CasosDeAceptacionTest: los 10 casos
└── mock/MockSkillSwapControllersTest.java
```

- `UiControllers` agrupa las sub-interfaces `BusquedaController`, `PublicacionController`, `ContratacionController`, `WalletController`, `NotificacionController` y `PerfilController`.
- `Models` son **modelos de vista** (records inmutables): `Perfil`, `Publicacion`, `Contratacion`, `Movimiento`, `Notificacion`, `FiltrosBusqueda` y los enums `Modalidad`, `EstadoPublicacion` y `EstadoContratacion`. **No** son entidades de dominio ni entidades JPA, y no deben tener lógica de negocio.
- `Modalidad` tiene `HIBRIDA` además de `REMOTA` y `PRESENCIAL`, aunque la consigna solo menciona remota y presencial.
- El usuario activo simula el login mock: por defecto `user-mateo`, otro con `--usuario <userId>`, y con la app abierta se cambia desde **⇄ Cambiar usuario** en la barra lateral (ofrece `DatosDemo.USUARIOS`; `main` vuelve a armar la vista en la misma ventana y con el mismo backend). `ServicioIdentidadSimulado` conoce a todos los usuarios de `DatosDemo`.
- **El backend está completo** (PRs #12 a #24, encadenados): dominio, aplicación, persistencia JPA (`BackendJpa` contra MySQL con `DB_URL`, `DB_USER`, `DB_PASSWORD`; por defecto el MySQL del `docker-compose.yml`), controllers reales y la UI conectada. Sin argumentos la app usa MySQL; con `--memoria`, `BackendEnMemoria`. En los dos casos `DatosDemo` carga la demo una sola vez.
- `mvn test` corre 606 tests. Los 26 de `infrastructure/persistencia` usan Testcontainers (MySQL en Docker) y quedan deshabilitados si Docker no está disponible. Cobertura de dominio + aplicación: 99,6 % de líneas y 95,0 % de ramas (JaCoCo). El detalle está en [`docs/arquitectura.md`](docs/arquitectura.md) y las decisiones en [`docs/informe-decisiones.md`](docs/informe-decisiones.md).

- **Pendientes:** lo que falta según la revisión contra la consigna (botón Calificar en la UI, bug de las contrataciones RECHAZADAS, orden "Más reciente", diagrama en StarUML, historias de usuario e informe en PDF) está en [`docs/pendientes-equipo.md`](docs/pendientes-equipo.md).

### Decisiones de dominio acordadas

Antes de cambiar alguna, consultarlo con el equipo. Todas están implementadas y cubiertas por tests.

- **El modelo parte del dominio.** El esquema Flyway (`V1`) se ajusta al dominio en el hito de persistencia, no al revés.
- **Identidad de las personas:** todo el dominio usa el `userId` del servicio de identidad mock (`autorId`, `clienteId`, `proveedorId`, `titularId`, `Perfil.userId`). No hay `PerfilId`.
- **Módulos solo por id:** ningún módulo de dominio importa a otro salvo sus ids (`PublicacionId`, `FranjaId`, `ContratacionId`). `notificacion` solo depende de `shared`.
- **Publicacion:** cupo **por franja** (`reservarCupo`/`liberarCupo`); `PAUSADA → PUBLICADA` (reanudar) permitido como interpretación del equipo. Las promociones las define el autor en su publicación, se acumulan en el orden en que se agregaron, se pueden cambiar en `BORRADOR`, `PUBLICADA` y `PAUSADA`, y `precioFinal()` nunca baja de 0.
- **Contratacion:** valida el rol del actor (el proveedor acepta, rechaza, inicia y completa; el cliente paga). La reprogramación no cambia el estado: una parte propone y la otra acepta o rechaza. El reintegro al cancelar lo calcula una `PoliticaReintegro` (100/50/0 % según anticipación de 48 h y 24 h, configurable; todo al cliente si cancela el proveedor).
- **Calificación:** es una acción de `Contratacion` en `COMPLETADA`, una por participante, dirigida siempre a la otra parte. La reputación se acumula en `Perfil` (`Reputacion`: suma y cantidad) y solo cambia con `registrarCalificacion`.
- **Wallet:** el pago queda **retenido** en una Wallet de garantía de la plataforma (`Wallet.TITULAR_GARANTIA`) hasta completar; al cancelar, la garantía reparte según el `Reintegro`. `RetencionDePagos` agrupa cada débito con su crédito. Saldo inicial: bono de bienvenida que pasa `main`, más `acreditarDemo`. La Wallet guarda solo los movimientos nuevos; el listado lo hará el repositorio.
- **Perfil:** `TipoPerfil` (`PROFESIONAL` u `ORGANIZACION`) fijo desde la creación; la baja lógica queda en el historial y un perfil dado de baja rechaza ediciones pero sigue recibiendo calificaciones.
- **Eventos y notificaciones:** `Contratacion` registra un `EventoDominio` por cada operación válida (solicitud, aceptación, pago, reprogramación propuesta y aceptada, cancelación, sesión completada); cada evento declara sus interesados. El caso de uso los toma con `extraerEventos()` después de guardar. `Notificacion` es genérica y solo su destinatario la marca como leída; el canal no es parte de la notificación.
- **Patrones implementados:** State (estados de publicación y contratación), Strategy (políticas de reintegro y recomendaciones), Simple Factory (política de reintegro y canal de envío), Decorator (promociones), Observer (`PublicadorEventos` y `NotificadorDeInteresados`) y Adapter (email, push e identidad simulados). Ver la tabla en `docs/arquitectura.md`.

### Persistencia y controllers: acuerdos

- **Persistencia:** el esquema lo define solo Flyway (`V1`, `hbm2ddl=validate`); entidades JPA solo en infraestructura, con mappers que usan `reconstruir(...)`; `@Version` en publicación, contratación, wallet y perfil (la publicación con `OPTIMISTIC_FORCE_INCREMENT`); `UnidadDeTrabajoJpa` con `ThreadLocal<EntityManager>`; listados sin N+1 (ids de la página y después `join fetch`). Un cambio de esquema nuevo va en una `V2`.
- **Controllers:** `UiControllers` no se modifica. Lo que la UI no expone va en las interfaces del equipo. Todos los errores pasan por `ManejoDeErrores` y salen como `OperacionRechazadaException`. `avanzar` decide según estado y rol. La infraestructura no lanza excepciones de negocio (lo verifica `ArquitecturaTest`).

### Capa de aplicación: acuerdos

- Las firmas de los Services y los puertos (PR #12) son el contrato para persistencia y controllers. Todos los Services están implementados (PRs #14 a #17); un controller se conecta a un Service recién cuando su PR está mergeado.
- Checklist que pidió la revisión, para código nuevo:
  - Las reglas que dependen del estado las responde el dominio, no una lista de nombres en el Service.
  - Toda operación que modifica un agregado corre en una `UnidadDeTrabajo`, aunque toque uno solo: la lectura y el `save` tienen que estar en la misma transacción para que el `@Version` detecte los cambios concurrentes.
  - Adentro de una `UnidadDeTrabajo`, primero se modifican todos los agregados y los `save` van al final.
  - Los DTOs copian sus colecciones.
  - Los listados no hacen N+1 (`findByIds`, `findByUserIds`).
  - Todas las operaciones publican sus eventos.
  - En los `reconstruir`, nulos con mensaje y colecciones vacías.
- **Limitación conocida:** `PublicacionRepository.findPublicadas()` no tiene límite (las recomendaciones cargan todas las publicadas). Alcanza para el volumen del TP; con muchos datos, habría que acotar las candidatas en la consulta.

### Qué cubren las interfaces del equipo

Lo que la consigna pide y `UiControllers` no tenía ya está implementado y probado: `ContratacionOperaciones` (solicitar con franja y mensaje, aceptar, rechazar, pagar, iniciar, completar, cancelar con motivo, reprogramar, paso siguiente, comprobante), `RecomendacionController`, `CalificacionController`, `PerfilOperaciones` (crear, editar, disponibilidad, canal y baja), `PublicacionOperaciones` (crear con cupo y franjas, listar franjas, descuentos), `BusquedaOperaciones` (rango de precio, habilidad, reputación mínima, cuatro órdenes) y listados paginados (`PaginaVista<T>`). La vista usa estas interfaces para elegir franja al solicitar, cargar franjas y cupo al publicar, y para los botones concretos de Contrataciones (Rechazar, Reprogramar, Comprobante, Calificar). Solo cambiar la estrategia de recomendación queda sin botón.

## Convenciones

- Idioma: **español para el negocio, inglés para lo técnico.**
  - En español (lenguaje ubicuo de la consigna): entidades, value objects, estados, reglas y operaciones de negocio (`Publicacion`, `Contratacion`, `FranjaDisponibilidad`, `publicar`, `reservarCupo`, `CupoAgotadoException`), nombres de módulo de negocio (`publicacion`, `contratacion`), nombres de tests que describen comportamiento y mensajes que ve el usuario.
  - En inglés: capas y paquetes técnicos (`domain`, `application`, `infrastructure`, `shared`), sufijos y roles técnicos (`Repository`, `Mapper`, `Exception`, `UseCase`), métodos técnicos (`findById`, `save`, `generate`) y helpers privados sin significado de negocio.
  - El código entregado por la cátedra (`controllers`, `model`, `ui`, `mock` y sus tipos) no se renombra.
- Estructura de paquetes: primero la capa, después el módulo de negocio (`domain/publicacion`, `application/publicacion`, `infrastructure/publicacion`). Lo compartido entre módulos va en `domain/shared`: value objects, la excepción base y `Pagina`/`PaginaSolicitada`, que devuelven los puertos de repositorio del dominio para paginar los listados. Un módulo referencia a otro solo por ids; la coordinación entre módulos va en `application`.
- Dentro de cada módulo de dominio: la raíz del agregado, sus entidades internas, los value objects y el puerto del repositorio van en la raíz del módulo. Las implementaciones de patrones y las excepciones van en subpaquetes: `state` (la interfaz sellada y sus estados juntos, porque Java exige el mismo paquete), `strategy` (la interfaz y sus estrategias), `decorator` (el componente, el decorador y lo que decora), `evento` (los eventos de dominio que emite el agregado) y `exception`. Ejemplo: `domain/publicacion/state/EstadoPublicacion.java`.
- Paquete base: `ar.edu.uade.skillswap`.
- Preferir `record` para value objects y DTOs inmutables.
- Al tocar la UI o el mock, la app tiene que seguir ejecutándose con `mvn exec:java`.
- Cualquier integrante tiene que poder explicar el código en la defensa: preferir soluciones claras antes que ingeniosas.
- Toda PR que agregue o modifique una entidad o agregado de dominio incluye en su descripción un **diagrama de clases Mermaid** (`classDiagram`) de ese agregado, hecho a partir del código real: atributos, operaciones públicas, entidades internas, value objects, estados y excepciones, con sus relaciones.
- Las decisiones de diseño van al informe PDF con el formato problema → alternativa elegida → consecuencias → evidencia.
- Documentación nueva del proyecto: en `docs/`.

## Diagrama de clases en StarUML (entregable)

La consigna (sección 13) pide un **diagrama de clases completo de dominio y aplicación en StarUML, con relaciones, multiplicidades y patrones identificados**, y que se sume al informe en PDF. Es el pendiente principal de `docs/pendientes-equipo.md` (punto 4). Estas pautas salen de las guías de la materia que el equipo armó para los parciales (`proceso-desarrollo-software/guias/plantuml-diagramas.md`, `convenciones-de-nombres.md` y `arquitectura-en-el-parcial.md`) y de las resoluciones oficiales del profesor (examen 15 y la resolución propia del examen 18).

### Qué hay hoy en el repo

| Archivo | Qué es | Uso |
|---|---|---|
| `docs/SkillSwap.mdj` | **El diagrama en StarUML**: el diagrama completo con todas las capas (Presentación, Controllers, Aplicación, Dominio, Infraestructura) y las 217 clases, más una vista por capa (`Capa …`: las clases de la capa completas y, solo con el nombre, las de capas internas que usa o implementa), generado por `docs/uml/generador/` | Es el entregable. Se regenera; no se edita a mano. `verificar.py` revisa los 6 diagramas y deja un SVG por diagrama en la carpeta que se le pasa |
| `docs/uml/generador/` | `generar.py` arma el `.mdj` y `verificar.py` lo exporta a SVG con StarUML y comprueba que nada se superponga | Ver "Flujo de trabajo" |
| `docs/uml/diagrama-clases-uml.puml` (+ `.png`, `.svg`) | Diagrama de dominio hecho a mano en PlantUML | Referencia; tiene asociaciones entre módulos que el código no tiene |
| `docs/diagrama-clases.puml`, `docs/diagramas/`, `docs/diagramas.md` | Generados desde el código, uno por módulo (PlantUML y Mermaid) | Referencia rápida |

### Flujo de trabajo

1. **Generar** (necesita Graphviz, el comando `dot`):
   ```bash
   python3 docs/uml/generador/generar.py src/main/java/ar/edu/uade/skillswap docs/SkillSwap.mdj
   ```
   - El modelo (clases, atributos, operaciones, herencia) sale del código. Las relaciones del dominio, con sus multiplicidades, están declaradas en `relaciones.py`; las de las otras capas salen de los campos (asociaciones), de `implements`/`extends` y del uso en el código (dependencias de clases sueltas).
   - Cada módulo se arma con dot por separado (`layout.py`); los módulos van en filas fijas con todas las dependencias apuntando hacia el dominio (las capas externas arriba, Dominio abajo) y las relaciones entre módulos van en ángulo recto por pasillos, cada una en su carril.
   - Las etiquetas (multiplicidades, nombres, estereotipos) las ubica `etiquetas.py`: StarUML dibuja cada una en `punto + distancia·(cos(θ−alpha), sin(θ−alpha))` y respeta el `alpha`/`distance` que trae el `.mdj`, así que se eligen para que no pisen nada.
   - Las raíces de composición (`BackendJpa`, `BackendEnMemoria`, `DatosDemo`, `SkillSwapControllers`) tienen una dependencia «create» al paquete que arman en vez de una asociación por campo: sus campos quedan tipados en la caja.
2. **Verificar siempre antes de entregar o commitear**:
   ```bash
   python3 docs/uml/generador/verificar.py docs/SkillSwap.mdj /tmp/SkillSwap.svg
   ```
   Exporta con StarUML y revisa sobre lo dibujado: etiquetas que se pisan entre sí o con cajas, títulos o líneas; cajas superpuestas o fuera de su paquete; texto fuera de su caja; líneas que cruzan cajas o títulos; líneas encimadas, paralelas pegadas o con cruces rasantes; nudos; multiplicidades ambiguas; y que StarUML haya ubicado cada etiqueta donde se previó. Tiene que terminar en `TOTAL 0`. Además, mirar el PNG (`staruml image docs/SkillSwap.mdj -f png -o x.png`) completo y por zonas: el verificador no juzga la estética general.
3. **No retocar el `.mdj` a mano en StarUML**: se pierde al regenerar. Si algo no queda bien, se corrige en el generador.
4. **Exportar** desde StarUML (*File → Export Diagram As → SVG*) y pegar el SVG en el informe, en una **página horizontal**: es vectorial y se puede ampliar. En modo de prueba, StarUML exporta con la marca "UNREGISTERED".
5. **Versionar** el `.mdj` y el generador. El PDF del informe no se versiona (`.gitignore`).

### Cómo organizar el diagrama

- **Un solo diagrama con todas las capas bien separadas**, cada una como paquete de color con sus módulos y subpaquetes adentro (lo pidió el equipo para la presentación).
- **Capas como `UMLPackage` con color de fondo pastel**, una por capa y con las clases adentro, como en la resolución oficial del examen 15. Así el diagrama muestra solo qué va en cada capa:

  | Capa | Color |
  |---|---|
  | Dominio | `#DFFFDF` verde |
  | Aplicación | `#E3ECFF` azul (lo único fijo en los diagramas del profesor: Application va en azul) |
  | Infraestructura | `#FFE8DF` durazno |
  | Presentación / controllers | `#FFE5E5` rosa |
  | Proveedor externo simulado (Adaptee) | `#EFEFEF` gris, en su propia caja: no es una capa nuestra |

- **Alcance:** todas las clases de todas las capas (la consigna pide como mínimo dominio y aplicación). Las flechas entre capas apuntan siempre hacia adentro: ninguna sale de Dominio o Aplicación hacia Infraestructura, y ninguna asociación une módulos de dominio (solo ids).
- **Dónde va cada patrón** (regla de dependencias: la interfaz adentro, la implementación afuera):
  - Dominio: State (`domain/publicacion/state`, `domain/contratacion/state`), Strategy y Simple Factory de reintegro (`domain/contratacion/strategy`), Decorator (`domain/publicacion/decorator`), eventos (`domain/*/evento`) y los puertos de repositorio.
  - Aplicación: Strategy de recomendación (`EstrategiaRecomendacion` y `busqueda/recomendacion`), Observer (`PublicadorEventos`, `SuscriptorEvento`, `NotificadorDeInteresados`), Simple Factory de canal (`CanalEnvioFactory`) y los puertos `ServicioIdentidad`, `EnvioNotificacion`, `UnidadDeTrabajo` y `BuscadorPublicaciones`.
  - Infraestructura: los Adapter concretos y lo que envuelven.

### Notación (lo que se corrige)

- **Los nombres son los del código, tal cual.** Las guías del parcial piden prefijo `I` en las interfaces, pero este TP es un proyecto Java donde el diagrama tiene que poder rastrearse en el código: `PublicacionRepository`, `EstrategiaRecomendacion`, `PoliticaReintegro`, sin `I`. No renombrar ni "mejorar" nombres.
- **Interfaces distinguibles**: con el estereotipo «interface» o con la notación de círculo de StarUML (la que usa el profesor). Si no se distinguen de las clases, se pierde el ítem "interfaces".
- **Atributos privados (`-`) y operaciones públicas (`+`), todos con tipo**: `-precioBase: SkillCoins`, `+reservarCupo(franjaId: FranjaId): void`. Parámetros como `nombre: Tipo` (no al revés). Primitivos en minúscula (`long`, `int`, `boolean`) y clases en mayúscula (`String`, `Instant`).
- **Colecciones tipadas**: `List<FranjaDisponibilidad>`, `Set<String>`. **En StarUML, un `Map` va con punto y coma** (`Map<String;Perfil>`), porque StarUML usa la coma como separador interno y parte el tipo al guardar. Hoy ningún campo del modelo es un `Map`; vale para cualquier tipo genérico con dos parámetros.
- **Abstracto marcado** (clase y método `{abstract}`); los `enum` como `UMLEnumeration` (`TipoPerfil`, `Modalidad`, `CanalNotificacion`); los `record` value objects con estereotipo «Value Object».
- **Estereotipo de patrón** en cada participante, como en `docs/uml/diagrama-clases-uml.puml`: «State», «Strategy», «SimpleFactory», «Decorator», «Observer», «Adapter».

### Relaciones y multiplicidades

| Relación | En StarUML | Cuándo |
|---|---|---|
| Composición ◆ | rombo lleno del lado del todo | La parte no existe sin el agregado y no se comparte (`Publicacion` ◆ `FranjaDisponibilidad`) |
| Agregación ◇ | rombo hueco del lado del todo | Contiene partes que viven solas (un Service con sus estrategias) |
| Asociación | línea con flecha de navegación, nombre (`+usa`, `+posee`) y multiplicidad en los dos extremos | Un campo que referencia a otra clase |
| Dependencia | punteada con flecha abierta | Usa sin guardar: Service → DTO, Service → command |
| Realización | punteada con triángulo hueco | Clase que implementa una interfaz |
| Generalización | sólida con triángulo hueco | Herencia; sin multiplicidad |

**Las multiplicidades salen del código**: campo obligatorio `1`; campo que puede ser nulo `0..1`; `List`/`Set` `0..*` (o `1..*` si el dominio exige al menos uno). Con los campos reales:

- `Publicacion` ◆ `FranjaDisponibilidad` `0..*`, ◆ `Promocion` `0..*`, ◆ `CambioEstado` `0..*`, → `EstadoPublicacion` `1`, → `SkillCoins` `1` (precio base). Al menos una franja se exige al publicar, no al crear: `0..*`.
- `Contratacion` → `Solicitud` `1`, → `EstadoContratacion` `1`, → `PropuestaReprogramacion` `0..1`, → `Reintegro` `0..1`, ◆ `CambioEstado` `0..*`, ◆ `Calificacion` `0..*` (a lo sumo 2: una por participante).
- `Perfil` → `Reputacion` `1`, ◆ `DisponibilidadSemanal` `0..*`, → `CanalNotificacion` `1`, ◆ `CambioEstado` `0..*`.
- `Wallet` → `SkillCoins` `1` (saldo), ◆ `Movimiento` `0..*`.

**Entre módulos no hay asociación.** Por la decisión "módulos solo por id", `Contratacion` guarda `publicacionId: PublicacionId` y `franjaId: FranjaId`, y las personas son `String` (`clienteId`, `proveedorId`, `autorId`). Se dibujan como atributos, nunca como una línea `Contratacion → Publicacion`. Una línea así contradice el código y el informe.

### Patrones identificados en el dibujo

- **Una nota por patrón, en el participante que define el rol**, con tres renglones: el rol en negrita, la variación que resuelve y qué se rompería sin él. Ejemplo para `PoliticaReintegro`: **STRATEGY** · reintegro 100/50/0 % o total según quién cancela · otra política es otra clase, `ContratacionService` no cambia. Lo mismo para State (×2), Decorator, Simple Factory (×2), Observer y Adapter.
- **Todas las notas del mismo lado** (abajo de la clase) y **solo en los roles de patrón**: notas de lados distintos hacen que las cajas se reacomoden, y una nota por clase vuelve ilegible el diagrama. Cuatro o cinco líneas cortas, sin repetir lo que la caja ya dice ni teoría de manual.
- Los GRASP y SOLID, que son transversales, van al informe o a una leyenda aparte, no en notas.

### Antes de exportar

- [ ] Cada clase del diagrama existe en el código con ese nombre, y cada agregado de dominio está (Publicacion, Contratacion, Perfil, Wallet, Notificacion, con sus entidades internas y value objects).
- [ ] Ninguna clase suelta (sin relaciones y sin motivo) ni relación duplicada (por ejemplo, una agregación y una composición entre las mismas dos clases).
- [ ] Todas las asociaciones tienen multiplicidad en los dos extremos y nombre; las etiquetas no se superponen con las líneas.
- [ ] Interfaces distinguibles; realizaciones punteadas; abstractos marcados.
- [ ] Ninguna flecha de Dominio o Aplicación hacia Infraestructura; ninguna asociación entre módulos de dominio.
- [ ] Los 6 patrones con estereotipo y nota: State, Strategy, Simple Factory, Decorator, Observer y Adapter.
- [ ] `verificar.py` termina en `TOTAL 0` y el PNG se revisó completo y por zonas.
- [ ] El diagrama exportado en SVG y pegado en el informe en página horizontal.

## Skills disponibles

Instaladas en `.claude/skills/` y registradas en `skills-lock.json`:

- `clean-architecture` ([facundomartinezvidal/skills](https://github.com/facundomartinezvidal/skills)): revisa, diseña y explica la estructura del software según *Clean Architecture* (regla de dependencias, capas, puertos y adaptadores, SOLID y principios de componentes).

Para reinstalarla o actualizarla:

```bash
npx skills add facundomartinezvidal/skills --skill clean-architecture -a claude-code
```
