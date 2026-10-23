# Persistencia con MySQL, Hibernate y JPA

> Fuente: `tpo-consignas.pdf`, sección 8.

- Persistir: **perfiles, habilidades, publicaciones, franjas, solicitudes, contrataciones, movimientos de créditos, calificaciones, notificaciones e historial de estados**.
- Modelar correctamente **cardinalidades, claves, restricciones de unicidad e índices**.
- Usar **migraciones versionadas con Flyway** (o Liquibase). **No depender de `ddl-auto=create`** en la entrega.
- Definir **límites transaccionales** para **aceptación, reserva, pago y cancelación**.
- **Evitar N+1** en las consultas principales y **justificar** los fetch plans o proyecciones usados.
- Incluir **datos de demostración reproducibles** para el entorno local.
- Implementar **optimistic locking** (`@Version`) o una solución equivalente para **evitar la sobreventa de cupos**.
