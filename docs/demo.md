# Guion de la demo

Unos 10 minutos. El usuario activo de la UI es **Mateo** (`user-mateo`), que es proveedor
de "Mentoría de Java inicial" y cliente de otras publicaciones.

## Antes de empezar

```bash
docker compose down -v      # base limpia (opcional)
docker compose up -d
mvn compile exec:java
```

Si MySQL no está disponible: `mvn compile exec:java -Dexec.args="--memoria"` (mismos datos).
Al arrancar se aplican las migraciones de Flyway y se cargan los datos de demo una sola vez.

## Datos que carga la demo

- 9 perfiles: Mateo, Ana, Lucía, Sofía, Tomás, Camila, Nicolás, Laura y Martín.
- 7 publicaciones publicadas, con franjas y etiquetas, y un borrador de Mateo ("Consultas SQL desde cero").
- Promociones: Sofía (−20 fijo), Tomás y Camila (−10 %).
- Reputación armada con contrataciones completadas y calificadas.
- Mateo: bono de bienvenida más una carga de 1500 SkillCoins, y una contratación en cada estado:

| Contratación | Rol de Mateo | Estado | Botones |
|---|---|---|---|
| Mentoría de Java (Ana) | proveedor | PENDIENTE | "Aceptar" o "Rechazar" (pide motivo) |
| Arquitectura limpia (Camila) | cliente | ACEPTADA | "Pagar 234 SkillCoins": el saldo baja y queda retenido; "Reprogramar" |
| Mentoría de Java (Lucía) | proveedor | CONFIRMADA | "Iniciar sesión" y después "Completar sesión": cobra |
| Revisión de portfolio (Tomás) | cliente | CONFIRMADA | sin botón de avance: espera que Tomás inicie; "Reprogramar" |
| Excel (Martín) | cliente | CANCELADA | "Ver historial"; el reintegro ya se acreditó |

Cada fila muestra también el precio, el rol de Mateo y una línea que explica en qué está.

## Recorrido

1. **Inicio.** Saldo, recomendaciones por afinidad con las habilidades e intereses de Mateo,
   categorías y notificaciones recientes.
2. **Explorar (caso 1).** Buscar "Java", filtrar por categoría Tecnología o modalidad remota y
   ordenar por precio o reputación. Los precios ya incluyen las promociones (caso 9).
3. **Solicitar (caso 3).** Abrir "Preparación para entrevistas técnicas" de Sofía y tocar
   "Solicitar mentoría": elegir una de sus franjas, escribir el objetivo y, si se quiere, un mensaje.
   Aparece en Contrataciones como PENDIENTE y a Sofía le llega una notificación. Dejar el objetivo vacío
   muestra el error del dominio: es el manejo uniforme de errores.
4. **Contrataciones: el flujo de estados.**
   - Java con Ana → "Aceptar": pasa a ACEPTADA y reserva el lugar en la franja (Mateo es el proveedor).
   - Arquitectura con Camila → "Pagar 234 SkillCoins"; ir a **SkillCoins** y mostrar el
     débito. El pago queda retenido en la garantía hasta completar.
   - Java con Lucía → "Iniciar sesión" y "Completar sesión". Después **"Calificar"**
     (caso 10): puntaje y comentario; un segundo intento muestra el error del dominio. "Comprobante"
     muestra importe, fechas y calificaciones. En SkillCoins aparece el cobro de Mateo.
   - Portfolio con Tomás → "Reprogramar": Mateo elige otra franja; con **⇄ Cambiar usuario** a Tomás,
     aparece "Aceptar cambio" o "Rechazar cambio".
5. **Cancelar (caso 6).** Cancelar una contratación confirmada: el reintegro depende de la
   anticipación (100 % con más de 48 h, 50 % entre 24 y 48 h, 0 % con menos). Mostrar el
   movimiento en SkillCoins.
6. **Notificaciones (caso 8).** Cada operación generó notificaciones para los interesados;
   marcar una como leída.
7. **Mis publicaciones (caso 2).** "Publicar" el borrador de SQL. Pausar una publicación y
   volver a publicarla. Crear una "Nueva publicación" con precio, duración, lugares por franja y dos o
   tres franjas (el formulario no deja cargar dos que se superpongan) y publicarla. En un borrador,
   "Franjas" suma más antes de publicar.
8. **Persistencia.** Cerrar y volver a abrir la app con MySQL: todo lo hecho sigue ahí y los
   datos de demo no se duplican.

## Lo que no tiene botón en la UI

Cambiar la estrategia de recomendación (caso 7) se muestra con los tests:

```bash
mvn test -Dtest=CasosDeAceptacionTest
```

Ese test recorre los 10 casos de aceptación a través de los controllers. El caso 4
(dos aceptaciones por el último cupo) y el caso 5 (pago sin saldo) se prueban además contra
MySQL en `BackendJpaTest`, que necesita Docker.

## Antes de la defensa

- Base limpia para que los datos de demo estén como al principio: `docker compose down -v`
  y `docker compose up -d`; con un MySQL instalado, `DROP DATABASE skillswap;` y
  `CREATE DATABASE skillswap;`. La app vuelve a cargar la demo al arrancar.
- El recorrido completo con dos usuarios (publicar → solicitar → aceptar → pagar → completar →
  calificar) se hace en la misma ventana, pasando de Mateo a Sofía con **⇄ Cambiar usuario** en la
  barra lateral. Como el backend es el mismo, funciona también con `--memoria`. Para abrir directamente
  con otro usuario sigue estando `--usuario user-sofia`.
