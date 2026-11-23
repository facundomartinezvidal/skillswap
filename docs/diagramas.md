# Diagramas de clases: capa de aplicación e infraestructura

Diagramas de clases hechos desde el código real (dominio y aplicación, con relaciones,
multiplicidades y patrones identificados). Hay tres formatos:

- **PlantUML:** [`diagrama-clases.puml`](diagrama-clases.puml) y su PNG (vista general) y uno por módulo
  en [`diagramas/`](diagramas/) (publicacion, contratacion, perfil, wallet, notificacion, busqueda, shared).
- **StarUML:** [`SkillSwap.mdj`](SkillSwap.mdj), con el modelo completo y 9 diagramas (paquetes,
  "Dominio y aplicación" y uno por módulo). La disposición de las cajas es en grilla y conviene
  ordenarla al abrirlo.
- **Mermaid:** este archivo. Las secciones 1 a 4 muestran la capa de aplicación (por partes, para que
  se pueda leer) y cómo la implementa la infraestructura; la 5, cada agregado del dominio, y la 6, las
  dependencias entre capas.

Todo se generó con un script del equipo que lee las fuentes Java; el PNG se renderizó con PlantUML
(layout smetana) y el `.mdj` se validó estructuralmente (JSON, ids únicos, sin `$ref` rotos).

Convenciones: `..>` depende de (usa), `..|>` implementa, `--|>` hereda, `*--` compone, `o--` agrega, `-->` tiene una referencia (campo).
Los estereotipos marcan el patrón: `<<Observer>>`, `<<Strategy>>`, `<<SimpleFactory>>`, `<<Adapter>>`.

## 1. Services y puertos

```mermaid
classDiagram
    direction LR
    class PerfilService {
        +crearPerfil(CrearPerfilCommand) PerfilDTO
        +editarPerfil(userId, EditarPerfilCommand) PerfilDTO
        +agregarDisponibilidad(userId, DisponibilidadDTO) PerfilDTO
        +quitarDisponibilidad(userId, DisponibilidadDTO) PerfilDTO
        +elegirCanal(userId, CanalNotificacion) PerfilDTO
        +darDeBaja(userId, motivo)
        +verPerfil(userId) PerfilDTO
    }
    class PublicacionService {
        +crearBorrador(userId, DatosPublicacionCommand) PublicacionDTO
        +editar(userId, id, DatosPublicacionCommand) PublicacionDTO
        +agregarFranja(userId, id, inicio, fin) PublicacionDTO
        +publicar(userId, id) PublicacionDTO
        +pausar(userId, id, motivo) PublicacionDTO
        +reanudar(userId, id) PublicacionDTO
        +cerrar(userId, id, motivo) PublicacionDTO
        +cancelar(userId, id, motivo) PublicacionDTO
        +agregarPromocion(userId, id, PromocionDTO) PublicacionDTO
        +quitarPromocion(userId, id, PromocionDTO) PublicacionDTO
        +listarPropias(userId, PaginaSolicitada) Pagina~PublicacionDTO~
        +verPublicacion(userId, id) PublicacionDTO
    }
    class BusquedaService {
        +buscar(CriteriosBusqueda, PaginaSolicitada) Pagina~PublicacionDTO~
        +recomendar(userId, cantidad) List~PublicacionDTO~
        +categorias() List~String~
    }
    class ContratacionService {
        +solicitar(SolicitarContratacionCommand) ContratacionDTO
        +aceptar(userId, id) ContratacionDTO
        +rechazar(userId, id, motivo) ContratacionDTO
        +pagar(userId, id) ContratacionDTO
        +iniciar(userId, id) ContratacionDTO
        +completar(userId, id) ContratacionDTO
        +cancelar(userId, id, motivo) ContratacionDTO
        +proponerReprogramacion(userId, id, franjaId) ContratacionDTO
        +aceptarReprogramacion(userId, id) ContratacionDTO
        +rechazarReprogramacion(userId, id) ContratacionDTO
        +calificar(userId, id, puntaje, comentario) CalificacionDTO
        +listarContrataciones(userId, PaginaSolicitada) Pagina~ContratacionDTO~
        +verContratacion(userId, id) ContratacionDTO
    }
    class WalletService {
        +consultarSaldo(userId) long
        +listarMovimientos(userId, PaginaSolicitada) Pagina~MovimientoDTO~
        +acreditarDemo(userId, importe) long
    }
    class NotificacionService {
        +listar(userId, PaginaSolicitada) Pagina~NotificacionDTO~
        +marcarLeida(userId, id)
        +marcarTodasLeidas(userId)
    }
    class UnidadDeTrabajo { <<interface>> +ejecutar(Supplier~T~) T }
    class BuscadorPublicaciones { <<interface>> +buscar(CriteriosBusqueda, PaginaSolicitada) Pagina~Publicacion~ }
    class ServicioIdentidad { <<interface>> +existe(userId) boolean }
    class PerfilRepository { <<interface>> }
    class PublicacionRepository { <<interface>> }
    class ContratacionRepository { <<interface>> }
    class WalletRepository { <<interface>> }
    class NotificacionRepository { <<interface>> }

    PerfilService ..> PerfilRepository
    PerfilService ..> WalletRepository
    PerfilService ..> PublicacionRepository
    PerfilService ..> ContratacionRepository
    PerfilService ..> ServicioIdentidad
    PerfilService ..> UnidadDeTrabajo
    PublicacionService ..> PublicacionRepository
    PublicacionService ..> PerfilRepository
    BusquedaService ..> BuscadorPublicaciones
    BusquedaService ..> PublicacionRepository
    BusquedaService ..> PerfilRepository
    ContratacionService ..> ContratacionRepository
    ContratacionService ..> PublicacionRepository
    ContratacionService ..> WalletRepository
    ContratacionService ..> PerfilRepository
    ContratacionService ..> UnidadDeTrabajo
    WalletService ..> WalletRepository
    NotificacionService ..> NotificacionRepository
    NotificacionService ..> UnidadDeTrabajo
```

