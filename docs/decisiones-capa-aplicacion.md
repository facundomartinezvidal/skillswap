# Decisiones de la capa de aplicación

Formato del informe: **problema → alternativa elegida → consecuencias → evidencia**.
Las decisiones de dominio están en `AGENTS.md` ("Decisiones de dominio acordadas").

## 1. Un Service por flujo

- **Problema:** organizar los casos de uso sin que la capa crezca en clases que nadie puede explicar.
- **Alternativa elegida:** un Service por flujo (`PerfilService`, `PublicacionService`, `BusquedaService`, `ContratacionService`, `WalletService`, `NotificacionService`), que se corresponde con las interfaces de `UiControllers`. Es el estilo de la Clase 9 y de Magic Café. La alternativa descartada era una clase por caso de uso, que da unas 40 clases muy chicas.
- **Consecuencias:** cada controller delega en un solo Service. Los Services no tienen reglas de negocio: coordinan agregados que se conocen solo por id.
- **Evidencia:** `application/*/…Service.java`; un test por Service.

## 2. Dónde van los puertos

- **Problema:** las interfaces que implementa la infraestructura tienen que estar del lado de adentro (regla de dependencias).
- **Alternativa elegida:** los repositorios, en la raíz de cada módulo de dominio, como ya estaban y como en Magic Café. Los demás puertos, en `application`: `UnidadDeTrabajo`, `BuscadorPublicaciones` (la búsqueda cruza publicación y perfil), `ServicioIdentidad` y `EnvioNotificacion`.
- **Consecuencias:** `Pagina` y `PaginaSolicitada` van en `domain/shared` porque las devuelven los repositorios.
- **Evidencia:** `ArquitecturaTest`: el dominio no depende de `application` ni de `infrastructure`, y `application` solo depende del dominio.

## 3. Atomicidad con una unidad de trabajo

- **Problema:** pagar, aceptar, completar, cancelar, reprogramar, calificar y dar de alta un perfil tocan varios agregados y tienen que ser todo o nada (regla 4).
- **Alternativa elegida:** un puerto `UnidadDeTrabajo.ejecutar(Supplier)`. Con JPA es una `EntityTransaction`. En memoria, `UnidadDeTrabajoEnMemoria` anota los `save` y los aplica recién si la operación termina bien (rollback real). Además, el Service modifica primero todos los agregados y guarda al final.
- **Consecuencias:** los repositorios en memoria devuelven copias, con `reconstruir(...)`, como una base. Por eso `reconstruir` es público, y `ArquitecturaTest` verifica que la aplicación no lo use.
- **Evidencia:** caso 5 (saldo insuficiente: no cambia nada); `siAlgoFallaDespuesDeMoverFondosNoQuedaNadaGuardado`; `enUnaTransaccionQueFallaNoSeGuardaNada`.

## 4. El cupo se reserva al aceptar

- **Problema:** la consigna dice que la aceptación bloquea el cupo y que una PAUSADA no admite solicitudes **nuevas**.
- **Alternativa elegida:**
  - Al solicitar, `Publicacion.franjaParaSolicitar(franja, ahora)` valida la regla 2 y que la franja no haya empezado y tenga lugar.
  - Al aceptar, `reservarCupo` toma el lugar sin mirar el estado.
  - Al cancelar, se libera si `ocupaCupo()`.
- **Consecuencias:** con la publicación pausada se puede aceptar una solicitud que ya llegó y reprogramar una contratación existente. Pendiente de decidir: hoy también se puede con una publicación cerrada o cancelada.
- **Evidencia:** caso 4 (`caso4_dosAceptacionesNoConsumenElUltimoCupo`); `conLaPublicacionPausadaSeAceptaUnaSolicitudQueYaHabiaLlegado`.

## 5. El pago queda retenido en garantía

- **Problema:** si se cancela, no hay que debitarle al proveedor algo que ya pudo gastar.
- **Alternativa elegida:** pagar mueve el precio del cliente a la Wallet de garantía; completar lo libera al proveedor; cancelar lo reparte según la `PoliticaReintegro` que elige la Simple Factory (Strategy). Solo se reparte si hay algo pagado.
- **Consecuencias:** la suma de saldos se conserva y nadie queda en negativo. El doble cobro lo impide el estado de la contratación.
- **Evidencia:** caso 3, caso 6 (72 h → 100 %, 30 h → 50 %, cancela el proveedor → 100 %), `unSegundoPagoSeRechazaSinCobrarDosVeces`.

## 6. Notificaciones: Observer, Simple Factory y Adapter

- **Problema:** un mismo evento interesa a varias personas, cada una elige su canal (email, push o interno), y los proveedores tienen que ser reemplazables.
- **Alternativa elegida:**
  - Los eventos los emite el dominio, que también dice quiénes son los interesados.
  - El Service los publica después de confirmar, en el `PublicadorEventos` (Observer).
  - `NotificadorDeInteresados` deja la notificación en la bandeja y la envía por el canal del perfil, que elige `CanalEnvioFactory` (Simple Factory).
  - `EmailAdapter` y `PushAdapter` traducen a la API de cada proveedor simulado (Adapter).
- **Consecuencias:** una reacción nueva es otro suscriptor, y un proveedor nuevo es otro adapter. Un envío que falla no deshace la operación: la bandeja queda.
- **Evidencia:** caso 8 (`caso8_unEventoSeEnviaPorEmailAlClienteYPorPushAlProveedor`); `siFallaUnEnvioLaBandejaQuedaYElOtroInteresadoLoRecibe`.

## 7. Recomendaciones intercambiables

- **Problema:** la consigna pide dos estrategias de recomendación intercambiables.
- **Alternativa elegida:** `EstrategiaRecomendacion` (Strategy), que entra por constructor en `BusquedaService`:
  - `AfinidadPorHabilidades`: intereses y habilidades del usuario contra la categoría, las etiquetas y las habilidades del autor.
  - `PopularidadReciente`: contrataciones aceptadas en los últimos 30 días.
- **Consecuencias:** cambiar la estrategia es cambiar una línea del composition root. Limitación conocida: las candidatas son todas las publicadas (`findPublicadas` sin límite).
- **Evidencia:** caso 7 (`caso7_cambiarLaEstrategiaCambiaLaRecomendacionSinTocarElCasoDeUso`).

## 8. Permisos con el userId (regla 10)

- **Problema:** los controllers reciben el `userId` del login simulado, y cada operación tiene que validar que quien actúa pueda hacerla.
- **Alternativa elegida:**
  - Los roles de la contratación los valida el dominio (`AccionNoPermitidaException`).
  - La autoría de una publicación y la visibilidad (BORRADOR y PAUSADA solo para el autor) las valida el Service, apoyado en `Publicacion.esVisiblePara`.
  - Perfil y Wallet operan solo sobre el userId que llega.
- **Consecuencias:** los controllers quedan delgados; solo traducen las excepciones (`DomainException` y `ApplicationException`) a mensajes.
- **Evidencia:** `soloElAutorModificaSuPublicacion`, `unBorradorOUnaPausadaSoloLaVeSuAutor`, `soloElClientePaga`.

## 9. Composition root sin Spring

- **Problema:** la consigna prohíbe Spring; las dependencias se inyectan a mano.
- **Alternativa elegida:** `BackendEnMemoria` arma todos los Services por constructor. La versión con base de datos va a ser otro composition root igual, que cambie los repositorios en memoria por los de JPA.
- **Consecuencias:** los Services no saben qué implementación usan, y la demo puede correr sin MySQL.
- **Evidencia:** `RecorridoCompletoTest`, el recorrido de la demo de punta a punta.
