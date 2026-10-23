# Entregas, pruebas y evaluación

> Fuente: `tpo-consignas.pdf`, secciones 11 a 15.

## Entregas incrementales

| Hito | Contenido | Demostración |
|---|---|---|
| 1 · Diseño | Alcance, historias, modelo de dominio, diagrama de clases, decisiones de arquitectura y patrones candidatos. | Recorrer un caso de uso sobre el UML. |
| 2 · Núcleo | Dominio y casos de uso principales, con puertos y pruebas unitarias. | Publicar, solicitar, aceptar y validar estados. |
| 3 · Infraestructura | JPA/MySQL, repositorios y adaptadores externos mock. | Persistencia real y flujo transaccional. |
| 4 · Producto | Interfaz conectada a los controllers, documentación, pruebas y versión final. | Demo completa y defensa técnica. |

## Pruebas y calidad

- Pruebas unitarias del **dominio** y de **cada estrategia/política**.
- Pruebas de **casos de uso con puertos simulados**.
- Pruebas de **persistencia** con una base compatible y datos controlados.
- Pruebas de **integración** de controllers, casos de uso y persistencia.
- **Casos negativos**: transición inválida, saldo insuficiente, cupo agotado, doble pago y calificación no autorizada.
- Cobertura mínima sugerida: **80 % en dominio y aplicación**. La cobertura no reemplaza la calidad de los casos.
- **README** con instrucciones reproducibles para ejecutar la aplicación.

## Consignas de entrega

- **Diagrama de clases** completo de dominio y aplicación en **StarUML**, con relaciones, multiplicidades y patrones identificados.
- **Código fuente** organizado y repositorio con el trabajo del equipo.
- **Informe breve de decisiones**: problema, alternativa elegida, consecuencias y evidencia en código o pruebas.
- **Demo en vivo** del recorrido principal: **publicar → solicitar → aceptar → pagar → completar → calificar**.

## Evaluación

| Criterio | Se observa |
|---|---|
| Modelo y reglas de negocio | Invariantes, estados, value objects y comportamiento en el dominio. |
| Clean Architecture | Separación de capas, puertos, dependencias y casos de uso. |
| SOLID, GRASP y patrones | Responsabilidades, extensibilidad, justificación y pruebas. |
| Persistencia y transacciones | JPA, MySQL, migraciones, concurrencia y consultas. |
| Controllers e integración | Controllers delgados, manejo de errores e interfaz conectada. |
| Pruebas y calidad | Cobertura significativa, casos negativos y mantenibilidad. |
| Documentación y defensa | Trazabilidad, claridad y dominio individual de la solución. |

## Casos de aceptación obligatorios

Cada uno debería tener al menos un test automatizado:

1. Publicar una mentoría válida y encontrarla mediante búsqueda filtrada.
2. Rechazar la publicación de una oferta incompleta.
3. Solicitar, aceptar y pagar una mentoría con saldo suficiente.
4. Evitar que dos aceptaciones consuman el último cupo.
5. Rechazar un pago con saldo insuficiente sin modificar saldos ni estado.
6. Cancelar y calcular el reintegro con dos políticas diferentes.
7. Cambiar la estrategia de recomendación sin modificar el caso de uso que la consume.
8. Enviar dos tipos de notificación ante un mismo evento mediante adaptadores mock.
9. Aplicar promociones acumulables sin permitir un precio negativo.
10. Completar una contratación, calificarla una sola vez y recalcular la reputación.