## 2. Patrones en la capa de aplicación

```mermaid
classDiagram
    direction LR
    class ContratacionService
    class PublicadorEventos {
        <<Observer: sujeto>>
        -List~SuscriptorEvento~ suscriptores
        +suscribir(SuscriptorEvento)
        +publicar(List~EventoDominio~)
    }
    class SuscriptorEvento { <<interface>> +actualizar(EventoDominio) }
    class NotificadorDeInteresados { <<Observer: observador>> +actualizar(EventoDominio) }
    class CanalEnvioFactory { <<SimpleFactory>> +para(CanalNotificacion) EnvioNotificacion }
    class EnvioNotificacion { <<interface>> +enviar(destinatarioId, titulo, detalle) }
    class EventoDominio { <<interface>> +tipo() +interesados() +titulo() +detalle() +ocurridoEn() }
    class BusquedaService
    class EstrategiaRecomendacion { <<Strategy>> +recomendar(Perfil, List~Publicacion~, cantidad) List~Publicacion~ }
    class AfinidadPorHabilidades
    class PopularidadReciente { -Duration ventana }
    class PoliticaReintegroFactory { <<SimpleFactory, dominio>> +paraCancelacion(Contratacion, userId) PoliticaReintegro }
    class PoliticaReintegro { <<Strategy, dominio>> +calcular(precio, inicio, ahora) Reintegro }

    ContratacionService ..> PublicadorEventos : publica después de confirmar
    ContratacionService ..> PoliticaReintegroFactory
    PoliticaReintegroFactory ..> PoliticaReintegro
    PublicadorEventos "1" o-- "*" SuscriptorEvento
    PublicadorEventos ..> EventoDominio
    NotificadorDeInteresados ..|> SuscriptorEvento
    NotificadorDeInteresados ..> CanalEnvioFactory
    CanalEnvioFactory ..> EnvioNotificacion
    BusquedaService "1" --> "1" EstrategiaRecomendacion
    AfinidadPorHabilidades ..|> EstrategiaRecomendacion
    PopularidadReciente ..|> EstrategiaRecomendacion
```

## 3. Infraestructura: implementaciones de los puertos

