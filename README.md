# SkillSwap

Trabajo Práctico Integrador de Procesos de Desarrollo de Software (UADE).
SkillSwap es un marketplace de mentorías, servicios breves y desafíos pagados
con créditos ficticios SkillCoins.

La guía funcional y las restricciones del trabajo están en
[`docs/consigna/`](docs/consigna/). Si una transcripción contradice el PDF
oficial, prevalece el PDF.

## Requisitos

- Java 17
- Maven
- MySQL para la futura integración persistente

## Ejecutar y probar

```bash
mvn test
mvn compile
mvn exec:java
mvn exec:java -Dexec.args="--screenshot out.png"
```

La aplicación Swing conserva la implementación mock existente. El dominio y
los casos de uso están completos y probados, pero todavía no están conectados a
esa UI ni a MySQL. `BackendEnMemoria` arma todos los casos de uso en memoria para
los tests y para la demo sin base de datos.

## Código

- `src/main/java/ar/edu/uade/skillswap/domain/`: reglas en Java puro por módulo
  (`publicacion`, `contratacion`, `wallet`, `perfil`, `notificacion`, `shared`),
  con sus puertos de repositorio.
- `src/main/java/ar/edu/uade/skillswap/application/`: casos de uso (un Service por
  flujo), DTOs y puertos de transacción, búsqueda, identidad y envío de notificaciones.
- `src/main/java/ar/edu/uade/skillswap/infrastructure/`: implementación en memoria,
  adapters simulados de email, push e identidad, y `configuracion/BackendEnMemoria`.
- `src/main/resources/db/migration/`: migraciones Flyway versionadas.
- `docs/arquitectura.md`: capas, casos de uso, patrones y decisiones.
- `docs/plan-de-trabajo.md`: pasos sugeridos para completar los cuatro hitos.

No se usa Spring ni HTTP/REST. La infraestructura JPA y la integración de
controllers son trabajo pendiente; revisar `AGENTS.md` antes de conectar la UI.
