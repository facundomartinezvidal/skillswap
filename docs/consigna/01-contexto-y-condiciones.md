# Contexto, condiciones y objetivos

> Fuente: `tpo-consignas.pdf`, secciones 1, 2, 3 y 16.

## Misión

*SkillSwap — Marketplace de talento, mentorías y proyectos · Backend con Java, Hibernate y JPA.*

Construir la lógica de una plataforma donde profesionales ofrecen mentorías o servicios breves, las organizaciones publican desafíos y los usuarios contratan usando créditos ficticios. La cátedra entrega una interfaz Java con datos mock; el equipo debe conectarla con sus controllers y casos de uso.

## Condiciones generales

- Equipos de hasta **5 integrantes**.
- Se desarrolla en **Java con Hibernate/JPA y MySQL**. **No se usa Spring Boot.**
- El foco de la evaluación es el **backend**: dominio, controllers, casos de uso, arquitectura, persistencia y pruebas.
- Cada entrega incluye el **código fuente**, el **modelo UML en StarUML** y un **informe PDF** con las decisiones de diseño.
- En la defensa, **cualquier integrante** tiene que poder explicar y modificar cualquier parte de la solución.
- **No se evalúa el diseño visual.** La cátedra provee una interfaz Java de referencia con datos mock.

## Contexto del producto

SkillSwap nace dentro de una empresa que quiere conectar a personas que necesitan resolver problemas concretos con profesionales dispuestos a enseñar o prestar un servicio.

- Un usuario puede actuar como **cliente**, como **proveedor de talento** o como **ambas cosas**.
- La plataforma combina una red profesional con un marketplace.
- Todas las operaciones económicas se hacen con créditos ficticios llamados **SkillCoins**.
- Ejemplos de ofertas: mentoría para preparar entrevistas, revisión de portfolio, asesoría de arquitectura, sesión de fotografía, creación de un prototipo.
- **Extensibilidad requerida:** el sistema tiene que poder incorporar nuevas **categorías**, **reglas de precio**, **medios de notificación** y **proveedores externos**.

## Objetivos de aprendizaje

| Competencia | Evidencia esperada |
|---|---|
| Clean Architecture | Dependencias hacia el dominio; casos de uso independientes de la interfaz, JPA y proveedores externos. |
| SOLID y GRASP | Responsabilidades cohesivas, bajo acoplamiento, polimorfismo y controllers delgados. |
| Patrones de diseño | Patrones justificados por variaciones reales del problema, no por cantidad. |
| Persistencia | Modelo relacional consistente, transacciones, relaciones JPA y consultas eficientes. |
| Calidad | Pruebas automatizadas sobre reglas, controllers, casos de uso y persistencia. |

## Fuera de alcance

- Autenticación, registro, recuperación de contraseña y autorización OAuth/JWT real.
- Dinero real, integración bancaria o procesamiento de tarjetas.
- Chat en tiempo real, videollamadas y almacenamiento de archivos pesados.
- Despliegue productivo de alta disponibilidad.
- Diseño visual responsivo desde cero.
