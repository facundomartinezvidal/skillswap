# SkillSwap

Trabajo Práctico Obligatorio de Procesos de Desarrollo de Software (UADE).
SkillSwap es un marketplace de mentorías, servicios breves y desafíos pagados
con créditos ficticios SkillCoins.

Backend en Java 17 con Clean Architecture, sin Spring ni REST: dominio, casos de
uso, persistencia con Hibernate/JPA, MySQL y Flyway, y controllers conectados a
la UI Swing de la cátedra. La consigna está en [`docs/consigna/`](docs/consigna/);
si una transcripción contradice el PDF oficial, prevalece el PDF.

## Requisitos

- Java 17 (JDK).
- Maven 3.9 o superior.
- Docker con Docker Compose, para el MySQL de desarrollo y los tests de persistencia.
  Sin Docker, la app corre con `--memoria` y los tests de MySQL se omiten.

## Ejecutar la demo

### Con MySQL (opción por defecto)

```bash
./correr.sh                       # levanta MySQL con Docker, compila y abre la app
./correr.sh --usuario user-ana    # cualquier argumento se le pasa a la app
./correr.sh --reset               # borra la base para volver a la demo original
```

`correr.sh` hace lo mismo que estos dos comandos (en Windows, sin Git Bash o WSL, usar estos):

```bash
docker compose up -d --wait
mvn compile exec:java
```

- `docker compose up -d` levanta MySQL 8.0 en el **puerto 3307** con base, usuario y
  contraseña `skillswap` (valores de desarrollo, no credenciales reales).
- Al arrancar, la app **aplica las migraciones de Flyway** (`src/main/resources/db/migration`)
  y **carga los datos de demo** si la base está vacía. Arrancar de nuevo no los duplica.
- Para usar otro MySQL, definir las variables de entorno antes de `mvn exec:java`:

| Variable | Por defecto |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3307/skillswap` |
| `DB_USER` | `skillswap` |
| `DB_PASSWORD` | `skillswap` |

```bash
DB_URL=jdbc:mysql://localhost:3306/skillswap DB_USER=yo DB_PASSWORD=secreto mvn exec:java
```

Para empezar de cero: `docker compose down -v` (borra el volumen con los datos).

### Sin base de datos (plan B)

```bash
mvn compile exec:java -Dexec.args="--memoria"
```

Arma los mismos Services con repositorios en memoria y carga los mismos datos de demo.
Los datos se pierden al cerrar.

### Captura de la pantalla de inicio

```bash
mvn compile exec:java -Dexec.args="--memoria --screenshot inicio.png"
```

El guion de la demo está en [`docs/demo.md`](docs/demo.md).

## Pruebas y cobertura

```bash
mvn test
mvn verify
```

- `mvn test` corre los 606 tests y deja el reporte de cobertura de JaCoCo en
  `target/site/jacoco/index.html`.
- Los 26 tests de `infrastructure/persistencia` usan Testcontainers: levantan un MySQL
  en Docker, migran con Flyway y prueban los repositorios, la búsqueda, las restricciones
  y la concurrencia. Si Docker no está disponible, se omiten (no fallan).
- `mvn verify` además exige al menos 80 % de líneas cubiertas en dominio + aplicación
  (hoy 99,6 %).

## Código

```
src/main/java/ar/edu/uade/skillswap/
├── domain/          reglas en Java puro por módulo (perfil, publicacion, contratacion,
│                    wallet, notificacion, shared), con sus puertos de repositorio
├── application/     un Service por flujo, DTOs y puertos (transacción, búsqueda, identidad, envío)
├── infrastructure/  persistencia JPA, implementación en memoria, adapters simulados de email,
│                    push e identidad, y los composition roots BackendJpa, BackendEnMemoria y DatosDemo
├── controllers/     contrato de la cátedra (UiControllers), interfaces del equipo y sus implementaciones
├── model/ ui/ mock/ código de la cátedra (modelos de vista, pantallas Swing y mock original)
└── SkillSwapApplication.java   main: elige el backend, carga la demo y abre la UI
```

## Documentación

- [`docs/guia-levantar-proyecto.md`](docs/guia-levantar-proyecto.md): paso a paso para instalar, abrir la app y correr los tests desde cero.
- [`docs/arquitectura.md`](docs/arquitectura.md): capas, casos de uso, patrones y persistencia.
- [`docs/informe-decisiones.md`](docs/informe-decisiones.md): decisiones en formato
  problema → alternativa → consecuencias → evidencia, patrones, SOLID/GRASP y casos de aceptación.
- [`docs/diagramas.md`](docs/diagramas.md): diagramas de clases en Mermaid.
- [`docs/demo.md`](docs/demo.md): guion de la demo.
- [`docs/plan-de-trabajo.md`](docs/plan-de-trabajo.md): estado por hito.
- [`AGENTS.md`](AGENTS.md): reglas y convenciones del repositorio.
