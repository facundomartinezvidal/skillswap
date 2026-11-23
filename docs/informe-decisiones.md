# Informe de decisiones de diseño — SkillSwap

TPO de Procesos de Desarrollo de Software (UADE). Backend en Java 17 con Clean Architecture, sin Spring ni REST, con Hibernate/JPA, MySQL y Flyway. La UI Swing de la cátedra se conecta solo a través de las interfaces de controllers.

Cada decisión sigue el formato **problema → alternativa elegida → consecuencias → evidencia** (código o tests).

## 1. Arquitectura

```
Presentación (ui, model)  →  Controllers  →  Aplicación (Services, DTOs, puertos)  →  Dominio
                                                     ↑                                  ↑
                              Infraestructura (JPA, Flyway, adapters, en memoria) ──────┘
```

**D1. Clean Architecture con el dominio en el centro.**
- **Problema:** la consigna exige que las reglas no dependan de la interfaz, de JPA ni de proveedores externos.
- **Alternativa elegida:** cuatro capas por paquete (`domain`, `application`, `infrastructure`, `controllers` más la UI de la cátedra). Todas las dependencias apuntan hacia el dominio, que es Java puro y no tiene anotaciones JPA. Los módulos de dominio se referencian solo por id. Las entidades JPA viven en infraestructura y se traducen al dominio con mappers.
- **Consecuencias:** el dominio y los casos de uso se prueban sin base de datos ni Swing. A cambio, hay que mantener mappers entidad ↔ dominio.
- **Evidencia:** `ArquitecturaTest` revisa cada fuente en cada build. El dominio no importa `application`, `infrastructure`, UI, JPA ni Swing; `application` solo depende del dominio y no usa `reconstruir(...)`; `notificacion` solo depende de `shared`.

**D2. Inyección de dependencias manual.**
- **Problema:** la consigna prohíbe Spring.
- **Alternativa elegida:** composition roots escritos a mano, que arman todo por constructor: `BackendEnMemoria` y `BackendJpa`. `SkillSwapApplication` elige cuál usar.
- **Consecuencias:** cada dependencia se ve en el constructor y no hay magia. Los Services no saben si persisten en memoria o en MySQL.
- **Evidencia:** `RecorridoCompletoTest` usa `BackendEnMemoria`; los tests de persistencia usan `BackendJpa`.

## 2. Dominio

**D3. El modelo parte del dominio.**
- **Problema:** el esquema inicial de la cátedra no coincidía con las reglas, por ejemplo en la identidad de las personas.
- **Alternativa elegida:** primero el dominio con sus invariantes. Después el esquema se ajustó a él, con la migración Flyway V1 reescrita.
- **Consecuencias:** las reglas se testean primero en Java puro.
- **Evidencia:** un test por agregado (`PublicacionTest`, `ContratacionTest`, `WalletTest`, `PerfilTest`, `NotificacionTest`).

**D4. Identidad por `userId`.**
- **Problema:** la consigna pide que el perfil referencie el `userId` de un servicio de identidad externo.
- **Alternativa elegida:** todo el dominio usa el `userId` (`autorId`, `clienteId`, `proveedorId`, `titularId`); no hay `PerfilId`.
- **Consecuencias:** los controllers reciben el `userId` del login simulado y lo pasan tal cual.
- **Evidencia:** `Perfil.crear(userId, …)`, `ServicioIdentidadSimulado`.

**D5. Estados con historial inmutable (regla 8).**
- **Problema:** cada transición tiene que registrar fecha, actor y motivo, y las transiciones inválidas se rechazan.
- **Alternativa elegida:** patrón **State**. Cada estado sabe qué acciones admite y el resto lanza `TransicionInvalidaException`. El agregado guarda un `CambioEstado` por cada transición válida, en una lista que solo crece.
- **Consecuencias:** sumar un estado es sumar una clase. Una operación rechazada no modifica nada.
- **Evidencia:** `EstadoPublicacionTest`, `EstadoContratacionTest`, historial en `ContratacionTest`.

