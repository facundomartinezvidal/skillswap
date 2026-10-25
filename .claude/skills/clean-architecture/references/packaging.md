# Screaming architecture and packaging

Chapters 21 and 34: what the top-level structure should say, and the four ways to package code (and how to make the compiler enforce them).

## Screaming Architecture

> Your architecture should tell readers about the system, not about the frameworks you used in your system.

*Chapter 21.*

### What it means in practice

A building's blueprint screams "house" or "library" — you can see the use cases (kitchen, reading rooms) before knowing whether it's brick or wood. Open the top-level directory of a codebase and it should scream "health care", "accounting", "inventory" — not "Rails", "Spring", or "ASP.NET". Following Jacobson's use-case-driven approach, architectures are structures that support use cases, and frameworks, databases, and the web are options to leave open, not organizing principles.

The web in particular is a delivery mechanism — an IO device — so the app should be deliverable as web, console, desktop, or service without restructuring. Frameworks are tools, not ways of life: approach them skeptically and decide how to protect the use cases from them. A direct consequence is **testability**: if the use cases and entities are plain objects, you can unit-test all of them with no web server and no database running.

### Example: same app, two top-level structures

```
# Screams "Spring MVC"                 # Screams "veterinary clinic"
src/                                   src/
  controllers/                           appointments/
  services/                              patients/
  repositories/                          billing/
  models/                                prescriptions/
  config/                                web/        ← delivery detail, off to the side
                                         persistence/ ← detail
```

A new developer looking at the right-hand tree learns the use cases first; when they ask "where are the controllers?", the answer is "that's a detail — over there."

### Related

- Application Business Rules (Use Cases) (`layers-and-dependency-rule.md`)
- Decoupling Layers and Use Cases (`drawing-boundaries.md`)
- Keep Options Open: Policy vs. Details (`drawing-boundaries.md`)
- Frameworks Are Details (`details.md`)
- The Web Is a Detail (`details.md`)
- Packaging Strategies: Layer, Feature, Ports & Adapters, Component — package-by-layer is the textbook non-screaming structure.
- The Test Boundary (`details.md`)

## Packaging Strategies: Layer, Feature, Ports & Adapters, Component

> Your best design intentions can be destroyed in a flash if you don't consider the intricacies of the implementation strategy.

*Chapter 34 (by Simon Brown).*

### What it means in practice

Four ways to organize the same "view orders" feature:

| Strategy | Shape | Strength | Weakness |
|---|---|---|---|
| **Package by layer** | `web/`, `service/`, `data/` | Quick to start | Doesn't scream the domain; strict layering is easy to cheat |
| **Package by feature** | everything for `orders/` in one package | Screams the domain; one place to change | Business and persistence logic mixed inside |
| **Ports and adapters** | `domain/` inside (named in ubiquitous language: `Orders`, not `OrdersRepository`), web and persistence outside, depending in | The Dependency Rule made explicit | One shared "outside" tree lets a controller call a repository directly — the **Périphérique anti-pattern** (driving around the city instead of through it) |
| **Package by component** | UI separate; business logic + persistence bundled behind one `OrdersComponent` interface | One entry point per coarse-grained component; stepping stone to microservices | Requires access modifiers to mean anything |

Brown's real point: **these differences only exist if the compiler enforces them.** Mark every type `public` and packages become folders — all four approaches collapse into the same dependency graph, and the newcomer who injects `OrdersRepository` straight into `OrdersController` (a "relaxed layered architecture") breaks nothing that would stop the build. Code reviews and post-compile rule checkers (ArchUnit-style, NDepend, Structure101) are fallible with long feedback loops; prefer making implementation types package-private so the violation doesn't compile. Other enforcement modes: module systems (Java 9 modules, OSGi — *public* vs. *published*), or separate source trees/build modules per component (domain, web, persistence).

His definition of component differs from Martin's (unit of deployment): "a grouping of related functionality behind a nice clean interface, which resides inside an execution environment" (from his C4 model) — whether it's a separate jar is orthogonal.

### Example: package by component in Java

```java
package com.shop.orders;                       // the component

public interface OrdersComponent {             // the ONLY public type
    List<OrderDto> ordersFor(String customerId);
}

class OrdersComponentImpl implements OrdersComponent { ... }   // package-private
interface OrdersRepository { ... }                             // package-private
class JdbcOrdersRepository implements OrdersRepository { ... } // package-private
```

```java
package com.shop.web;
class OrdersController {
    OrdersController(OrdersComponent orders) { ... }   // can only see the interface
    // OrdersRepository repo;  ← doesn't compile: not visible outside com.shop.orders
}
```

### Related

- Screaming Architecture
- The Clean Architecture (the Circles) (`layers-and-dependency-rule.md`) — ports and adapters is one of the architectures it unifies.
- The Dependency Rule (`layers-and-dependency-rule.md`)
- Decoupling Layers and Use Cases (`drawing-boundaries.md`) — layer vs. feature packaging are the horizontal and vertical axes.
- Decoupling Modes and Boundary Anatomy (`drawing-boundaries.md`) — package-private vs. module vs. separate build: different strengths of source-level decoupling.
- Components (`component-principles.md`) — Martin's definition vs. Brown's.
- ADP: Acyclic Dependencies Principle (`component-principles.md`) — a strict layered graph is acyclic, yet can still be cheated.
