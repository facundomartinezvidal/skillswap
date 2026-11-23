# Arquitectura

Este documento describe el backend de SkillSwap tal como se entrega. La
consigna PDF de la cátedra prevalece si difiere de esta interpretación. Las
decisiones están justificadas en [`informe-decisiones.md`](informe-decisiones.md).

## Estructura y regla de dependencias

```text
ar.edu.uade.skillswap
├── domain/                  # Entidades, value objects y reglas Java puro
│   ├── publicacion/         # Agregado Publicacion, franjas e ids
│   │   ├── state/           # Patrón State: EstadoPublicacion y sus estados
│   │   ├── decorator/       # Patrón Decorator: precio y promociones acumulables
│   │   └── exception/
│   ├── contratacion/        # Agregado Contratacion, solicitud, reintegro, reprogramación y calificaciones
│   │   ├── state/           # Patrón State: EstadoContratacion y sus estados
│   │   ├── strategy/        # Patrón Strategy: políticas de reintegro y su factory
│   │   ├── evento/          # Eventos de dominio que emite la contratación
│   │   └── exception/
│   ├── notificacion/        # Agregado Notificacion (bandeja) y su repositorio
│   │   └── exception/
│   ├── perfil/              # Agregado Perfil, reputación, disponibilidad semanal y su repositorio
│   │   └── exception/
│   ├── wallet/              # Agregado Wallet, movimientos, retención de pagos y su repositorio
│   │   └── exception/
│   └── shared/              # SkillCoins, Puntaje, CambioEstado, EventoDominio y excepciones base
├── application/             # Casos de uso (un Service por flujo), DTOs y puertos que no son repositorios
│   ├── shared/              # UnidadDeTrabajo, Ids, excepciones de aplicación
│   ├── evento/              # Patrón Observer: PublicadorEventos y SuscriptorEvento
│   ├── perfil/  publicacion/  contratacion/  wallet/
│   ├── busqueda/            # BusquedaService, BuscadorPublicaciones; recomendacion/ (Strategy)
│   └── notificacion/        # NotificadorDeInteresados (Observer), CanalEnvioFactory (Simple Factory)
├── infrastructure/          # Implementaciones de los puertos
│   ├── memoria/             # Repositorios, buscador y unidad de trabajo en memoria (tests y demo sin MySQL)
│   ├── notificacion/        # Patrón Adapter: EmailAdapter y PushAdapter sobre proveedores simulados
│   ├── identidad/           # ServicioIdentidadSimulado
│   ├── persistencia/        # JPA y MySQL: entidades, mappers, repositorios, buscador, UnidadDeTrabajoJpa, Flyway
│   └── configuracion/       # BackendEnMemoria y BackendJpa (composition roots sin Spring) y DatosDemo
├── controllers/             # UiControllers (contrato de la cátedra) e interfaces del equipo (*Operaciones,
│   │                        # RecomendacionController, CalificacionController)
│   └── impl/                # Un *ControllerImpl por interfaz, SkillSwapControllers, ManejoDeErrores, ModelosMapper
├── model/                   # Modelos de vista de la cátedra
├── mock/                    # Mock original de la cátedra (ya no lo usa main)
└── ui/                      # Interfaz provista; no contiene reglas de negocio
```

```mermaid
flowchart LR
    UI[UI Swing] --> Controllers[Controllers]
    Controllers --> Application[Casos de uso]
    Application --> Domain[Dominio]
    Application --> Ports[Puertos de salida]
    Infrastructure[Adaptadores JPA y externos] -. implementa .-> Ports
    Infrastructure --> Domain
```