**D6. Cupo por franja, reservado al aceptar.**
- **Problema:** no puede sobrevenderse una franja (regla 3, caso 4), y la consigna dice que la aceptación bloquea el cupo.
- **Alternativa elegida:**
  - El cupo es por franja.
  - Al solicitar solo se valida (`franjaParaSolicitar`: publicación publicada, franja propia, no empezada, con lugar).
  - Al aceptar se reserva (`reservarCupo`), y al cancelar se libera si `ocupaCupo()`.
  - Con la publicación PAUSADA se puede aceptar y reprogramar lo que ya existía; con CERRADA o CANCELADA no (`admiteReservas()`).
- **Consecuencias:** en persistencia, el cupo se protege con bloqueo optimista sobre la publicación (ver D14).
- **Evidencia:** `FranjaDisponibleTest`, `PublicacionTest.CupoPorFranja`, `caso4_dosAceptacionesNoConsumenElUltimoCupo`, `conLaPublicacionCerradaNoSeAceptaNiSeReprograma`.

**D7. Reintegro configurable (regla 5).**
- **Problema:** al cancelar se devuelve el 100 %, el 50 % o el 0 % según la anticipación, y si cancela el proveedor se devuelve todo.
- **Alternativa elegida:** patrón **Strategy** (`ReintegroPorAnticipacion`, con 48 h y 24 h configurables, y `ReintegroTotal`), más una **Simple Factory** (`PoliticaReintegroFactory`) que elige la política según quién cancela.
- **Consecuencias:** cambiar los umbrales o agregar una política no toca `Contratacion`.
- **Evidencia:** `PoliticaReintegroTest`, `PoliticaReintegroFactoryTest`, caso 6 en `ContratacionServiceTest`.

**D8. Promociones acumulables sin precio negativo (regla 6).**
- **Problema:** varias promociones se acumulan y el orden importa (100 con 10 % y −20 da 70; con −20 y 10 %, da 72).
- **Alternativa elegida:** patrón **Decorator**. `PrecioBase` se envuelve con un `PrecioConPromocion` por cada promoción, en el orden en que se agregaron, y cada promoción garantiza que el precio no baje de 0.
- **Consecuencias:** un tipo de promoción nuevo es un record nuevo.
- **Evidencia:** `PromocionTest`, `PrecioTest`, `caso9_lasPromocionesSeAcumulanSinBajarDeCero`.

**D9. Pago retenido en una Wallet de garantía (regla 4).**
- **Problema:** si se cancela después de pagar, no hay que debitarle al proveedor algo que ya pudo haber gastado, y nunca puede quedar un saldo negativo.
- **Alternativa elegida:** el servicio de dominio `RetencionDePagos` mueve los fondos:
  - `retener`: del cliente a la garantía, al pagar;
  - `liberarAlProveedor`: de la garantía al proveedor, al completar;
  - `repartir`: de la garantía al cliente y al proveedor según el reintegro, al cancelar.

  Siempre debita primero.
- **Consecuencias:** la suma de saldos se conserva. El doble cobro lo impide el estado (solo se paga desde ACEPTADA) y, en la base, una restricción única.
- **Evidencia:** `RetencionDePagosTest`, `caso5_unPagoConSaldoInsuficienteNoModificaSaldosNiEstado`, `unSegundoPagoSeRechazaSinCobrarDosVeces`.

**D10. Calificación como acción de la contratación (regla 7).**
- **Problema:** solo los participantes de una contratación completada califican, una vez cada uno, y nadie se califica a sí mismo.
- **Alternativa elegida:** `Contratacion.calificar` deduce el destinatario (la otra parte) y rechaza duplicados. La reputación (suma y cantidad) solo cambia con `Perfil.registrarCalificacion`.
- **Consecuencias:** la reputación no se puede editar a mano.
- **Evidencia:** `caso10_cadaParteCalificaUnaVezYSeRecalculaLaReputacion`, `PerfilTest.soloRegistrarCalificacionPuedeCambiarLaReputacion`.

**D11. Baja lógica (regla 9).**
- **Problema:** dar de baja un perfil no puede borrar operaciones históricas.
- **Alternativa elegida:** `darDeBaja` exige un motivo y queda en el historial. Un perfil dado de baja no se edita, no publica ni solicita, pero sigue recibiendo calificaciones.
- **Evidencia:** `PerfilTest`, `laBajaEsLogicaYDespuesNoSePuedeEditar`, `unClienteDadoDeBajaOSinWalletNoPuedeSolicitar`.