```mermaid
classDiagram
    direction LR
    class BackendEnMemoria {
        <<composition root>>
        +perfilService
        +publicacionService
        +busquedaService
        +contratacionService
        +walletService
        +notificacionService
    }
    class UnidadDeTrabajoEnMemoria { -List~Runnable~ pendientes +ejecutar(Supplier~T~) T ~registrar(Runnable) }
    class PerfilRepositoryEnMemoria
    class PublicacionRepositoryEnMemoria
    class ContratacionRepositoryEnMemoria
    class WalletRepositoryEnMemoria
    class NotificacionRepositoryEnMemoria
    class BuscadorPublicacionesEnMemoria
    class ServicioIdentidadSimulado
    class EmailAdapter { <<Adapter>> }
    class PushAdapter { <<Adapter>> }
    class ProveedorEmailSimulado { +enviarCorreo(Correo) }
    class ProveedorPushSimulado { +notificar(token, texto) }
    class UnidadDeTrabajo { <<interface>> }
    class EnvioNotificacion { <<interface>> }
    class ServicioIdentidad { <<interface>> }
    class BuscadorPublicaciones { <<interface>> }

    UnidadDeTrabajoEnMemoria ..|> UnidadDeTrabajo
    BuscadorPublicacionesEnMemoria ..|> BuscadorPublicaciones
    ServicioIdentidadSimulado ..|> ServicioIdentidad
    EmailAdapter ..|> EnvioNotificacion
    PushAdapter ..|> EnvioNotificacion
    EmailAdapter --> ProveedorEmailSimulado
    PushAdapter --> ProveedorPushSimulado
    PerfilRepositoryEnMemoria --> UnidadDeTrabajoEnMemoria
    PublicacionRepositoryEnMemoria --> UnidadDeTrabajoEnMemoria
    ContratacionRepositoryEnMemoria --> UnidadDeTrabajoEnMemoria
    WalletRepositoryEnMemoria --> UnidadDeTrabajoEnMemoria
    NotificacionRepositoryEnMemoria --> UnidadDeTrabajoEnMemoria
    BackendEnMemoria *-- UnidadDeTrabajoEnMemoria
    BackendEnMemoria *-- "5" PerfilRepositoryEnMemoria : y los otros repositorios
    BackendEnMemoria ..> EmailAdapter
    BackendEnMemoria ..> PushAdapter
    BackendEnMemoria ..> ServicioIdentidadSimulado
```

Los repositorios en memoria implementan los puertos del dominio: `PerfilRepositoryEnMemoria ..|> PerfilRepository`, y lo mismo con publicación, contratación, wallet y notificación.

## 4. DTOs y excepciones de aplicación

```mermaid
classDiagram
    class ApplicationException { <<abstract>> }
    class RecursoNoEncontradoException
    class PermisoDenegadoException
    class PerfilDuplicadoException
    RecursoNoEncontradoException --|> ApplicationException
    PermisoDenegadoException --|> ApplicationException
    PerfilDuplicadoException --|> ApplicationException

    class ContratacionDTO { <<record>> id, publicacionId, tituloPublicacion, clienteId, proveedorId, franjaId, inicioSesion, precioAcordado, estado }
    class CambioEstadoDTO { <<record>> desde, hacia, fecha, actorId, motivo }
    class CalificacionDTO { <<record>> autorId, destinatarioId, puntaje, comentario, fecha }
    class ReintegroDTO { <<record>> alCliente, alProveedor }
    class PropuestaReprogramacionDTO { <<record>> propuestaPor, franjaId, inicio }
    ContratacionDTO "1" *-- "*" CambioEstadoDTO : historial
    ContratacionDTO "1" *-- "*" CalificacionDTO : calificaciones
    ContratacionDTO "1" *-- "0..1" ReintegroDTO
    ContratacionDTO "1" *-- "0..1" PropuestaReprogramacionDTO

    class PublicacionDTO { <<record>> id, autorId, autorNombre, reputacionAutor, tipo, titulo, categoria, modalidad, precioBase, precioFinal, estado }
    class FranjaDTO { <<record>> id, inicio, fin, lugaresLibres }
    class PromocionDTO { <<record>> tipo, valor }
    PublicacionDTO "1" *-- "*" FranjaDTO
    PublicacionDTO "1" *-- "*" PromocionDTO

    class PerfilDTO { <<record>> userId, tipo, nombreVisible, canal, reputacion, cantidadCalificaciones, contratacionesCompletadas, activo }
    class DisponibilidadDTO { <<record>> dia, inicio, fin }
    PerfilDTO "1" *-- "*" DisponibilidadDTO
    PerfilDTO "1" *-- "*" PublicacionDTO : publicacionesActivas
```

Entrada (commands): `CrearPerfilCommand`, `EditarPerfilCommand`, `DatosPublicacionCommand`,
`SolicitarContratacionCommand` y `CriteriosBusqueda` (con `OrdenBusqueda`). Salida, además de las
de arriba: `MovimientoDTO` y `NotificacionDTO`.

<!-- generado:inicio (herramientas/diagramas/generar.py) -->

## 5. Agregados del dominio