La infraestructura es un detalle: la capa de dominio no importa JPA, Hibernate,
Swing, MySQL ni clases de `controllers`, `ui`, `mock` o `model`; `ArquitecturaTest`
lo verifica en cada build. Los módulos de dominio se referencian entre sí solo
por id (`PublicacionId`, `FranjaId` o el `userId` de las personas); la coordinación entre módulos corresponde a
la capa de aplicación. `ArquitecturaTest` también verifica que `application`
dependa solo del dominio y que no use `reconstruir(...)` (eso es solo para los
repositorios). La configuración de objetos es manual, por constructor:
`BackendEnMemoria` arma todo con la implementación en memoria y `BackendJpa`, los
mismos Services con los repositorios JPA. `SkillSwapApplication` usa `BackendJpa`
por defecto y `BackendEnMemoria` con `--memoria`. `ArquitecturaTest` verifica
además que los controllers no usen repositorios, infraestructura, JPA ni Swing, y
que la infraestructura no lance excepciones de negocio.

## Dominio implementado

- `Publicacion` se crea como borrador con autor y tipo (oferta o desafío). En
  `BORRADOR` se edita libremente y se agregan franjas de disponibilidad, que no
  pueden superponerse. Publicar exige título, categoría, precio, cupo de al menos
  1 y al menos una franja; si falta algo, la excepción lista todos los faltantes.
- Los estados de la publicación usan el patrón State (`domain/publicacion/state`):
  cada estado sabe qué acciones admite y el resto se rechaza con
  `TransicionInvalidaException`. `PAUSADA → PUBLICADA` (reanudar) está permitido
  como interpretación del equipo; `CERRADA` y `CANCELADA` son terminales y
  cancelar requiere motivo. Solo una publicación `PUBLICADA` admite solicitudes.
- El cupo es **por franja**: `reservarCupo(franjaId)` toma un lugar de esa franja
  y falla con `CupoAgotadoException` cuando se llena; `liberarCupo` lo devuelve.
  Lo controla el agregado `Publicacion`, no la contratación.
- Las **promociones** modifican el precio de la publicación sin producir nunca un
  total negativo (regla 6, caso de aceptación 9). El autor las agrega y las quita
  con `agregarPromocion` y `quitarPromocion` en `BORRADOR`, `PUBLICADA` y
  `PAUSADA` (`EstadoPublicacion.admitePromociones()`); en `CERRADA` o `CANCELADA`
  se rechazan con `TransicionInvalidaException`. Son acumulables, se pueden
  repetir y no cambian el estado ni el historial. Hay dos tipos:
  `DescuentoPorcentual` (1 a 100 %, el descuento se redondea hacia abajo) y
  `DescuentoFijo` (mayor a 0, con tope en 0). `precioFinal()` usa el patrón
  Decorator (`domain/publicacion/decorator`): envuelve `PrecioBase` con un
  `PrecioConPromocion` por promoción, en el orden en que se agregaron, porque el
  orden cambia el resultado (100 con 10 % y −20 da 70; con −20 y 10 %, 72).
  `precioBase()` sigue devolviendo el precio sin descuentos, y `Contratacion`
  sigue recibiendo el precio ya calculado.
- `Contratacion` también usa State (`domain/contratacion/state`): `PENDIENTE`,
  `ACEPTADA`, `CONFIRMADA`, `EN_CURSO`, `COMPLETADA`, `RECHAZADA` y `CANCELADA`.
  Valida el rol de quien actúa (el proveedor acepta, rechaza, inicia y completa;
  el cliente paga), rechaza la autosolicitud y, en `PENDIENTE`, solo deja
  cancelar al cliente (retira la solicitud).
- Al cancelar una contratación confirmada, el reintegro lo calcula una política
  del patrón Strategy (`domain/contratacion/strategy`): `ReintegroPorAnticipacion`
  devuelve 100 %, 50 % o 0 % según la anticipación (48 h y 24 h por defecto,
  configurables) y `ReintegroTotal` devuelve todo cuando cancela el proveedor.
  `PoliticaReintegroFactory` elige la política según quién cancela. Antes del
  pago el reintegro es cero.
- La reprogramación no cambia el estado: un participante propone otra franja en
  `ACEPTADA` o `CONFIRMADA`, antes del inicio, y el otro la acepta o la rechaza.
  Al aceptarla, la contratación devuelve la franja anterior para que se libere su
  cupo.