## 3. Capa de aplicación

**D12. Un Service por flujo, con DTOs.**
- **Problema:** coordinar agregados sin poner reglas en la capa de aplicación.
- **Alternativa elegida:** seis Services (`Perfil`, `Publicacion`, `Busqueda`, `Contratacion`, `Wallet`, `Notificacion`), uno por cada interfaz de `UiControllers`, como en la Clase 9 y Magic Café. Reciben el `userId` que actúa y devuelven records con colecciones inmodificables.
- **Consecuencias:** los controllers quedan delgados. Las reglas siguen en el dominio, y el Service solo coordina y valida los permisos que cruzan módulos.
- **Evidencia:** un test por Service; `RecorridoCompletoTest`.

**D13. Puertos.**
- **Problema:** la infraestructura implementa interfaces que tienen que estar del lado de adentro.
- **Alternativa elegida:** los repositorios van en la raíz de cada módulo de dominio, con listados paginados (`Pagina<T>`). En `application` van los demás puertos: `UnidadDeTrabajo`, `BuscadorPublicaciones` (la búsqueda cruza publicación y perfil), `ServicioIdentidad` y `EnvioNotificacion`.
- **Evidencia:** `ArquitecturaTest`.

**D14. Operaciones todo o nada.**
- **Problema:** aceptar, pagar, completar, cancelar, reprogramar, calificar y dar de alta un perfil tocan varios agregados.
- **Alternativa elegida:** se ejecutan dentro de `UnidadDeTrabajo.ejecutar(...)`. El Service modifica primero todos los agregados y guarda al final. En memoria, `UnidadDeTrabajoEnMemoria` anota los `save` y los aplica al confirmar (rollback real); con JPA es una `EntityTransaction` (ver persistencia).
- **Evidencia:** `siAlgoFallaDespuesDeMoverFondosNoQuedaNadaGuardado`, `enUnaTransaccionQueFallaNoSeGuardaNada`.

**D15. Notificaciones.**
- **Problema:** un evento interesa a varias personas, cada una elige su canal, y los proveedores tienen que ser reemplazables.
- **Alternativa elegida:**
  - Los eventos los emite el dominio, que también declara a los interesados.
  - El Service los publica después de confirmar, en el `PublicadorEventos` (**Observer**).
  - El suscriptor `NotificadorDeInteresados` deja la notificación en la bandeja y la envía por el canal del perfil.
  - El canal lo elige `CanalEnvioFactory` (**Simple Factory**).
  - Email y push salen por `EmailAdapter` y `PushAdapter`, que traducen el puerto a la API de cada proveedor simulado (**Adapter**).
- **Consecuencias:** una reacción nueva es otro suscriptor, y un proveedor nuevo es otro adapter. Un envío que falla no deshace la operación.
- **Evidencia:** caso 8 en `NotificadorDeInteresadosTest`; `unSuscriptorQueFallaNoCortaALosDemas`.

**D16. Búsqueda y recomendaciones.**
- **Problema:** hay que filtrar por texto, categoría, modalidad, rango de precio, habilidad y reputación mínima, ordenar de cuatro formas y paginar. Además, la consigna pide dos estrategias de recomendación intercambiables.
- **Alternativa elegida:**
  - La búsqueda es un puerto (`BuscadorPublicaciones`).
  - El precio que se filtra es el precio final, con promociones.
  - La estrategia de recomendación (**Strategy**: `AfinidadPorHabilidades`, `PopularidadReciente`) entra por constructor en `BusquedaService`.
  - Los autores de una página se buscan de una vez (`findByUserIds`), sin N+1.
- **Consecuencias:** cambiar la estrategia es cambiar una línea del composition root. Limitación conocida: las candidatas a recomendar son todas las publicadas.
- **Evidencia:** `BusquedaServiceTest` (caso 1), `RecomendacionTest` (caso 7).

**D17. Permisos con el `userId` (regla 10).**
- **Problema:** cada operación que modifica datos tiene que validar que quien actúa pueda hacerla.
- **Alternativa elegida:**
  - Los roles de la contratación los valida el dominio (`AccionNoPermitidaException`).
  - La autoría y la visibilidad de una publicación las valida el Service, con `Publicacion.esVisiblePara`.
  - Perfil y Wallet operan solo sobre el `userId` que llega.
