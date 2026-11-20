# Historias de usuario

Alcance del Hito 1 (`docs/consigna/07-entregas-pruebas-y-evaluacion.md`): una historia por flujo del sistema.
Cada una tiene criterios de aceptación y el test que los verifica. Los casos 1 a 10 son los casos de
aceptación de la consigna, en `src/test/java/.../controllers/CasosDeAceptacionTest.java`.

**Actores**
- **Profesional**: ofrece mentorías o servicios y también puede contratar.
- **Organización**: publica desafíos.
- **Cliente**: cualquier perfil que contrata una publicación.
- **Sistema**: notificaciones y recomendaciones.

---

## HU-01 · Registrar y administrar mi perfil

**Como** usuario identificado por el servicio de identidad, **quiero** crear mi perfil y mantener mis datos,
**para** que otros sepan qué sé hacer y cuándo estoy disponible.

- El perfil usa el `userId` del servicio de identidad (simulado). Si el usuario no existe, no se crea.
- Al crear el perfil se abre la Wallet con el bono inicial, en la misma transacción.
- Se editan el nombre visible, la biografía, la zona horaria, las habilidades, los intereses, la disponibilidad y el canal de notificación.
- La reputación no se edita a mano.
- Un perfil dado de baja no publica, no reanuda publicaciones y no recibe solicitudes.

*Tests:* `PerfilServiceTest`, `PerfilOperacionesTest`.

## HU-02 · Ver un perfil público

**Como** cliente, **quiero** ver el perfil de quien ofrece algo, **para** decidir si lo contrato.

- Muestra la reputación, las publicaciones activas y un historial resumido.

*Tests:* `PerfilServiceTest`, `PerfilOperacionesTest`.

## HU-03 · Publicar una oferta o un desafío

**Como** profesional (oferta) u organización (desafío), **quiero** publicar lo que ofrezco o necesito,
**para** recibir solicitudes.

- El tipo de publicación sale del tipo de perfil: un profesional publica una oferta y una organización, un desafío.
- Se arma un BORRADOR editable y no visible.
- Para publicar hacen falta un título, una categoría, un precio no negativo y al menos una franja. Si falta algo, se rechaza (**caso 2**).
- Ya publicada, se encuentra con la búsqueda (**caso 1**).

*Tests:* casos 1 y 2, `PublicacionServiceTest`, `PublicacionTest`.

## HU-04 · Gestionar el estado de mis publicaciones

**Como** autor, **quiero** pausar, reanudar, cerrar o cancelar mis publicaciones, **para** controlar cuándo recibo solicitudes.

- Las transiciones válidas son PUBLICADA ⇄ PAUSADA, → CERRADA y → CANCELADA. Cancelar exige un motivo.
- CERRADA y CANCELADA no se reabren. El dominio rechaza las transiciones inválidas.
- Solo el autor cambia el estado de su publicación.

*Tests:* `EstadoPublicacionTest`, `PublicacionServiceTest`, `CasosNegativosTest`.

## HU-05 · Aplicar promociones

**Como** autor, **quiero** agregar descuentos a mi publicación, **para** atraer más clientes.

- Hay descuentos porcentuales (1 a 100 %) y fijos, y se acumulan.
- El precio final nunca es negativo (**caso 9**).

*Tests:* caso 9, `PrecioTest`, `PromocionTest`.

## HU-06 · Buscar publicaciones

**Como** cliente, **quiero** buscar por texto, categoría, modalidad, rango de precio, habilidad y reputación mínima,
**para** encontrar lo que necesito.

- Se ordena por relevancia, mejor reputación, menor precio o más reciente.
- Los listados están paginados.
- Solo aparecen las publicaciones PUBLICADA.

*Tests:* caso 1, `BusquedaServiceTest`, `ListadosPaginadosTest`.

## HU-07 · Recibir recomendaciones

**Como** usuario, **quiero** que el sistema me recomiende publicaciones, **para** descubrir cosas nuevas.

- Hay dos estrategias intercambiables: afinidad por habilidades y popularidad reciente.
- Cambiar de estrategia no cambia el caso de uso (**caso 7**).

*Tests:* caso 7, `BusquedaServiceTest`.

## HU-08 · Solicitar una contratación

**Como** cliente, **quiero** solicitar una publicación con un mensaje, una franja y un objetivo, **para** reservar una sesión.

- La solicitud queda PENDIENTE y le avisa al proveedor.
- No se puede solicitar la publicación propia, una que no esté PUBLICADA ni una franja que ya empezó o no tiene lugar.

*Tests:* caso 3, `ContratacionServiceTest`.

## HU-09 · Aceptar o rechazar una solicitud

**Como** proveedor, **quiero** aceptar o rechazar las solicitudes que recibo, **para** administrar mi agenda.

- Al aceptar se reserva el cupo de la franja en una transacción. Dos aceptaciones no consumen el último cupo (**caso 4**).
- RECHAZADA es un estado final: solo se consulta el historial. El cliente puede volver a solicitar, y eso crea una contratación nueva.

*Tests:* casos 3 y 4, `ContratacionTerminadaTest` (llega con el PR #26).

## HU-10 · Pagar con SkillCoins

**Como** cliente, **quiero** pagar la contratación aceptada con mis SkillCoins, **para** confirmarla.

- El pago queda retenido en garantía hasta que la sesión se completa.
- No se permiten saldos negativos ni doble cobro. Si el saldo no alcanza, no cambia nada (**caso 5**).

*Tests:* casos 3 y 5, `WalletTest`, `RetencionDePagosTest`.

## HU-11 · Reprogramar una sesión

**Como** cliente o proveedor, **quiero** proponer otra franja antes de que empiece la sesión, **para** adaptarme a un imprevisto.

- Solo se reprograma si las dos partes aceptan.

*Tests:* `ContratacionServiceTest`, `ContratacionTest`.

## HU-12 · Cancelar con reintegro

**Como** participante, **quiero** cancelar una contratación y recuperar lo que corresponda, **para** no perder créditos injustamente.

- Si cancela el cliente: 100 % con 48 h o más de anticipación, 50 % entre 24 y 48 h y 0 % con menos de 24 h.
- Si cancela el proveedor: el cliente recupera el 100 %.
- La política se elige en el momento de cancelar (**caso 6**).

*Tests:* caso 6, `PoliticaReintegroTest`.

## HU-13 · Completar y calificar

**Como** participante, **quiero** calificar a la otra parte cuando la sesión termina, **para** construir reputación confiable.

- Al completar se libera el pago al proveedor.
- Cada parte califica una sola vez y nadie se califica a sí mismo.
- La reputación se recalcula (**caso 10**).
- En la UI hay un botón "Calificar" en las contrataciones COMPLETADA.

*Tests:* caso 10, `ContratacionTest`.

## HU-14 · Recibir notificaciones

**Como** usuario, **quiero** enterarme de lo que pasa con mis publicaciones y contrataciones, **para** actuar a tiempo.

- Cada evento del dominio genera una notificación interna, y otra por email o push según el canal del perfil (**caso 8**).
- Se marcan como leídas de a una o todas juntas.

*Tests:* caso 8, `NotificacionServiceTest`.

## HU-15 · Consultar mi wallet

**Como** usuario, **quiero** ver mi saldo y mis movimientos, **para** controlar mis SkillCoins.

- Se ve el saldo y los movimientos paginados, con el concepto y el saldo resultante.
- La Wallet de garantía no se toca desde la UI.

*Tests:* `WalletServiceTest`, `ListadosPaginadosTest`.
