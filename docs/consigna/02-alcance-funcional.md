# Alcance funcional obligatorio

> Fuente: `tpo-consignas.pdf`, sección 4.

## Perfiles y reputación

- Registrar un perfil profesional que referencie un `userId` provisto por un **servicio de identidad externo** (mock).
- Administrar nombre visible, biografía, **zona horaria**, habilidades, **intereses** y **disponibilidad**.
- Consultar el **perfil público** con reputación, publicaciones activas e historial resumido.
- La reputación **se recalcula a partir de calificaciones verificadas** y **no puede editarse manualmente**.

## Publicaciones

- Un **profesional** puede publicar una **oferta** de mentoría o servicio.
- Una **organización** puede publicar un **desafío**.
- Toda publicación tiene: **título, descripción, categoría, modalidad** (remota/presencial), **duración estimada, cupo, precio base en SkillCoins y etiquetas**.

| Estado | Significado |
|---|---|
| `BORRADOR` | Editable y no visible. |
| `PUBLICADA` | Visible y disponible para recibir solicitudes. |
| `PAUSADA` | Visible para su autor, pero no admite nuevas solicitudes. |
| `CERRADA` | Finalizada; **no puede reabrirse**. |
| `CANCELADA` | Retirada, con **motivo obligatorio**. |

- **El dominio rechaza las transiciones inválidas.**
- **Para publicar se requiere:** título, categoría, precio no negativo y **al menos una franja de disponibilidad**.

> **Interpretación del equipo (no está en el PDF):** que `PAUSADA` pueda volver a `PUBLICADA` es razonable, pero hay que decidirlo y documentarlo en el informe.

## Búsqueda y recomendación

- Buscar publicaciones por **texto, categoría, modalidad, rango de precio, habilidad y reputación mínima**.
- Ordenar por **relevancia, mejor reputación, menor precio o fecha más reciente**.
- Implementar **al menos dos estrategias de recomendación intercambiables**: **afinidad por habilidades** y **popularidad reciente**.
- **Paginar todos los listados** que devuelven los controllers de consulta.

## Solicitud, reserva y contratación

- Un cliente solicita una publicación indicando **mensaje, franja elegida y objetivo**.
- El proveedor **acepta o rechaza**. La aceptación **crea una reserva y bloquea el cupo de manera transaccional**.
- El cliente **confirma el pago con SkillCoins**. No se permiten **saldos negativos** ni **doble cobro**.
- Una reserva puede **reprogramarse antes de comenzar** si **ambas partes aceptan**.
- Al completar la sesión, se habilita **una calificación por cada parte**; una persona **no puede calificarse a sí misma**.

## Estados de una contratación

| Estado | Acciones válidas |
|---|---|
| `PENDIENTE` | Aceptar o rechazar. |
| `ACEPTADA` | Pagar, cancelar o proponer reprogramación. |
| `CONFIRMADA` | Iniciar, reprogramar o cancelar según política. |
| `EN_CURSO` | Completar. |
| `COMPLETADA` | Calificar y consultar comprobante. |
| `RECHAZADA` / `CANCELADA` | Solo consultar historial. |

Flujo feliz: `PENDIENTE → ACEPTADA → (pago) → CONFIRMADA → EN_CURSO → COMPLETADA`.

## Notificaciones

- Los eventos relevantes notifican a las partes: **solicitud recibida, aceptación, pago confirmado, reprogramación, cancelación y sesión completada**.
- Cada usuario elige su canal: **email, push o notificación interna**.
- Los proveedores reales se simulan, pero se conectan mediante **interfaces reemplazables**.
