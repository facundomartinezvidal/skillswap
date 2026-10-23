# Reglas de negocio mínimas

> Fuente: `tpo-consignas.pdf`, sección 5.

1. El autor **no puede solicitar su propia publicación**.
2. Una publicación **no publicada no admite solicitudes**.
3. No puede reservarse una **franja ocupada** ni **superar el cupo**.
4. El débito y el crédito de SkillCoins se hacen en **una única transacción**.
5. La cancelación devuelve **100 %, 50 % o 0 %** según una **anticipación configurable**.
6. Las promociones pueden modificar el precio, pero **nunca producir un total negativo**.
7. Solo los **participantes** de una contratación **completada** pueden calificarla, y **una sola vez** cada uno.
8. Toda transición de estado registra **fecha, actor y motivo** en un **historial inmutable**.
9. La baja de un perfil **no elimina operaciones históricas**: se hace **baja lógica**.
10. Los controllers que modifican información **validan permisos** a partir del `userId` recibido del servicio de identidad mock.