- Calificar es una acción de la `Contratacion`, no un agregado aparte: solo el
  estado `COMPLETADA` la admite (`admiteCalificacion()` del State), solo los
  participantes califican (un tercero recibe `AccionNoPermitidaException`) y
  cada uno lo hace una vez (`CalificacionDuplicadaException`). El destinatario se
  deduce como la otra parte, así que nadie puede calificarse a sí mismo. El
  puntaje (`Puntaje`, en `domain/shared`) va de 1 a 5 y el comentario es
  opcional. Calificar no cambia el estado ni agrega un cambio al historial.
- `Perfil` se identifica por el `userId` del servicio de identidad (un usuario
  tiene un solo perfil) y tiene un `TipoPerfil` fijo desde la creación:
  `PROFESIONAL` (publica ofertas) u `ORGANIZACION` (publica desafíos). Guarda
  nombre visible, biografía, zona horaria, habilidades, intereses, disponibilidad
  semanal (`DisponibilidadSemanal`: día y horario, sin superposiciones en el
  mismo día) y el canal de notificación preferido (`EMAIL`, `PUSH` o `INTERNA`,
  que es el valor inicial).
- La reputación (`Reputacion`) guarda la suma de puntajes y la cantidad de
  calificaciones, para filtrar y ordenar sin recorrerlas. No se edita: la única
  forma de cambiarla es `Perfil.registrarCalificacion(puntaje)` con el puntaje de
  una calificación que ya validó la contratación. El caso de uso de calificar
  hará las dos cosas en una transacción. Sin calificaciones, el promedio es 0 y
  `cantidad()` distingue ese caso.
- La baja del perfil es lógica (regla 9): `darDeBaja` exige motivo, queda en el
  historial (`ACTIVO → INACTIVO`) con fecha y actor, y una segunda baja se
  rechaza. Un perfil dado de baja no se edita (`PerfilInactivoException`), pero
  sigue recibiendo calificaciones de contrataciones anteriores.
- `PerfilRepository` (`findByUserId`, `save`) es el puerto de persistencia del
  perfil y vive en la raíz del módulo.
- Toda transición válida guarda estado de origen y destino, fecha, actor y motivo
  en un historial que solo crece y se expone como lista inmodificable. Una
  operación rechazada no modifica nada.
- Las operaciones de `Contratacion` que le interesan a otra parte registran un
  evento de dominio (`EventoDominio`, en `domain/shared`) después de validarse,
  en el mismo punto donde se registra el cambio de estado. Una operación
  rechazada no agrega eventos. `extraerEventos()` devuelve los pendientes en
  orden, como lista inmodificable, y vacía la lista; el caso de uso lo llamará
  una vez, después de guardar.
- Cada evento declara a quién le interesa, así la regla "a quién se notifica"
  vive en el dominio:

  | Evento | Lo emite | Interesados |
  |---|---|---|
  | `SolicitudRecibida` | `solicitar` | Proveedor |
  | `ContratacionAceptada` | `aceptar` | Cliente |
  | `PagoConfirmado` | `pagar` | Proveedor |
  | `ReprogramacionPropuesta` | `proponerReprogramacion` | La contraparte de quien propone |
  | `ReprogramacionAceptada` | `aceptarReprogramacion` | Quien propuso |
  | `ContratacionCancelada` | `cancelar` (con motivo) | La contraparte de quien cancela |
  | `SesionCompletada` | `completar` | Cliente y proveedor |

  `rechazar`, `iniciar` y `rechazarReprogramacion` no emiten eventos porque la
  consigna no los pide.
- `Notificacion` (`domain/notificacion`) es un aviso genérico en la bandeja de un
  usuario: se crea con `Notificacion.para(destinatarioId, evento)` a partir de
  cualquier `EventoDominio`, copia tipo, título, detalle y fecha, y arranca sin
  leer. El destinatario tiene que estar entre los interesados del evento. Solo
  el destinatario puede marcarla como leída (`NotificacionAjenaException`) y
  marcarla dos veces no cambia nada. No conoce `Contratacion`: depende solo de
  `domain/shared`, y `ArquitecturaTest` lo verifica. El canal (interna, email o
  push) no es parte de la notificación; se resolverá en aplicación según el
  perfil.