- **Evidencia:** `soloElAutorModificaSuPublicacion`, `unBorradorOUnaPausadaSoloLaVeSuAutor`, `soloElClientePaga`.

## 4. Persistencia

**D18. El esquema lo define solo Flyway.**
- **Problema:** la consigna pide migraciones versionadas y prohíbe que Hibernate cree el esquema.
- **Alternativa elegida:** `V1__esquema_inicial.sql` alineada con el dominio. Se reescribió en lugar de agregar una V2 porque nunca se aplicó en ninguna base. `Migraciones.aplicar` corre al construir `BackendJpa`, y Hibernate usa `hbm2ddl.auto=validate`.
- **Consecuencias:** si una entidad JPA no coincide con su tabla, la aplicación no arranca. Un cambio de esquema futuro es una `V2`, nunca una edición de `V1`.
- **Evidencia:** `RepositoriosJpaTest` y `BackendJpaTest` levantan un MySQL limpio, migran y validan.

**D19. Entidades JPA separadas del dominio.**
- **Problema:** el dominio tiene que seguir siendo Java puro (D1).
- **Alternativa elegida:** una entidad, un mapper y un `*RepositoryJpa` por agregado en `infrastructure/persistencia`. El mapper usa `reconstruir(...)` para ir al dominio y los getters para volver. Los estados se guardan por nombre.
- **Consecuencias:** hay código de traducción, pero el dominio no cambia por la base y los mismos Services funcionan en memoria y con MySQL.
- **Evidencia:** `RepositoriosJpaTest` (13 tests de ida y vuelta de cada agregado con todos sus datos); `ArquitecturaTest`.

**D20. Restricciones de integridad en la base.**
- **Problema:** el doble cobro y la calificación duplicada no pueden depender solo del código.
- **Alternativa elegida:** `UNIQUE(contratacion_id, autor_id)` en calificaciones y `UNIQUE(contratacion_id, titular, tipo)` en movimientos, más claves foráneas e índices para la búsqueda y los listados. La Wallet de garantía la crea la migración, porque es parte de la plataforma.
- **Consecuencias:** aunque falle una validación del dominio, la base rechaza el dato inconsistente.
- **Evidencia:** `RestriccionesJpaTest` (la base rechaza el doble pago y la calificación duplicada).

**D21. Bloqueo optimista para el cupo y los saldos (caso 4).**
- **Problema:** dos aceptaciones concurrentes no pueden consumir el último lugar de una franja.
- **Alternativa elegida:** `@Version` en publicación, contratación, wallet y perfil. Guardar una publicación siempre sube su versión (`OPTIMISTIC_FORCE_INCREMENT`), porque el cupo vive en la tabla de franjas y el agregado es la unidad de consistencia. El conflicto se traduce a `ConflictoDeConcurrenciaException` ("Otra operación modificó los mismos datos; volvé a intentar").
- **Consecuencias:** no hay bloqueos largos; el que pierde reintenta. La traducción es técnica: la infraestructura no decide reglas de negocio.
- **Evidencia:** `BackendJpaTest` (dos hilos aceptan el último lugar y solo uno gana; el test es determinista); `ArquitecturaTest.laInfraestructuraNoLanzaExcepcionesDeNegocio`.

**D22. Unidad de trabajo con `EntityTransaction`.**
- **Problema:** pagar mueve fondos entre dos wallets y cambia la contratación; o se guarda todo, o nada (caso 5).
- **Alternativa elegida:** `UnidadDeTrabajoJpa` con un `ThreadLocal<EntityManager>` y begin/commit/rollback explícitos. Los repositorios le piden el `EntityManager` en curso; fuera de una transacción, cada llamada abre una corta propia.
- **Consecuencias:** los Services marcan el límite transaccional con `unidadDeTrabajo.ejecutar(...)` sin conocer JPA.
- **Evidencia:** `BackendJpaTest` (caso 5 contra MySQL: sin saldo no queda nada guardado).

