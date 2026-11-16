# Arquitectura

Este documento describe la base de código generada para continuar el TPI. La
consigna PDF de la cátedra prevalece si difiere de esta interpretación.

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
│   └── configuracion/       # BackendEnMemoria: composition root sin Spring
│                            # (pendiente: JPA y Flyway, del equipo de persistencia)
├── controllers/             # Contrato actual de la UI Swing
├── model/                   # Modelos de vista existentes
├── mock/                    # Implementación de demostración existente
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
`BackendEnMemoria` arma todo con la implementación en memoria, y la versión con
base de datos será otro composition root igual que cambie los repositorios.

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

## Patrones

| Patrón | Variación que resuelve | Dónde | Evidencia |
|---|---|---|---|
| State | Qué acciones admite cada estado de publicación y de contratación | `domain/*/state` | `EstadoPublicacionTest`, `EstadoContratacionTest` |
| Strategy | Política de reintegro según quién cancela; estrategia de recomendación | `domain/contratacion/strategy`, `application/busqueda/recomendacion` | `PoliticaReintegroTest`, `RecomendacionTest` (caso 7) |
| Simple Factory | Elegir la política de reintegro; elegir el canal de envío según el perfil | `PoliticaReintegroFactory`, `CanalEnvioFactory` | `PoliticaReintegroFactoryTest`, `CanalEnvioFactoryTest` |
| Decorator | Promociones acumulables sobre el precio | `domain/publicacion/decorator` | `PromocionTest`, `PrecioTest`, `PublicacionServiceTest` (caso 9) |
| Observer | Un evento interesa a varias personas y puede tener varias reacciones | `PublicadorEventos`, `NotificadorDeInteresados` | `PublicadorEventosTest`, `NotificadorDeInteresadosTest` (caso 8) |
| Adapter | Proveedores externos de email, push e identidad, reemplazables | `infrastructure/notificacion`, `infrastructure/identidad` | `AdaptersDeNotificacionTest`, caso 8 |

No se introducen patrones sin una variación concreta.

## Migraciones

`src/main/resources/db/migration/V1__esquema_inicial.sql` define el esquema
relacional previsto, claves foráneas, restricciones, índices y control de
versiones para publicaciones, contrataciones y wallets. Los datos se mantienen
en tablas relacionales normalizadas; las migraciones, no Hibernate, serán la
fuente de verdad del esquema. Las entidades/adaptadores JPA deben mapear estas
tablas y validar el esquema en vez de crearlo.

La migración todavía no se ejecuta automáticamente desde la UI: falta definir la
configuración de conexión local y el adaptador de arranque una vez que el equipo
prepare MySQL.