- Los objetos de dominio no son entidades JPA ni DTOs de la UI.

### Wallet

- `Wallet` se identifica por el `userId` del titular, igual que `Contratacion`.
  Se abre con un bono de bienvenida (el monto lo decide el punto de composición,
  no el dominio) que queda registrado como `ACREDITACION_DEMO`;
  `acreditarDemo` permite cargar más saldo de demostración.
- El saldo nunca es negativo (regla 4): `debitar` más de lo que hay lanza
  `SaldoInsuficienteException` sin modificar nada. `acreditar` y `reintegrar`
  suman al saldo. Los importes deben ser mayores que 0.
- Cada operación crea un `Movimiento` inmutable con su tipo (`CREDITO`, `DEBITO`,
  `REINTEGRO`, `ACREDITACION_DEMO`, los mismos del esquema), importe, saldo
  resultante, concepto, contratación y fecha. La Wallet guarda el saldo y solo
  los movimientos nuevos; el historial completo y paginado lo listará el
  repositorio (`WalletRepository`).
- **Retención del pago.** Lo pagado no va directo al proveedor: queda retenido en
  una Wallet de garantía de la plataforma hasta que la contratación se completa.
  El servicio de dominio `RetencionDePagos` agrupa cada débito con su crédito para
  que no puedan separarse: `retener` (cliente → garantía, al pagar),
  `liberarAlProveedor` (garantía → proveedor, al completar) y `repartir`
  (garantía → cliente y proveedor según el `Reintegro`, al cancelar después del
  pago; un monto 0 no genera movimiento y el reparto tiene que sumar el total
  retenido). Siempre debita primero, así que si el débito falla ninguna Wallet
  cambia, y la suma de los saldos se conserva.
- `wallet` conoce a `contratacion` solo por `ContratacionId`. Coordinar la
  transición de la contratación con el movimiento de fondos, y guardarlos en una
  misma transacción, es tarea del caso de uso. El doble cobro lo impide el estado
  de `Contratacion` (solo se paga desde `ACEPTADA`) y, en persistencia, la
  restricción única `(contratacion_id, wallet_perfil_id, tipo)`.
- Wallet no tiene estados ni variaciones, así que no introduce un patrón nuevo.

## Casos de uso y puertos

Un Service por flujo, que se corresponde con las interfaces de `UiControllers`.
Coordinan agregados que se conocen solo por id: las reglas viven en el dominio y
el Service no tiene reglas de negocio. Reciben el `userId` de quien actúa y
devuelven DTOs (records con colecciones inmodificables).

| Service | Qué hace | Casos de aceptación |
|---|---|---|
| `PerfilService` | Alta validada contra la identidad, con Wallet y bono en una transacción; edición, disponibilidad, canal, baja lógica, perfil público | — |
| `PublicacionService` | Borrador (el tipo sale del perfil), franjas, publicar, pausar, reanudar, cerrar, cancelar, promociones; solo el autor modifica; BORRADOR y PAUSADA solo los ve el autor | 2, 9 |
| `BusquedaService` | Búsqueda con filtros, orden y paginación; recomendaciones con una `EstrategiaRecomendacion` intercambiable | 1, 7 |
| `ContratacionService` | Solicitar, aceptar (reserva el cupo), pagar (retiene en garantía), iniciar, completar (libera al proveedor), cancelar con reintegro, reprogramar, calificar | 3, 4, 5, 6, 10 |
| `WalletService` | Saldo, movimientos paginados, acreditación de demo | — |
| `NotificacionService` | Bandeja paginada, marcar leída, marcar todas | 8 |