Generados desde `src/main/java/.../domain`. Cada agregado muestra atributos y operaciones públicas (sin los accesores triviales `x()`), las partes que compone y las clases de otros módulos con las que tiene una relación estructural (sin miembros, con el módulo como estereotipo). Los estados concretos, los eventos y las excepciones se dibujan sin miembros: repiten la interfaz o la superclase. Los ids (`PerfilId`, `PublicacionId`, ...) se ven como dependencia hacia el agregado que identifican.

### 5.1 Publicación

22 clases en `domain.publicacion`, 29 relaciones.

```mermaid
classDiagram
    direction LR
    class FranjaDisponibilidad {
        -FranjaId id
        -Instant inicio
        -Instant fin
        -int reservados
        +reconstruir(FranjaId, Instant, Instant, int) FranjaDisponibilidad$
        +estaOcupada(int) boolean
    }
    class FranjaId {
        <<record>>
        +UUID valor
        +generate() FranjaId$
    }
    class Modalidad {
        <<enumeration>>
        REMOTA
        PRESENCIAL
        HIBRIDA
    }
    class Publicacion {
        -PublicacionId id
        -String autorId
        -TipoPublicacion tipo
        -String titulo
        -String descripcion
        -String categoria
        -Modalidad modalidad
        -Duration duracionEstimada
        -int cupo
        -SkillCoins precioBase
        -Set~String~ etiquetas
        -EstadoPublicacion estado
        -String motivoCancelacion
        -List~FranjaDisponibilidad~ franjas
        -List~Promocion~ promociones
        -List~CambioEstado~ historial
        +crearBorrador(String, TipoPublicacion) Publicacion$
        +reconstruir(...) Publicacion$
        +editar(String, String, String, Modalidad, Duration, int, SkillCoins, Set~String~)
        +agregarFranja(Instant, Instant) FranjaId
        +publicar(String, Instant)
        +pausar(String, String, Instant)
        +reanudar(String, Instant)
        +cerrar(String, String, Instant)
        +cancelar(String, String, Instant)
        +admiteSolicitudes() boolean
        +franjaParaSolicitar(FranjaId, Instant) FranjaDisponibilidad
        +franjaDisponible(FranjaId, Instant) FranjaDisponibilidad
        +reservarCupo(FranjaId)
        +esVisiblePara(String) boolean
        +liberarCupo(FranjaId)
        +agregarPromocion(Promocion)
        +quitarPromocion(Promocion)
        +precioFinal() SkillCoins
    }
    class PublicacionId {
        <<record>>
        +UUID valor
        +generate() PublicacionId$
    }
    class PublicacionRepository {
        <<interface>>
        +findById(PublicacionId) Optional~Publicacion~
        +save(Publicacion)
        +findByAutor(String, PaginaSolicitada) Pagina~Publicacion~
        +findActivasByAutor(String) List~Publicacion~
        +findByIds(Collection~PublicacionId~) List~Publicacion~
        +findPublicadas() List~Publicacion~
        +categorias() List~String~
    }
    class TipoPublicacion {
        <<enumeration>>
        OFERTA
        DESAFIO
    }
    class DescuentoFijo {
        <<record, Decorator>>
        +SkillCoins monto
        +aplicar(SkillCoins) SkillCoins
    }
    class DescuentoPorcentual {
        <<record, Decorator>>
        +int porcentaje
        +aplicar(SkillCoins) SkillCoins
    }
    class Precio {
        <<interface, Decorator>>
        +valor() SkillCoins
    }
    class PrecioBase {
        <<record, Decorator>>
        +SkillCoins valor
    }
    class PrecioConPromocion {
        <<Decorator>>
        -Precio envuelto
        -Promocion promocion
        +valor() SkillCoins
    }
    class Promocion {
        <<interface, Decorator>>
        +aplicar(SkillCoins) SkillCoins
    }
    class CupoAgotadoException
    class FranjaInvalidaException
    class PublicacionIncompletaException
    class Borrador {
        <<record, State>>
    }
    class Cancelada {
        <<record, State>>
    }
    class Cerrada {
        <<record, State>>
    }
    class EstadoPublicacion {
        <<interface, State>>
        +nombre() String
        +publicar() EstadoPublicacion
        +pausar() EstadoPublicacion
        +reanudar() EstadoPublicacion
        +cerrar() EstadoPublicacion
        +cancelar() EstadoPublicacion
        +editar()
        +admiteSolicitudes() boolean
        +esVisibleParaTodos() boolean
        +admiteReservas() boolean
        +admitePromociones() boolean
    }
    class Pausada {
        <<record, State>>
    }
    class Publicada {
        <<record, State>>
    }
    class DomainException {
        <<abstract, Domain shared>>
    }
    class CambioEstado {
        <<record, Domain shared>>
    }
    CupoAgotadoException --|> DomainException
    FranjaInvalidaException --|> DomainException
    PublicacionIncompletaException --|> DomainException
    DescuentoFijo ..|> Promocion
    DescuentoPorcentual ..|> Promocion
    PrecioBase ..|> Precio
    PrecioConPromocion ..|> Precio
    Borrador ..|> EstadoPublicacion
    Cancelada ..|> EstadoPublicacion
    Cerrada ..|> EstadoPublicacion
    Pausada ..|> EstadoPublicacion
    Publicada ..|> EstadoPublicacion
    Publicacion "1" *-- "*" FranjaDisponibilidad : franjas
    Publicacion "1" *-- "*" Promocion : promociones
    Publicacion "1" *-- "*" CambioEstado : historial
    Publicacion --> "1" EstadoPublicacion : estado
    PrecioConPromocion --> "1" Promocion : promocion
    Publicacion ..> PrecioBase
    Publicacion ..> PrecioConPromocion
    Publicacion ..> Borrador
    PublicacionRepository ..> Publicacion
    Borrador ..> Cancelada
    Borrador ..> Publicada
    Pausada ..> Cancelada
    Pausada ..> Cerrada
    Pausada ..> Publicada
    Publicada ..> Cancelada
    Publicada ..> Cerrada
    Publicada ..> Pausada
```