**D23. Listados y búsqueda sin N+1.**
- **Problema:** cargar cada publicación con sus franjas y su autor por separado multiplica las consultas.
- **Alternativa elegida:** primero se traen los ids de la página y un `count`; después el lote con un `join fetch` por colección (para evitar `MultipleBagFetchException` y el producto cartesiano). La búsqueda filtra y ordena en JPQL con `precio_final` y `publicada_en`, desnormalizados al grabar.
- **Consecuencias:** cantidad de consultas constante por página. A cambio, hay dos columnas que se recalculan en cada `save`.
- **Evidencia:** `BuscadorPublicacionesJpaTest` (filtros, orden y paginación).

**D24. Configuración por variables de entorno.**
- **Problema:** no se versionan credenciales.
- **Alternativa elegida:** `DB_URL` (por defecto `jdbc:mysql://localhost:3307/skillswap`), `DB_USER` y `DB_PASSWORD` (por defecto `skillswap`, solo de desarrollo, iguales al `docker-compose.yml`). Se usa el puerto 3307 porque el 3306 suele estar ocupado por un MySQL local. Fechas en `DATETIME(6)` UTC e ids `VARCHAR(36)`.
- **Consecuencias:** `docker compose up -d` y `mvn exec:java` alcanzan para la demo; en otra máquina se cambia solo el entorno.
- **Evidencia:** `ConfiguracionBaseDeDatos`, `docker-compose.yml`.

## 5. Controllers e integración con la UI

**D25. Controllers delgados sobre el contrato de la cátedra.**
- **Problema:** la UI de la cátedra no se rediseña y conoce solo `UiControllers`.
- **Alternativa elegida:** un `*ControllerImpl` por sub-interfaz de `UiControllers` (que no se modificó), agrupados en `SkillSwapControllers`, que se construye con los seis Services. `ModelosMapper` pasa los DTOs a `Models`. Lo que la consigna pide y la UI no expone va en interfaces del equipo: `ContratacionOperaciones`, `PublicacionOperaciones`, `PerfilOperaciones`, `BusquedaOperaciones`, `WalletOperaciones`, `NotificacionOperaciones`, `RecomendacionController` y `CalificacionController`.
- **Consecuencias:** los controllers no tienen reglas ni SQL; solo validan permisos con el `userId` recibido y delegan.
- **Evidencia:** `ArquitecturaTest` (los controllers no usan repositorios, infraestructura, JPA ni Swing, y del dominio solo usan tipos simples).

**D26. Manejo uniforme de errores.**
- **Problema:** la vista no atrapa excepciones.
- **Alternativa elegida:** `ManejoDeErrores` es el único punto que traduce `DomainException`, `ApplicationException` e `IllegalArgumentException` a `OperacionRechazadaException` con el mensaje del dominio. `SkillSwapApplication` lo muestra en un `JOptionPane`.
- **Consecuencias:** el usuario ve "Saldo insuficiente" en lugar de un stack trace; un error inesperado se informa aparte.
- **Evidencia:** `CasosNegativosTest` (8 casos: transición inválida, saldo insuficiente, cupo agotado, doble pago, calificación no autorizada, entre otros; en todos se verifica el mensaje).

**D27. Adaptaciones a una UI que no se rediseña.**
- **Problema:** la UI tiene un único botón "Avanzar estado", solicita sin franja y crea borradores sin cupo.
- **Alternativa elegida:** `avanzar` decide según estado y rol (PENDIENTE y proveedor acepta; ACEPTADA y cliente paga; CONFIRMADA y proveedor inicia; EN_CURSO y proveedor completa; otro caso explica quién tiene que actuar). `solicitar` usa la primera franja futura con lugar. `crearBorrador` crea con cupo 1 y una franja a 7 días. Los listados de la UI piden la primera página de 50.
- **Consecuencias:** la demo se puede hacer entera desde la UI, de publicar a calificar. Sin rediseñar las pantallas, la vista suma tres botones que reciben las interfaces del equipo por constructor: "Calificar" en las contrataciones COMPLETADA, y "Ver historial" en RECHAZADA y CANCELADA (estados finales: solo se consulta el historial). En RECHAZADA, el cliente tiene además "Volver a solicitar", que crea una contratación nueva sobre la misma publicación. Quién puede y desde qué estado lo decide el dominio (`Contratacion.validarNuevaSolicitud` y `EstadoContratacion.admiteNuevaSolicitud()`), no el controller, que solo elige la franja. Reprogramar y cargar franjas quedan en las interfaces del equipo y se prueban por tests.
- **Evidencia:** `CasosDeAceptacionTest` (los diez casos de punta a punta a través de los controllers) y `ContratacionTerminadaTest` (historial, volver a solicitar y sus rechazos); la regla de volver a solicitar, en `ContratacionTest`, `EstadoContratacionTest` y `ContratacionServiceTest`.