**Puertos.** Los repositorios están en la raíz de cada módulo de dominio
(`PublicacionRepository`, `ContratacionRepository`, `WalletRepository`,
`PerfilRepository`, `NotificacionRepository`), y los listados devuelven
`Pagina<T>` (en `domain/shared`). En `application` están los puertos que no son
repositorios: `UnidadDeTrabajo`, `BuscadorPublicaciones` (la búsqueda cruza
publicación y perfil), `ServicioIdentidad` y `EnvioNotificacion`.

**Atomicidad.** Aceptar, pagar, completar, cancelar, reprogramar, calificar y el
alta de perfil corren en una `UnidadDeTrabajo`: o se guarda todo o nada. Con JPA
será una `EntityTransaction`. En memoria, `UnidadDeTrabajoEnMemoria` anota los
`save` y los aplica recién si la operación termina bien. Los repositorios en
memoria devuelven copias (con `reconstruir`), como una base.

**Eventos.** Después de confirmar, el Service pasa `extraerEventos()` al
`PublicadorEventos`. El suscriptor `NotificadorDeInteresados` deja la
notificación en la bandeja de cada interesado y la envía por el canal de su
perfil. Un suscriptor que falla no deshace la operación ni corta a los demás.

**Decisiones de la revisión de la capa de aplicación** (PRs #12 a #15):

- El cupo se reserva al **aceptar** y no depende de que la publicación admita
  solicitudes nuevas: con la publicación pausada se puede aceptar una solicitud
  que ya llegó y reprogramar una contratación existente. Las solicitudes nuevas
  las frena `Publicacion.franjaParaSolicitar`.
- Una franja que ya empezó no se puede elegir.
- `EstadoContratacion.ocupaCupo()` y `Publicacion.esVisiblePara(userId)` dejan
  esas reglas en el dominio.
- Limitación conocida: `findPublicadas()` no tiene límite (las recomendaciones
  cargan todas las publicadas).

## Persistencia (JPA y MySQL)

- **Esquema:** lo define solo Flyway (`V1__esquema_inicial.sql`); Hibernate corre con
  `hbm2ddl.auto=validate`, así que si una entidad no coincide con la tabla la app no arranca.
  `Migraciones` corre Flyway al construir `BackendJpa`. La Wallet de garantía la inserta la migración.
- **Entidades** solo en `infrastructure/persistencia/<módulo>`, una por tabla (las colecciones de textos,
  como etiquetas y habilidades, son `@ElementCollection`). Un mapper por agregado arma el dominio con
  `reconstruir(...)` y los estados se guardan por nombre (`"PUBLICADA"`).
- **Transacciones:** `UnidadDeTrabajoJpa` abre un `EntityManager` por transacción (en un `ThreadLocal`),
  hace `begin`/`commit` y `rollback` si algo falla. Los repositorios usan ese `EntityManager`; fuera de
  una transacción, cada llamada abre una transacción corta propia.
- **Concurrencia (caso 4):** `@Version` en publicación, wallet, contratación y perfil. Guardar una
  publicación siempre sube su versión (`OPTIMISTIC_FORCE_INCREMENT`), aunque solo cambie el cupo de una
  franja: de dos aceptaciones simultáneas sobre el último lugar, la segunda falla al confirmar y se
  traduce a `ConflictoDeConcurrenciaException`.
- **Unicidad:** `UNIQUE(contratacion_id, titular_id, tipo)` en movimientos impide el doble cobro, y
  `UNIQUE(contratacion_id, autor_id)` en calificaciones, la calificación duplicada.
- **Denormalizaciones:** `publicaciones.precio_final` (con promociones) y `publicada_en` se calculan con
  el dominio al guardar, para filtrar y ordenar la búsqueda en SQL.
- **Sin N+1:** los listados buscan primero los ids de la página (con una consulta de `count` para el
  total) y después cargan ese lote con una consulta `join fetch` por colección.
- **MySQL local:** `docker compose up -d` levanta MySQL 8 en el puerto 3307 (usuario y contraseña de
  desarrollo `skillswap`). `DB_URL`, `DB_USER` y `DB_PASSWORD` cambian la conexión.

## Controllers e integración con la UI

- `SkillSwapControllers` implementa `UiControllers` (sin modificarlo) y se construye con los seis
  Services. Hay un `*ControllerImpl` por sub-interfaz; cada uno valida el `userId`, delega en un
  Service y mapea el DTO a `Models` con `ModelosMapper`.
- Lo que la consigna pide y la UI no expone está en interfaces del equipo: `ContratacionOperaciones`
  (aceptar, rechazar, pagar, iniciar, completar, cancelar con motivo, reprogramar), `PublicacionOperaciones`
  (franjas y descuentos), `PerfilOperaciones`, `BusquedaOperaciones` (todos los filtros, cuatro órdenes,
  paginación), `WalletOperaciones`, `NotificacionOperaciones`, `RecomendacionController` y
  `CalificacionController`. Los listados paginados devuelven `PaginaVista<T>`.
- **Errores:** `ManejoDeErrores` traduce `DomainException`, `ApplicationException` e
  `IllegalArgumentException` a `OperacionRechazadaException`; `SkillSwapApplication` muestra el mensaje
  en un `JOptionPane`, porque la vista no atrapa excepciones.
- **Adaptaciones a la UI:** `avanzar` decide según estado y rol (aceptar, pagar, iniciar o completar);
  `solicitar` usa la primera franja futura con lugar; `crearBorrador` crea con cupo 1 y una franja a
  7 días; los listados de la UI piden la primera página de 50.
- **Datos de demo:** `DatosDemo` los carga con los Services (sirve igual con MySQL y en memoria) y es
  idempotente: si Mateo ya tiene perfil, no hace nada.

## Patrones

| Patrón | Variación que resuelve | Dónde | Evidencia |
|---|---|---|---|
| State | Qué acciones admite cada estado de publicación y de contratación | `domain/*/state` | `EstadoPublicacionTest`, `EstadoContratacionTest` |
| Strategy | Política de reintegro según quién cancela; estrategia de recomendación | `domain/contratacion/strategy`, `application/busqueda/recomendacion` | `PoliticaReintegroTest`, `RecomendacionTest` (caso 7) |
| Simple Factory | Elegir la política de reintegro; elegir el canal de envío según el perfil | `PoliticaReintegroFactory`, `CanalEnvioFactory` | `PoliticaReintegroFactoryTest`, `CanalEnvioFactoryTest` |
| Decorator | Promociones acumulables sobre el precio | `domain/publicacion/decorator` | `PromocionTest`, `PrecioTest`, `PublicacionServiceTest` (caso 9) |
| Observer | Un evento interesa a varias personas y puede tener varias reacciones | `PublicadorEventos`, `NotificadorDeInteresados` | `PublicadorEventosTest`, `NotificadorDeInteresadosTest` (caso 8) |
| Adapter | Proveedores externos de email, push e identidad, y la persistencia JPA, reemplazables | `infrastructure/notificacion`, `infrastructure/identidad`, `infrastructure/persistencia` | `AdaptersDeNotificacionTest`, caso 8, tests de persistencia |

No se introducen patrones sin una variación concreta.

## Migraciones

`src/main/resources/db/migration/V1__esquema_inicial.sql` define el esquema:
claves foráneas, restricciones de unicidad, índices y columnas de versión para
publicaciones, contrataciones, wallets y perfiles. Flyway es la única fuente de
verdad del esquema: `Migraciones.aplicar` corre al construir `BackendJpa` (o sea,
al arrancar la app) y Hibernate solo valida. Un cambio futuro va en una `V2`.

## Pruebas

- `mvn test`: 573 tests. Los 26 de `infrastructure/persistencia` usan Testcontainers
  y se omiten sin Docker.
- JaCoCo: reporte en `target/site/jacoco/index.html`. Dominio + aplicación: 99,4 % de
  líneas y 94,9 % de ramas; `mvn verify` exige al menos 80 % de líneas.
- `CasosDeAceptacionTest` recorre los 10 casos de aceptación a través de los controllers;
  `CasosNegativosTest`, los rechazos y sus mensajes.