### 5.2 Contratación

31 clases en `domain.contratacion`, 59 relaciones.

```mermaid
classDiagram
    direction LR
    class Calificacion {
        <<record>>
        +CalificacionId id
        +String autorId
        +String destinatarioId
        +Puntaje puntaje
        +String comentario
        +Instant fecha
    }
    class CalificacionId {
        <<record>>
        +UUID valor
        +generate() CalificacionId$
    }
    class Contratacion {
        -ContratacionId id
        -PublicacionId publicacionId
        -String clienteId
        -String proveedorId
        -Solicitud solicitud
        -FranjaId franjaId
        -Instant inicioSesion
        -SkillCoins precioAcordado
        -EstadoContratacion estado
        -PropuestaReprogramacion propuesta
        -Reintegro reintegro
        -List~CambioEstado~ historial
        -List~Calificacion~ calificaciones
        -List~EventoDominio~ eventos
        +solicitar(String, String, PublicacionId, Solicitud, Instant, SkillCoins, Instant) Contratacion$
        +reconstruir(...) Contratacion$
        +aceptar(String, Instant)
        +rechazar(String, String, Instant)
        +pagar(String, Instant)
        +iniciar(String, Instant)
        +completar(String, Instant)
        +cancelar(String, String, PoliticaReintegro, Instant) Reintegro
        +proponerReprogramacion(String, FranjaId, Instant, Instant)
        +aceptarReprogramacion(String, Instant) FranjaId
        +rechazarReprogramacion(String, Instant)
        +calificar(String, Puntaje, String, Instant) Calificacion
        +validarNuevaSolicitud(String)
        +ocupaCupo() boolean
        +esParticipante(String) boolean
        +extraerEventos() List~EventoDominio~
    }
    class ContratacionId {
        <<record>>
        +UUID valor
        +generate() ContratacionId$
    }
    class ContratacionRepository {
        <<interface>>
        +findById(ContratacionId) Optional~Contratacion~
        +save(Contratacion)
        +findByParticipante(String, PaginaSolicitada) Pagina~Contratacion~
        +countCompletadasDe(String) long
        +countAceptadasDesde(Instant) Map~PublicacionId, Long~
    }
    class PropuestaReprogramacion {
        <<record>>
        +String propuestaPor
        +FranjaId franjaId
        +Instant inicio
    }
    class Reintegro {
        <<record>>
        +SkillCoins alCliente
        +SkillCoins alProveedor
        +cero() Reintegro$
    }
    class Solicitud {
        <<record>>
        +String mensaje
        +String objetivo
        +FranjaId franjaElegida
    }
    class ContratacionAceptada {
        <<record>>
    }
    class ContratacionCancelada {
        <<record>>
    }
    class PagoConfirmado {
        <<record>>
    }
    class ReprogramacionAceptada {
        <<record>>
    }
    class ReprogramacionPropuesta {
        <<record>>
    }
    class SesionCompletada {
        <<record>>
    }
    class SolicitudRecibida {
        <<record>>
    }
    class AccionNoPermitidaException
    class AutoSolicitudException
    class CalificacionDuplicadaException
    class ReprogramacionInvalidaException
    class Aceptada {
        <<record, State>>
    }
    class Cancelada {
        <<record, State>>
    }
    class Completada {
        <<record, State>>
    }
    class Confirmada {
        <<record, State>>
    }
    class EnCurso {
        <<record, State>>
    }
    class EstadoContratacion {
        <<interface, State>>
        +nombre() String
        +aceptar() EstadoContratacion
        +rechazar() EstadoContratacion
        +pagar() EstadoContratacion
        +iniciar() EstadoContratacion
        +completar() EstadoContratacion
        +cancelar() EstadoContratacion
        +admiteReprogramacion() boolean
        +ocupaCupo() boolean
        +admiteCalificacion() boolean
        +admiteNuevaSolicitud() boolean
    }
    class Pendiente {
        <<record, State>>
    }
    class Rechazada {
        <<record, State>>
    }
    class PoliticaReintegro {
        <<interface, Strategy>>
        +calcular(SkillCoins, Instant, Instant) Reintegro
    }
    class PoliticaReintegroFactory {
        <<Simple Factory>>
        -ReintegroPorAnticipacion porAnticipacion
        -ReintegroTotal total
        +paraCancelacion(Contratacion, String) PoliticaReintegro
    }
    class ReintegroPorAnticipacion {
        <<Strategy>>
        -Duration para100
        -Duration para50
        +conUmbralesPorDefecto() ReintegroPorAnticipacion$
        +calcular(SkillCoins, Instant, Instant) Reintegro
    }
    class ReintegroTotal {
        <<Strategy>>
        +calcular(SkillCoins, Instant, Instant) Reintegro
    }
    class DomainException {
        <<abstract, Domain shared>>
    }
    class EventoDominio {
        <<interface, Domain shared>>
    }
    class CambioEstado {
        <<record, Domain shared>>
    }
    AccionNoPermitidaException --|> DomainException
    AutoSolicitudException --|> DomainException
    CalificacionDuplicadaException --|> DomainException
    ReprogramacionInvalidaException --|> DomainException
    ContratacionAceptada ..|> EventoDominio
    ContratacionCancelada ..|> EventoDominio
    PagoConfirmado ..|> EventoDominio
    ReprogramacionAceptada ..|> EventoDominio
    ReprogramacionPropuesta ..|> EventoDominio
    SesionCompletada ..|> EventoDominio
    SolicitudRecibida ..|> EventoDominio
    Aceptada ..|> EstadoContratacion
    Cancelada ..|> EstadoContratacion
    Completada ..|> EstadoContratacion
    Confirmada ..|> EstadoContratacion
    EnCurso ..|> EstadoContratacion
    Pendiente ..|> EstadoContratacion
    Rechazada ..|> EstadoContratacion
    ReintegroPorAnticipacion ..|> PoliticaReintegro
    ReintegroTotal ..|> PoliticaReintegro
    Contratacion "1" *-- "*" Calificacion : calificaciones
    Contratacion "1" *-- "1" PropuestaReprogramacion : propuesta
    Contratacion "1" *-- "1" Reintegro : reintegro
    Contratacion "1" *-- "1" Solicitud : solicitud
    Contratacion "1" *-- "*" CambioEstado : historial
    Contratacion "1" o-- "*" EventoDominio : eventos
    Contratacion --> "1" EstadoContratacion : estado
    PoliticaReintegroFactory --> "1" ReintegroPorAnticipacion : porAnticipacion
    PoliticaReintegroFactory --> "1" ReintegroTotal : total
    Contratacion ..> ContratacionAceptada
    Contratacion ..> ContratacionCancelada
    Contratacion ..> PagoConfirmado
    Contratacion ..> ReprogramacionAceptada
    Contratacion ..> ReprogramacionPropuesta
    Contratacion ..> SesionCompletada
    Contratacion ..> SolicitudRecibida
    Contratacion ..> Pendiente
    Contratacion ..> PoliticaReintegro
    ContratacionRepository ..> Contratacion
    ContratacionAceptada ..> Contratacion
    ContratacionCancelada ..> Contratacion
    PagoConfirmado ..> Contratacion
    ReprogramacionAceptada ..> Contratacion
    ReprogramacionPropuesta ..> Contratacion
    SesionCompletada ..> Contratacion
    SolicitudRecibida ..> Contratacion
    Aceptada ..> Cancelada
    Aceptada ..> Confirmada
    Confirmada ..> Cancelada
    Confirmada ..> EnCurso
    EnCurso ..> Completada
    Pendiente ..> Aceptada
    Pendiente ..> Cancelada
    Pendiente ..> Rechazada
    PoliticaReintegro ..> Reintegro
    PoliticaReintegroFactory ..> Contratacion
    PoliticaReintegroFactory ..> PoliticaReintegro
    ReintegroPorAnticipacion ..> Reintegro
    ReintegroTotal ..> Reintegro
```