**D28. Dos backends y datos de demo reproducibles.**
- **Problema:** la demo no puede depender de que MySQL ande en la máquina del día.
- **Alternativa elegida:** sin argumentos arranca `BackendJpa` (MySQL y Flyway); con `--memoria`, `BackendEnMemoria`. En los dos casos `DatosDemo` carga perfiles, publicaciones, calificaciones y una contratación de Mateo en cada estado. Usa solo los Services y es idempotente.
- **Consecuencias:** hay un plan B sin base de datos con exactamente los mismos datos.
- **Evidencia:** `DatosDemoTest` (es idempotente y la demo funciona sobre esos datos).

## 6. Patrones de diseño

| Patrón | Variación que resuelve | Dónde | Test que lo cubre |
|---|---|---|---|
| **State** | Qué acciones admite cada estado de la publicación y de la contratación, y qué pasa al cambiar de estado | `domain/publicacion/state`, `domain/contratacion/state` | `EstadoPublicacionTest`, `EstadoContratacionTest` |
| **Strategy** | Cómo se calcula el reintegro (según anticipación o total) y cómo se recomiendan publicaciones (afinidad o popularidad) | `domain/contratacion/strategy`, `application/busqueda/recomendacion` | `PoliticaReintegroTest`, `RecomendacionTest` (caso 7) |
| **Simple Factory** | Elegir la política de reintegro según quién cancela; elegir el canal de envío según el perfil | `PoliticaReintegroFactory`, `CanalEnvioFactory` | `PoliticaReintegroFactoryTest`, `CanalEnvioFactoryTest` |
| **Decorator** | Promociones acumulables sobre el precio, que nunca bajan de 0 | `domain/publicacion/decorator` | `PromocionTest`, `PrecioTest` (caso 9) |
| **Observer** | Un evento le interesa a varias personas y puede tener varias reacciones | `PublicadorEventos`, `SuscriptorEvento`, `NotificadorDeInteresados` | `PublicadorEventosTest`, `NotificadorDeInteresadosTest` (caso 8) |
| **Adapter** | Proveedores externos de email, push e identidad, y la persistencia JPA, reemplazables sin tocar la aplicación | `infrastructure/notificacion`, `infrastructure/identidad`, `infrastructure/persistencia` | `AdaptersDeNotificacionTest`, tests de persistencia |

No se agregó ningún patrón sin una variación concreta que lo justifique. Wallet, por ejemplo, no tiene estados ni variaciones, así que no usa ninguno.

## 7. SOLID y GRASP

