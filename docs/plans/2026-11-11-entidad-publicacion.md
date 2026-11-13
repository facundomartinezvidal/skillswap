# Diseño de la entidad `Publicacion`

Fecha: 2026-11-11. Alcance: modelo de dominio del agregado `Publicacion` (Java puro), su puerto de repositorio y lo que la persistencia tiene que garantizar. No incluye controllers, búsqueda ni recomendación.

## Qué hay que preservar

- `Models.Publicacion` y `Models.EstadoPublicacion` son **modelos de vista** ([Models.java](../../src/main/java/ar/edu/uade/skillswap/model/Models.java)). El dominio tiene sus propios tipos; el controller traduce.
- Contrato actual de la UI: `PublicacionController.crearBorrador(...)` y `cambiarEstado(...)` ([UiControllers.java](../../src/main/java/ar/edu/uade/skillswap/controllers/UiControllers.java)). La app tiene que seguir abriendo con `mvn exec:java`.
- Reglas de la consigna: [02-alcance-funcional.md](../consigna/02-alcance-funcional.md) (estados, requisitos para publicar) y [03-reglas-de-negocio.md](../consigna/03-reglas-de-negocio.md) (reglas 2, 3 y 8).

## Estructura objetivo

Paquete: `ar.edu.uade.skillswap.domain.publicacion`, con los estados en `.state` y las excepciones en `.exception`; `SkillCoins` y `DomainException` en `ar.edu.uade.skillswap.domain.shared`. Sin JPA, sin Swing.

```mermaid
classDiagram
  class Publicacion {
    PublicacionId id
    String autorId
    TipoPublicacion tipo
    String titulo
    String descripcion
    String categoria
    Modalidad modalidad
    Duration duracionEstimada
    int cupo
    SkillCoins precioBase
    Set~String~ etiquetas
    EstadoPublicacion estado
    String motivoCancelacion
    +crearBorrador(...)$
    +editar(...)
    +agregarFranja(inicio, fin)
    +publicar(actorId, ahora)
    +pausar(actorId, motivo, ahora)
    +reanudar(actorId, ahora)
    +cerrar(actorId, motivo, ahora)
    +cancelar(actorId, motivo, ahora)
    +admiteSolicitudes() bool
    +reservarCupo(franjaId)
    +liberarCupo(franjaId)
    +historial() List~CambioEstado~
  }
  class FranjaDisponibilidad {
    FranjaId id
    Instant inicio
    Instant fin
    int reservados
  }
  class CambioEstado {
    <<record>>
    String desde
    String hacia
    Instant fecha
    String actorId
    String motivo
  }
  class SkillCoins { <<record>> long valor }
  class TipoPublicacion { <<enum>> OFERTA DESAFIO }
  class Modalidad { <<enum>> REMOTA PRESENCIAL HIBRIDA }
  class EstadoPublicacion { <<sealed interface>> }
  EstadoPublicacion <|.. Borrador
  EstadoPublicacion <|.. Publicada
  EstadoPublicacion <|.. Pausada
  EstadoPublicacion <|.. Cerrada
  EstadoPublicacion <|.. Cancelada
  Publicacion "1" *-- "0..*" FranjaDisponibilidad
  Publicacion "1" *-- "0..*" CambioEstado : historial append-only
  Publicacion --> EstadoPublicacion
  Publicacion --> SkillCoins
```

- `TipoPublicacion { OFERTA, DESAFIO }` es un enum sin subclases. El caso de uso valida que solo un profesional cree ofertas y solo una organización cree desafíos.
- `Modalidad` incluye `HIBRIDA`, que es una extensión de la consigna (la consigna solo nombra remota y presencial).

### Máquina de estados (patrón State)

```mermaid
stateDiagram-v2
  [*] --> BORRADOR
  BORRADOR --> PUBLICADA : publicar (valida requisitos)
  BORRADOR --> CANCELADA : motivo obligatorio
  PUBLICADA --> PAUSADA
  PAUSADA --> PUBLICADA : reanudar
  PUBLICADA --> CERRADA
  PAUSADA --> CERRADA
  PUBLICADA --> CANCELADA
  PAUSADA --> CANCELADA
  CERRADA --> [*]
  CANCELADA --> [*]
```

`EstadoPublicacion` declara `publicar`, `pausar`, `reanudar`, `cerrar`, `cancelar`, `editar`, `admiteSolicitudes` y `nombre`. Por defecto cada acción lanza `TransicionInvalidaException`; cada estado concreto sobrescribe solo lo que permite. `Publicacion` delega en su estado y registra el `CambioEstado`. Que `PAUSADA` pueda volver a `PUBLICADA` es una interpretación del equipo y va al informe.

### Invariantes

- `crearBorrador` exige `autorId` y `tipo`; el resto puede estar incompleto.
- `editar` y `agregarFranja` solo en `BORRADOR`.
- `publicar` exige título no vacío, categoría, precio ≥ 0, cupo ≥ 1 y al menos una franja. Si falla, la excepción lista todos los faltantes.
- `cancelar` exige motivo no vacío.
- `CERRADA` y `CANCELADA` son finales.
- Toda transición válida agrega un `CambioEstado` (fecha, actor, motivo). El historial se expone con `List.copyOf` y nunca se modifica ni se borra. Una transición rechazada no deja registro.
- `admiteSolicitudes()` es verdadero solo en `PUBLICADA` (regla 2).
- Franjas: `inicio < fin` y sin solapamiento dentro de la misma publicación.
- `reservarCupo(franjaId)` falla si la publicación no está `PUBLICADA` o si la franja tiene `reservados == cupo` (regla 3). El cupo es por franja: cada franja admite hasta `cupo` reservas. `liberarCupo` decrementa sin bajar de 0.
- `SkillCoins` rechaza valores negativos.
- Las operaciones reciben `ahora` y `actorId` como parámetros (tests deterministas).