### 5.3 Perfil

8 clases en `domain.perfil`, 6 relaciones.

```mermaid
classDiagram
    direction LR
    class CanalNotificacion {
        <<enumeration>>
        EMAIL
        PUSH
        INTERNA
    }
    class DisponibilidadSemanal {
        <<record>>
        +DayOfWeek dia
        +LocalTime inicio
        +LocalTime fin
        +seSolapaCon(DisponibilidadSemanal) boolean
    }
    class Perfil {
        -String ACTIVO$
        -String INACTIVO$
        -String userId
        -TipoPerfil tipo
        -String nombreVisible
        -String biografia
        -ZoneId zonaHoraria
        -Set~String~ habilidades
        -Set~String~ intereses
        -List~DisponibilidadSemanal~ disponibilidad
        -CanalNotificacion canal
        -Reputacion reputacion
        -boolean activo
        -List~CambioEstado~ historial
        +crear(String, TipoPerfil, String, ZoneId) Perfil$
        +reconstruir(...) Perfil$
        +editarDatos(String, String, ZoneId)
        +definirHabilidades(Set~String~)
        +definirIntereses(Set~String~)
        +agregarDisponibilidad(DayOfWeek, LocalTime, LocalTime)
        +quitarDisponibilidad(DisponibilidadSemanal)
        +elegirCanal(CanalNotificacion)
        +registrarCalificacion(Puntaje)
        +darDeBaja(String, String, Instant)
        +estaActivo() boolean
    }
    class PerfilRepository {
        <<interface>>
        +findByUserId(String) Optional~Perfil~
        +findByUserIds(Collection~String~) List~Perfil~
        +save(Perfil)
    }
    class Reputacion {
        <<record>>
        +long sumaPuntajes
        +int cantidad
        +inicial() Reputacion$
        +registrar(Puntaje) Reputacion
        +promedio() double
    }
    class TipoPerfil {
        <<enumeration>>
        PROFESIONAL
        ORGANIZACION
    }
    class DisponibilidadInvalidaException
    class PerfilInactivoException
    class DomainException {
        <<abstract, Domain shared>>
    }
    class CambioEstado {
        <<record, Domain shared>>
    }
    DisponibilidadInvalidaException --|> DomainException
    PerfilInactivoException --|> DomainException
    Perfil "1" *-- "*" DisponibilidadSemanal : disponibilidad
    Perfil "1" *-- "1" Reputacion : reputacion
    Perfil "1" *-- "*" CambioEstado : historial
    PerfilRepository ..> Perfil
```

