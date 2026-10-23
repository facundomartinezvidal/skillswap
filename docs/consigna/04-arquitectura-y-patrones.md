# Arquitectura y patrones de diseño

> Fuente: `tpo-consignas.pdf`, secciones 6 y 7.

## Arquitectura obligatoria

Hay que respetar el **principio de dependencias de Clean Architecture**. Los nombres de paquete son libres, pero la separación tiene que verse claramente:

| Capa | Responsabilidad | No debería conocer |
|---|---|---|
| **Dominio** | Entidades, value objects, reglas, políticas y eventos. | Interfaz gráfica, JPA, MySQL. |
| **Aplicación** | Casos de uso, puertos de entrada/salida y coordinación transaccional. | Controllers concretos y SDKs externos. |
| **Infraestructura** | JPA, repositorios, adaptadores externos, scheduler y configuración. | Decisiones de negocio nuevas. |
| **Presentación** | Interfaz Java, modelos de vista y captura de datos. | Detalles de persistencia y reglas de negocio. |
| **Controllers** | Reciben acciones de la interfaz, validan datos de entrada y delegan en casos de uso. | SQL, componentes visuales y reglas internas de entidades. |

### Cómo aplicarlo sin Spring (criterio del equipo, no está en el PDF)

- El dominio es **Java puro**, sin anotaciones JPA. Las entidades JPA viven en infraestructura y se mapean al dominio.
- Los repositorios son **interfaces (puertos)** del lado de dominio o aplicación y se implementan en infraestructura.
- La **inyección de dependencias es manual**: un *composition root* en `main` arma todo el grafo de objetos.
- Las transacciones se manejan explícitamente (`EntityManager` / `EntityTransaction`) desde la capa de aplicación, a través de un puerto transaccional.

## Patrones de diseño

Hay que aplicar **al menos cuatro patrones**, además de evidenciar **SOLID y GRASP**. No es obligatorio usarlos todos. **Cada patrón tiene que resolver una variación o dependencia concreta y estar respaldado por pruebas.**

| Patrón sugerido | Variación del problema |
|---|---|
| Strategy | Cálculo de recomendación, precio o política de cancelación. |
| State | Comportamiento y transiciones de publicación o contratación. |
| Observer | Publicación de eventos y notificaciones a múltiples interesados. |
| Adapter | Identidad, email, push o pasarela de SkillCoins simulada. |
| Simple Factory | Creación del canal de notificación o de la estrategia seleccionada. |
| Decorator | Promociones, comisiones o beneficios acumulables sobre el precio. |
| Facade | Vista resumida del perfil o coordinación simplificada del checkout. |
| Composite | Catálogo jerárquico de categorías y habilidades. |
| Singleton | Solo si se demuestra que hace falta una única instancia y se justifican sus consecuencias. |