Fuera de la entidad: reputación, nombre del proveedor y `destacada` (se arman en la consulta para la vista); precio final con promociones (Decorator, otro diseño); permiso del autor (`userId == autorId`, lo valida el caso de uso).

### Persistencia

- Puerto `PublicacionRepository` en el dominio; implementación JPA en infraestructura, con entidades JPA separadas y un mapper.
- Tablas: `publicacion`, `franja_disponibilidad`, `publicacion_etiqueta`, `publicacion_cambio_estado` (solo inserts). Migración Flyway.
- El estado se guarda como `nombre()`; el mapper reconstruye el estado concreto.
- Concurrencia de cupo: la aceptación modifica `reservados` de una franja (entidad hija). Hay que garantizar que dos aceptaciones simultáneas no tomen el último lugar: `@Version` en la franja, o `@Version` en `publicacion` cargada con `OPTIMISTIC_FORCE_INCREMENT`. Elige el desarrollador; el test de concurrencia es obligatorio.

### Cerrar y cancelar con contrataciones activas

- **Cerrar** siempre se permite. Las contrataciones `ACEPTADA`, `CONFIRMADA` y `EN_CURSO` siguen su curso; las `PENDIENTE` se rechazan automáticamente.
- **Cancelar** se bloquea si hay contrataciones `ACEPTADA`, `CONFIRMADA` o `EN_CURSO`; las `PENDIENTE` se rechazan automáticamente. Nunca mueve SkillCoins.

La entidad `Publicacion` no conoce las contrataciones. La coordinación va en los casos de uso `CerrarPublicacion` y `CancelarPublicacion`, dentro de una única transacción:

1. Validar que el actor sea el autor.
2. `ContratacionRepository.buscarActivasPorPublicacion(publicacionId)`.
3. Solo en cancelar: si alguna está `ACEPTADA`, `CONFIRMADA` o `EN_CURSO`, lanzar `PublicacionConContratacionesActivasException` (con la cantidad) y no cambiar nada.
4. Rechazar cada `PENDIENTE` con motivo "publicación cerrada" o "publicación cancelada" (queda en el historial de la contratación y dispara la notificación).
5. `publicacion.cerrar(...)` o `publicacion.cancelar(...)` y guardar.

Motivo: el dinero solo se mueve en la cancelación de cada contratación (regla 5), así que la lógica de reembolso no se duplica. Costo aceptado: con clientes comprometidos, el proveedor cancela primero cada contratación y después la publicación.

## Pasos

1. Value objects y enums: `PublicacionId`, `FranjaId`, `SkillCoins`, `TipoPublicacion`, `Modalidad`. Tests unitarios.
2. `EstadoPublicacion` (interfaz sellada) y los cinco estados. Tests por estado × acción.
3. `FranjaDisponibilidad` y `Publicacion` con invariantes e historial. Tests unitarios.
4. Puerto `PublicacionRepository` y caso de uso de crear borrador / cambiar estado; conectar `PublicacionController` real manteniendo el mock compilando.
5. Infraestructura JPA, migración Flyway y test de concurrencia de cupo.

Cada paso deja `mvn test` en verde y la app abriendo.

## Verificación (aceptación)

- Publicar una oferta completa pasa a `PUBLICADA` y deja un `CambioEstado` con actor y fecha.
- Publicar sin franjas, sin título, sin categoría o con precio negativo falla y lista los faltantes (caso de aceptación 2).
- Cada transición no listada en el diagrama lanza `TransicionInvalidaException` y no modifica el historial; en particular, `CERRADA → PUBLICADA` y `CANCELADA → *`.
- `PAUSADA → PUBLICADA` funciona.
- Cancelar sin motivo falla.
- `admiteSolicitudes()` es falso en todo estado salvo `PUBLICADA`.
- `reservarCupo` sobre una franja con `cupo = 1` y `reservados = 1` falla; otra franja de la misma publicación sigue aceptando.
- Dos aceptaciones concurrentes sobre el último lugar: una sola se confirma (caso de aceptación 4).
- El historial devuelto no se puede modificar.
- Cerrar con una `CONFIRMADA` y una `PENDIENTE`: la publicación pasa a `CERRADA`, la `CONFIRMADA` sigue igual y la `PENDIENTE` queda `RECHAZADA`.
- Cancelar con una `CONFIRMADA`: falla, la publicación sigue en su estado y no se rechaza ninguna `PENDIENTE` (todo o nada).
- Cancelar con solo `PENDIENTE`: la publicación pasa a `CANCELADA`, las solicitudes quedan `RECHAZADA` y ningún saldo cambia.
- Una `Publicacion` con `HIBRIDA` se guarda y se recupera igual.
- Ninguna clase de `domain` importa `jakarta.persistence` ni `javax.swing`.

## Preguntas abiertas

- Los casos de uso de cerrar y cancelar dependen de que existan `Contratacion` y `ContratacionRepository`. Hasta entonces, se implementan solo las transiciones de la entidad.
- Si `categoria` pasa a ser un catálogo jerárquico (Composite), `String categoria` se reemplaza por `CategoriaId`. Hoy queda como `String`.
- Detalles de implementación libres: nombres de excepciones, tipo de los ids (UUID o String), y dónde va `@Version` (ver Persistencia).
