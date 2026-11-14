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
casos de uso nuevos todavía no están conectados a esa UI ni a MySQL.

## Código inicial

- `src/main/java/ar/edu/uade/skillswap/domain/`: reglas Java puro para
  publicaciones (`domain/publicacion`), contrataciones (`domain/contratacion`) y
  value objects compartidos (`domain/shared`).
- Los casos de uso y puertos de salida (`application/`) están pendientes de
  reconstruirse sobre el nuevo dominio.
- `src/main/resources/db/migration/`: migraciones Flyway versionadas.
- `docs/arquitectura.md`: límites y decisiones iniciales.
- `docs/plan-de-trabajo.md`: pasos sugeridos para completar los cuatro hitos.

No se usa Spring ni HTTP/REST. La infraestructura JPA, el composition root y la
integración de controllers son trabajo pendiente; revisar la guía antes de
conectar la UI.