### 5.4 Wallet

7 clases en `domain.wallet`, 5 relaciones.

```mermaid
classDiagram
    direction LR
    class Movimiento {
        <<record>>
        +MovimientoId id
        +TipoMovimiento tipo
        +SkillCoins importe
        +SkillCoins saldoResultante
        +String concepto
        +ContratacionId contratacionId
        +Instant fecha
        +importeConSigno() long
    }
    class MovimientoId {
        <<record>>
        +UUID valor
        +generate() MovimientoId$
    }
    class RetencionDePagos {
        +retener(Wallet, Wallet, SkillCoins, ContratacionId, Instant)
        +liberarAlProveedor(Wallet, Wallet, SkillCoins, ContratacionId, Instant)
        +repartir(Wallet, Wallet, Wallet, SkillCoins, SkillCoins, SkillCoins, ContratacionId, Instant)
    }
    class TipoMovimiento {
        <<enumeration>>
        CREDITO
        DEBITO
        REINTEGRO
        ACREDITACION_DEMO
    }
    class Wallet {
        +String TITULAR_GARANTIA$
        -String CONCEPTO_BONO$
        -String CONCEPTO_DEMO$
        -String titularId
        -SkillCoins saldo
        -List~Movimiento~ movimientosNuevos
        +abrir(String, SkillCoins, Instant) Wallet$
        +abrirGarantia() Wallet$
        +reconstruir(String, SkillCoins) Wallet$
        +debitar(SkillCoins, String, ContratacionId, Instant) Movimiento
        +acreditar(SkillCoins, String, ContratacionId, Instant) Movimiento
        +reintegrar(SkillCoins, String, ContratacionId, Instant) Movimiento
        +acreditarDemo(SkillCoins, Instant) Movimiento
        +esGarantia() boolean
    }
    class WalletRepository {
        <<interface>>
        +findByTitular(String) Optional~Wallet~
        +garantia() Wallet
        +save(Wallet)
        +findMovimientos(String, PaginaSolicitada) Pagina~Movimiento~
    }
    class SaldoInsuficienteException
    class DomainException {
        <<abstract, Domain shared>>
    }
    SaldoInsuficienteException --|> DomainException
    Wallet "1" *-- "*" Movimiento : movimientosNuevos
    RetencionDePagos ..> Wallet
    WalletRepository ..> Movimiento
    WalletRepository ..> Wallet
```

