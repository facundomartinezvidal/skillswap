# Guía para levantar el proyecto desde cero

Paso a paso para clonar SkillSwap, abrir la app y correr los tests en una compu sin nada instalado.
El resumen técnico está en el [README](../README.md).

## 1. Instalar lo necesario

| Herramienta | Para qué | Notas |
|---|---|---|
| **JDK 17** | Compilar y correr | Por ejemplo, Eclipse Temurin 17. Comprobar con `java -version`: tiene que decir 17. |
| **Git** | Clonar el repo | |
| **Un IDE** | Abrir y ejecutar | **IntelliJ IDEA** (Community alcanza; trae Maven) o **VS Code** con la extensión *Extension Pack for Java*. |
| **Maven 3.9+** | Correr por consola | Opcional si usás el IDE. Comprobar con `mvn -v`. |
| **Una base MySQL** | Guardar los datos | Elegir **una** de las opciones del paso 3. Sin base de datos la app también anda, con `--memoria`. |

## 2. Clonar el repo

```bash
git clone https://github.com/facundomartinezvidal/skillswap.git
cd skillswap
```

Para estar al día antes de probar: `git checkout main` y `git pull`.

## 3. Preparar la base de datos (elegir una opción)

La app crea las tablas sola (Flyway) y carga los datos de demo la primera vez que arranca.
Solo hace falta que exista una base vacía y un usuario con permisos sobre ella.

### Opción A · Docker (la más simple)

```bash
docker compose up -d
```

Levanta MySQL 8 en el **puerto 3307** con base, usuario y contraseña `skillswap`. Son los valores
por defecto de la app, así que no hay que configurar nada más.

### Opción B · MySQL instalado en la compu (por ejemplo, con Workbench)

Si ya tenés MySQL (suele estar en el puerto 3306), creá la base y un usuario propio para el
proyecto. En Workbench, conectado como root:

```sql
CREATE DATABASE skillswap;
CREATE USER 'skillswap'@'localhost' IDENTIFIED BY 'skillswap';
GRANT ALL PRIVILEGES ON skillswap.* TO 'skillswap'@'localhost';
FLUSH PRIVILEGES;
```

Después, al ejecutar la app, definir estas variables de entorno (el paso 4 explica dónde):

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/skillswap` |
| `DB_USER` | `skillswap` |
| `DB_PASSWORD` | `skillswap` |

### Opción C · Sin base de datos

Ejecutar con el argumento `--memoria`. Usa los mismos datos de demo y se pierde todo al cerrar.
Sirve como plan B para la demo.

## 4. Abrir la app

La clase principal es `ar.edu.uade.skillswap.SkillSwapApplication`.

Argumentos útiles:

| Argumento | Qué hace |
|---|---|
| `--usuario user-ana` | Entra como otro usuario (login simulado). Sin este argumento entra `user-mateo`. |
| `--memoria` | Usa la opción C, sin base de datos. |

Usuarios de demo: `user-mateo`, `user-ana`, `user-sofia`, `user-tomas`, `user-camila`, `user-lucia`,
`user-laura`, `user-martin`, `user-nicolas`.

### Desde IntelliJ IDEA

1. *File → Open…* y elegir la carpeta `skillswap`. IntelliJ detecta el `pom.xml`: aceptar "Load Maven project".
2. *File → Project Structure → SDK*: elegir el JDK 17.
3. Abrir `SkillSwapApplication.java` y tocar el ▶ verde al lado de `main`.
4. Con la opción B o para cambiar de usuario: *Run → Edit Configurations…* y completar:
   - *Program arguments*: por ejemplo, `--usuario user-ana`.
   - *Environment variables*: `DB_URL=jdbc:mysql://localhost:3306/skillswap;DB_USER=skillswap;DB_PASSWORD=skillswap`.

### Desde VS Code

1. Instalar la extensión *Extension Pack for Java* y abrir la carpeta `skillswap`.
2. Esperar a que termine de cargar el proyecto Java (abajo a la derecha).
3. Abrir `SkillSwapApplication.java` y tocar *Run* arriba de `main`.
4. Para usar argumentos o variables, crear `.vscode/launch.json` con *Ejecutar y depurar → crear un archivo launch.json*, y en la configuración agregar `"args": "--usuario user-ana"` y `"env": { "DB_URL": "...", "DB_USER": "...", "DB_PASSWORD": "..." }`.

### Desde la consola (con Maven instalado)

```bash
mvn compile exec:java                                    # opción A (Docker)
mvn compile exec:java -Dexec.args="--memoria"            # opción C
mvn compile exec:java -Dexec.args="--usuario user-ana"   # otro usuario
```

Para la opción B, definir antes las variables. En PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/skillswap"; $env:DB_USER="skillswap"; $env:DB_PASSWORD="skillswap"
mvn compile exec:java
```

## 5. Correr los tests

```bash
mvn test      # todos los tests; reporte de cobertura en target/site/jacoco/index.html
mvn verify    # además controla la cobertura mínima (80 % en dominio + aplicación)
```

Desde el IDE: clic derecho en `src/test/java` → *Run Tests*.

Los tests de `infrastructure/persistencia` necesitan **Docker andando** (Testcontainers). Sin Docker
se omiten, no fallan. En GitHub Actions corren siempre.

## 6. Probar el recorrido de la demo

El guion completo está en [`demo.md`](demo.md). La versión corta:

1. Entrar como `user-mateo` → *Explorar* → solicitar una mentoría.
2. Entrar como el proveedor → *Contrataciones* → "Avanzar estado" (acepta).
3. Volver como `user-mateo` → "Avanzar estado" (paga).
4. Como el proveedor → "Avanzar estado" dos veces (inicia y completa).
5. Cualquiera de los dos → "Calificar".

## Problemas comunes

| Síntoma | Causa y solución |
|---|---|
| `Communications link failure` al arrancar | MySQL no está levantado o el puerto no coincide. Con Docker es el 3307; con un MySQL instalado, el 3306 y las variables del paso 3B. |
| `Access denied for user 'skillswap'` | Falta crear el usuario o darle permisos (paso 3B). |
| Docker Desktop no arranca | Usar la opción B o la C. Los tests de MySQL se omiten. |
| El IDE marca errores en todo el código | El proyecto no está usando el JDK 17, o Maven todavía no terminó de descargar las dependencias. |
| Quiero los datos de demo desde cero | Docker: `docker compose down -v`. MySQL instalado: `DROP DATABASE skillswap; CREATE DATABASE skillswap;`. La próxima vez que arranca, la app los vuelve a cargar. |