| Principio | Cómo se aplica | Ejemplo |
|---|---|---|
| **S** (responsabilidad única) | Cada clase tiene un motivo para cambiar: el agregado guarda sus reglas, el Service coordina, el repositorio persiste, el mapper traduce, el controller adapta a la UI | `Contratacion` / `ContratacionService` / `ContratacionRepositoryJpa` / controller de contratación |
| **O** (abierto/cerrado) | Un estado, una política, una promoción, un suscriptor o un proveedor nuevo es una clase nueva, sin modificar las existentes | `EstadoContratacion`, `PoliticaReintegro`, `Promocion`, `SuscriptorEvento`, `EnvioNotificacion` |
| **L** (sustitución de Liskov) | Cualquier implementación de un puerto sirve igual: los mismos Services funcionan con los repositorios en memoria y con los JPA | `BackendEnMemoria` y `BackendJpa` arman los mismos Services |
| **I** (segregación de interfaces) | Puertos chicos y específicos en lugar de un repositorio genérico | `EnvioNotificacion` (un método), `ServicioIdentidad` (un método), un repositorio por agregado |
| **D** (inversión de dependencias) | La aplicación depende de interfaces; la infraestructura las implementa y se inyecta por constructor | `UnidadDeTrabajo`, `PublicacionRepository`, etc.; `ArquitecturaTest` |
| **Experto en información** (GRASP) | Quien tiene los datos decide: la publicación sabe si una franja tiene lugar; la contratación, quién puede pagar; la wallet, si alcanza el saldo | `Publicacion.franjaParaSolicitar`, `Contratacion.pagar`, `Wallet.debitar` |
| **Creador** (GRASP) | El agregado crea sus partes | `Publicacion.agregarFranja`, `Contratacion.calificar` crea la `Calificacion`, `Notificacion.para(evento)` |
| **Controlador** (GRASP) | Los controllers reciben los eventos de la UI y los delegan en un Service, sin reglas | controllers de `controllers.impl` |
| **Bajo acoplamiento / alta cohesión** (GRASP) | Los módulos del dominio se conocen solo por id; la coordinación está en la aplicación | `ContratacionService` coordina `Contratacion`, `Publicacion` y `Wallet` |
| **Polimorfismo** (GRASP) | Los comportamientos que varían se resuelven con polimorfismo, no con `if` por tipo | estados, políticas, promociones, estrategias, adapters |
| **Indirección / fabricación pura** (GRASP) | Clases que no son del negocio, pero desacoplan | `PublicadorEventos`, `UnidadDeTrabajo`, mappers, `RetencionDePagos` (servicio de dominio) |
| **Variaciones protegidas** (GRASP) | Los puntos que pueden cambiar quedan detrás de una interfaz | proveedores externos, persistencia, estrategia de recomendación |

## 8. Casos de aceptación

| Caso | Test |
|---|---|
| 1 Publicar y encontrar con búsqueda filtrada | `BusquedaServiceTest.caso1_…`, `RecorridoCompletoTest` |
| 2 Rechazar la publicación de una oferta incompleta | `PublicacionServiceTest.caso2_…` |
| 3 Solicitar, aceptar y pagar con saldo suficiente | `ContratacionServiceTest.caso3_…` |
| 4 Dos aceptaciones no consumen el último cupo | `ContratacionServiceTest.caso4_…` y el test de concurrencia contra MySQL |
| 5 Pago con saldo insuficiente sin modificar nada | `ContratacionServiceTest.caso5_…` y contra MySQL |
| 6 Cancelar y calcular el reintegro con dos políticas | `ContratacionServiceTest.caso6_…` |
| 7 Cambiar la estrategia de recomendación sin tocar el caso de uso | `RecomendacionTest.caso7_…` |
| 8 Dos tipos de notificación ante un mismo evento | `NotificadorDeInteresadosTest.caso8_…` |
| 9 Promociones acumulables sin precio negativo | `PublicacionServiceTest.caso9_…` |
| 10 Completar, calificar una vez y recalcular la reputación | `ContratacionServiceTest.caso10_…` |

Los diez casos también se recorren de punta a punta a través de los controllers (ver la sección de controllers).

## 9. Pruebas y cobertura

- `mvn test` con Java 17: **579 tests, 0 fallos**. Los 26 de `infrastructure/persistencia` usan Testcontainers (MySQL en Docker) y se omiten si Docker no está disponible; en CI se ejecutan.
- Cobertura medida con JaCoCo (`target/site/jacoco/index.html`), sin excluir clases:

| Capa | Líneas | Ramas |
|---|---|---|
| Dominio | 99,7 % (945/948) | 96,1 % (273/284) |
| Aplicación | 99,4 % (504/507) | 91,4 % (85/93) |
| **Dominio + aplicación** | **99,6 % (1449/1455)** | **95,0 % (358/377)** |
| Controllers | 91,7 % (233/254) | 72,6 % (61/84) |

- `mvn verify` falla si dominio + aplicación bajan del 80 % de líneas (`jacoco:check`).
- La infraestructura medida sin Docker da 36 % porque sus tests de MySQL quedan omitidos; la UI y el mock de la cátedra no se miden como objetivo.
- `ArquitecturaTest` verifica la regla de dependencias en cada build: dominio Java puro, aplicación solo con dominio, controllers sin repositorios ni JPA, notificación solo con `shared`, y la infraestructura sin excepciones de negocio.
