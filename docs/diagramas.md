# Diagramas de clases: capa de aplicación e infraestructura

Base para el diagrama de StarUML que pide la entrega ("dominio y aplicación, con relaciones,
multiplicidades y patrones identificados"). Los diagramas de cada agregado del dominio están en
las descripciones de los PRs #4 a #11 y #13. Acá están la capa de aplicación (por partes, para que
se pueda leer) y cómo la implementa la infraestructura.

Convenciones: `..>` depende de (usa), `..|>` implementa, `*--` compone, `o--` agrega.
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