### 5.5 Notificación

4 clases en `domain.notificacion`, 3 relaciones.

```mermaid
classDiagram
    direction LR
    class Notificacion {
        -NotificacionId id
        -String destinatarioId
        -String tipo
        -String titulo
        -String detalle
        -Instant fecha
        -boolean leida
        +para(String, EventoDominio) Notificacion$
        +reconstruir(NotificacionId, String, String, String, String, Instant, boolean) Notificacion$
        +marcarLeida(String)
        +estaLeida() boolean
    }
    class NotificacionId {
        <<record>>
        +UUID valor
        +generate() NotificacionId$
    }
    class NotificacionRepository {
        <<interface>>
        +findById(NotificacionId) Optional~Notificacion~
        +save(Notificacion)
        +findByDestinatario(String, PaginaSolicitada) Pagina~Notificacion~
        +findNoLeidas(String) List~Notificacion~
    }
    class NotificacionAjenaException
    class DomainException {
        <<abstract, Domain shared>>
    }
    NotificacionAjenaException --|> DomainException
    NotificacionRepository ..> Notificacion
    NotificacionAjenaException ..> Notificacion
```

## 6. Capas y flujo de dependencias

Cantidad de relaciones del código (campos, parámetros, retornos, `new`, `throw`, herencia) que cruzan de una capa a otra. `implementa` cuenta clases que realizan una interfaz de la otra capa (los puertos).

```mermaid
classDiagram
    direction TB
    class Presentation {
        <<capa>>
        controllers 20 clases
        controllers_impl 11 clases
        model 9 clases
        ui 3 clases
    }
    class Application {
        <<capa>>
        Interfaces 6 clases
        busqueda 5 clases
        contratacion 8 clases
        evento 1 clase
        notificacion 4 clases
        perfil 7 clases
        publicacion 7 clases
        shared 5 clases
        wallet 2 clases
    }
    class Domain {
        <<capa>>
        contratacion 8 clases
        contratacion_evento 7 clases
        contratacion_exception 4 clases
        contratacion_state 8 clases
        contratacion_strategy 4 clases
        notificacion 4 clases
        perfil 8 clases
        publicacion 7 clases
        publicacion_decorator 6 clases
        publicacion_exception 3 clases
        publicacion_state 6 clases
        shared 8 clases
        wallet 7 clases
    }
    class Infrastructure {
        <<capa>>
        configuracion 3 clases
        identidad 1 clase
        main 1 clase
        memoria 7 clases
        notificacion 6 clases
        persistencia 4 clases
        persistencia_contratacion 5 clases
        persistencia_notificacion 2 clases
        persistencia_perfil 5 clases
        persistencia_publicacion 7 clases
        persistencia_shared 1 clase
        persistencia_wallet 3 clases
    }
    Presentation ..> Application : usa (25)
    Application ..> Domain : usa (49)
    Infrastructure ..> Presentation : usa (3)
    Infrastructure ..> Application : usa (37)
    Infrastructure ..|> Application : implementa (7)
    Infrastructure ..> Domain : usa (50)
    Infrastructure ..|> Domain : implementa (10)
```

El dominio no depende de ninguna otra capa y Application solo depende de Domain: las flechas apuntan hacia adentro (Presentation → Application → Domain; Infrastructure implementa los puertos de Application y Domain).

Infrastructure → Presentation: SkillSwapApplication usa SkillSwapControllers, SkillSwapView, Theme. Es la raíz de composición: arma la aplicación y por eso conoce la UI.

<!-- generado:fin -->
