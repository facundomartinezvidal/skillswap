# Services, tests and details

Chapters 27–32: microservices, the test boundary, embedded systems, and why the database, the web and frameworks are details.

## Contents

- [Services Are Not an Architecture](#services-are-not-an-architecture)
- [The Test Boundary](#the-test-boundary)
- [Clean Embedded Architecture](#clean-embedded-architecture)
- [The Database Is a Detail](#the-database-is-a-detail)
- [The Web Is a Detail](#the-web-is-a-detail)
- [Frameworks Are Details](#frameworks-are-details)

## Services Are Not an Architecture

> Architectural boundaries do not fall between services. Rather, those boundaries run through the services, dividing them into components.

*Chapter 27.*

### What it means in practice

Architecture is defined by boundaries that separate high-level policy from low-level detail and obey the Dependency Rule (`layers-and-dependency-rule.md`). A service is just a function call across a process or network boundary: some are architecturally significant, many are just expensive function calls. Martin attacks the two selling points of (micro)services:

- **The decoupling fallacy.** Services don't share variables, but they're still coupled through shared resources and, above all, **shared data**: add a field to a record that flows between services and every service that touches it must change and agree on its meaning. And a service's interface is no more formal than a function signature.
- **The independent development/deployment fallacy.** Large systems have been built as monoliths and component systems too; and to the extent services are coupled by data or behavior, their development and deployment have to be coordinated anyway.

The **kitty problem** shows both: a ride-hailing system split by function (UI → finder → selector → dispatcher) gets a "deliver kittens" feature with its own driver/customer allergy rules — and *every* service has to change. Functional decomposition is fragile against **cross-cutting** features. The fix is the same one you'd use in a monolith: design with SOLID so features are polymorphic extensions (Template Method / Strategy) in their own components, created by factories, so a new feature is a new jar on the classpath (OCP (`solid.md`)). Services can be built the same way — **component-based services** whose internal component structure follows the Dependency Rule.

### Example: adding same-day delivery to a fulfillment pipeline

Services: `cart-service` → `inventory-service` → `shipping-quote-service` → `dispatch-service`. "Same-day delivery" touches all four (stock in nearby warehouse, special quote, courier dispatch, UI badge) — a coordinated four-service release.

Component-based alternative: each service exposes abstract extension points (`DeliveryOption`, `QuoteRule`, `DispatchStrategy`). The `same-day` feature ships as its own components implementing those abstractions in each service, loaded by configuration. The services' existing code doesn't change; only the UI has to know a new option exists.

### Related

- Decoupling Modes and Boundary Anatomy (`drawing-boundaries.md`) — a service is one physical form of boundary, not the boundary itself.
- Architectural Boundaries and the Plugin Architecture (`drawing-boundaries.md`)
- The Dependency Rule (`layers-and-dependency-rule.md`)
- OCP: Open-Closed Principle (`solid.md`)
- Anticipating Boundaries: When to Pay for One (`drawing-boundaries.md`)
- Keep Options Open: Policy vs. Details (`drawing-boundaries.md`)

## The Test Boundary

> Tests are not outside the system; rather, they are parts of the system that must be well designed if they are to provide the desired benefits of stability and regression.

*Chapter 28.*

### What it means in practice

Architecturally, every kind of test (TDD unit tests, Cucumber, FitNesse, ...) is the same thing: the **outermost circle**. Tests are concrete, depend inward on the code under test, nothing depends on them, and they're always deployed independently. Treating them as "outside the design" is catastrophic: tests coupled tightly to the system break by the hundreds on trivial changes (the **Fragile Tests Problem**), and then the team starts refusing changes — tests make the system *rigid*.

The first rule of design applies: **don't depend on volatile things**. GUIs are volatile, so tests that drive the GUI to check business rules are fragile by construction. Instead:

- **Testing API.** A dedicated API — a superset of the interactors and interface adapters the UI uses — with "superpowers": bypass security, skip expensive resources like the database, force the system into specific states. Keep it (and its dangerous parts) in a separately deployable component so it never ships to production.
- **Avoid structural coupling.** A test class per production class and a test method per production method locks tests to the code's structure. The testing API hides that structure, so production code can be refactored (and grow more abstract and general) while tests grow more concrete and specific — independently.

### Example: testing promotions without the storefront

```java
// Fragile: drives the browser to test a pricing rule
loginAs("vip@example.com"); clickCategory("Shoes"); addToCart("Runner X"); applyCoupon("VIP20");
assertThat(text("#total")).isEqualTo("$96.00");

// Through a testing API: same rule, no UI, no DB, no knowledge of internal classes
var shop = TestShop.inMemory();
var vip  = shop.givenCustomer().withTier(VIP).create();
shop.givenProduct("runner-x").pricedAt("120.00");
var cart = shop.cartFor(vip).add("runner-x").applyCoupon("VIP20");
assertThat(cart.total()).isEqualTo(Money.of("96.00"));
```

A checkout redesign breaks zero tests in the second style, and the pricing classes can be restructured freely as long as `TestShop` still works.

### Related

- The Dependency Rule (`layers-and-dependency-rule.md`) — tests obey it: they only point inward.
- The Clean Architecture (the Circles) (`layers-and-dependency-rule.md`)
- Humble Object (`layers-and-dependency-rule.md`) — the pattern that makes the core testable without the UI or DB.
- Screaming Architecture (`packaging.md`) — use cases testable with no framework running.
- SDP: Stable Dependencies Principle (`component-principles.md`) — "don't depend on volatile things" applied to tests.
- Architectural Boundaries and the Plugin Architecture (`drawing-boundaries.md`)

## Clean Embedded Architecture

> Although software does not wear out, it can be destroyed from within by unmanaged dependencies on firmware and hardware.

*Chapter 29 (by James Grenning).*

### What it means in practice

Grenning redefines **firmware** not by where code is stored (ROM, flash) but by *what it depends on*: code that knows the hardware is firmware and dies when the hardware changes; code that doesn't is software and can live a long time. The goal is less firmware, more software — and non-embedded developers write firmware too: SQL smeared through the code, or business logic tangled with the Android API.

Most embedded code passes only the **"App-titude test"** (it works), skipping Kent Beck's "make it right". The result is the **target-hardware bottleneck**: code can only be tested on a board that's late, scarce, and buggy. A clean embedded architecture is one that's **testable off-target**, built from substitution points:

- **Hardware is a detail → HAL.** A hardware abstraction layer shaped by what the *software* needs: not "write bytes to flash" but "store this name/value pair"; not `Led_TurnOn(5)` but `Indicate_LowBattery()`. Layers can nest fractally.
- **Processor is a detail → PAL.** Vendor C extensions (`SBUF0`, `TI_0`, custom headers) aren't C; confine them to a few firmware files and use standard headers like `stdint.h` (write one that maps the vendor types if needed).
- **OS is a detail → OSAL.** Wrap the RTOS so switching OS means writing a new OSAL, not editing the application; a bonus is a common concurrency style (e.g. message passing).
- **Interfaces and substitutability.** Headers as interfaces containing only what callers need; replace `#ifdef BOARD_V2` scattered thousands of times (a DRY violation) with link-time or runtime binding behind the HAL.

### Example: a smart thermostat

```c
// app/ (software — compiles and runs on a laptop)
#include "climate_hal.h"
void Thermostat_Tick(void) {
    Celsius t = Climate_ReadIndoorTemp();
    if (t < schedule_target()) Climate_StartHeating(); else Climate_StopHeating();
    if (Battery_IsLow()) Indicate_LowBattery();
}

// climate_hal.h — the seam, expressed in thermostat terms
Celsius Climate_ReadIndoorTemp(void);
void    Climate_StartHeating(void);
void    Indicate_LowBattery(void);

// firmware/board_v2/climate_hal.c — the only place that knows ADC channels and GPIO pins
Celsius Climate_ReadIndoorTemp(void) { return adc_to_celsius(ADC_Read(CH_TEMP_SENSOR)); }
```

Off-target, a `fake_climate_hal.c` drives `Thermostat_Tick` through cold mornings and dead batteries in milliseconds; the board revision is a different `.c` file at link time, not `#ifdef`s.

### Related

- Architectural Boundaries and the Plugin Architecture (`drawing-boundaries.md`)
- The Dependency Rule (`layers-and-dependency-rule.md`)
- The Test Boundary — off-target testability is the embedded version of the test boundary.
- The Database Is a Detail — the same "it's a detail" argument applied to storage.

## The Database Is a Detail

> The data is significant. The database is a detail.

*Chapter 30 and Appendix A.*

### What it means in practice

Martin separates two things that usually get conflated. The **data model** — how the application's data is structured — *is* architecturally significant. The **database** — the software that moves bits between disk and RAM — is not; it's a mechanism, like a doorknob on a house. The relational model is elegant and robust, but it's still a technology, and knowledge of rows and tables belongs in the lowest-level utilities of the outer circle. Frameworks that pass rows/tables around the system as objects are an architectural error: they couple use cases, business rules, even the UI to the storage shape.

Why do databases dominate? **Disks.** Disks are slow (milliseconds vs. nanoseconds), so we needed indexes, caches, and query schemes: file systems (good for whole documents by name) and RDBMSs (good for finding records by content). But once data is in RAM, programmers immediately reorganize it into lists, trees, hash tables, stacks, and queues — nobody keeps it as tables. Performance is a real concern, but a *low-level* one that can be encapsulated in the data-access layer.

Two war stories: Martin lost a fight against putting an RDBMS in a system's core — not on engineering grounds, but because customers expected one as a checkbox (he should have bolted it on the side with a narrow access channel). And in the Appendix, a team embedded vendor-specific SQL everywhere; when the vendor discontinued the product, porting was impossible and they paid for maintenance contracts on a dead database for years.

### Example: keeping SQL at the edge

```java
// Use case sees domain-shaped data only
public interface OrderHistory {
    List<OrderSummary> recentFor(CustomerId id, int limit);
}

// Outer circle: the only code that knows tables, joins, or that Postgres exists
class SqlOrderHistory implements OrderHistory {
    public List<OrderSummary> recentFor(CustomerId id, int limit) {
        return jdbc.query("""
            SELECT o.id, o.placed_at, SUM(li.qty * li.unit_price) AS total
            FROM orders o JOIN line_items li ON li.order_id = o.id
            WHERE o.customer_id = ? GROUP BY o.id ORDER BY o.placed_at DESC LIMIT ?""",
            this::toSummary, id.value(), limit);
    }
}
```

Moving order history to a read-optimized store, adding an index, or caching it in Redis are all changes to `SqlOrderHistory` alone.

### Related

- Keep Options Open: Policy vs. Details (`drawing-boundaries.md`)
- Architectural Boundaries and the Plugin Architecture (`drawing-boundaries.md`) — the database behind an interface, arrow pointing at the business rules.
- Humble Object (`layers-and-dependency-rule.md`) — gateway implementations and ORMs ("data mappers") are humble objects in the database layer.
- The Dependency Rule (`layers-and-dependency-rule.md`) — rows must not cross inward.
- The Main Sequence (`component-principles.md`) — a schema everything depends on lands in the Zone of Pain.
- The Web Is a Detail
- Frameworks Are Details

## The Web Is a Detail

> The GUI is a detail. The web is a GUI. So the web is a detail.

*Chapter 31.*

### What it means in practice

The web didn't change anything — it's one more swing of a pendulum the industry has been on since the 1960s: mainframes with smart terminals, minicomputers with dumb ones, client–server, thin browsers, applets, server-side rendering, Ajax/SPAs, then JavaScript back on the server. Architects should push those swings away from the core business rules. Marketing will also periodically demand a new look (Martin's examples: a desktop finance app redesigned to look like a browser and then back, a phone OS restyling every app) — and the only protection is having already decoupled business rules from the UI.

The web is an **IO device**, and the 1960s lesson of device independence still applies. Martin concedes that the *interaction* between a rich UI and the app is too chatty and UI-specific to abstract fully. But a different boundary can be abstracted: at some point in that back-and-forth, the input for a use case is **complete**; package it into a data structure, run the use case, and return the output as a data structure to feed back into the UI dance. That's the use case operating the UI in a device-independent way. It takes iterations to get right, but it's possible — and often necessary.

### Example: the same "transfer money" use case behind three UIs

The web SPA does field-by-field validation, autocompletes payees, and shows a confirmation dialog — all chatty, all web-specific. When the user hits *Confirm*, it sends one `TransferRequest(from, to, amount, memo)`. The mobile app (biometric confirm, different screens) and the bank's chatbot ("send $50 to Alex") build the same `TransferRequest` their own way. `TransferFunds` never knows which one called; a full web redesign or a new voice channel touches no business rule.

### Related

- Keep Options Open: Policy vs. Details (`drawing-boundaries.md`)
- Screaming Architecture (`packaging.md`) — the web is a delivery mechanism, not what the system is.
- Application Business Rules (Use Cases) (`layers-and-dependency-rule.md`) — request/response models are the device-independent boundary.
- Humble Object (`layers-and-dependency-rule.md`) — presenter/view split on the UI side of that boundary.
- The Database Is a Detail
- Frameworks Are Details
- OCP: Open-Closed Principle (`solid.md`) — device independence was OCP before it had a name.

## Frameworks Are Details

> Don't marry the framework!

*Chapter 32 and Appendix A.*

### What it means in practice

Frameworks are useful, but they aren't architectures. Their authors solve *their own* problems, not yours, and the relationship is an **asymmetric marriage**: the docs urge you to derive your business objects from their base classes and import their annotations everywhere — a huge commitment from you, none from them. The risks: the framework's own design often violates the Dependency Rule (`layers-and-dependency-rule.md`) by reaching into your Entities; you outgrow it and it fights you; it evolves in directions you don't want or drops features you rely on; or something better comes along and you can't switch.

The fix: use the framework, don't couple to it. Keep it in an outer circle, behind a boundary. If it wants your business objects to inherit from it, write proxies/adapters in plugin components instead. E.g. use Spring for wiring, but only in Main (`layers-and-dependency-rule.md`) — no `@Autowired` in business objects. Some frameworks you simply marry (a language's standard library, C++'s STL) — fine, but make it a conscious decision, because it's for the life of the application. "Date" a framework before committing.

A related Appendix lesson about *building* frameworks: you can't make a reusable framework until you've made a usable one — it has to be built in concert with several applications that actually reuse it.

### Example: keeping an ORM out of the domain

```java
// Married: the entity is a framework artifact
@Entity @Table(name = "subscriptions")
public class Subscription extends AbstractAuditableEntity {
    @ManyToOne(fetch = LAZY) private Plan plan;
    ...
}

// Dating: the entity is plain; the framework lives in an adapter
public class Subscription {                      // inner circle, no imports from Jakarta/Spring
    private final Plan plan; ...
}

@Entity @Table(name = "subscriptions")
class SubscriptionRow { ... }                    // outer circle, mapped by the ORM

class JpaSubscriptionGateway implements SubscriptionGateway {
    public Subscription byId(String id) { return toDomain(em.find(SubscriptionRow.class, id)); }
}
```

Moving off JPA (or upgrading through a breaking major version) now means rewriting `JpaSubscriptionGateway` and the row classes — not the billing rules.

### Related

- Keep Options Open: Policy vs. Details (`drawing-boundaries.md`)
- Screaming Architecture (`packaging.md`) — "frameworks are tools, not ways of life."
- The Dependency Rule (`layers-and-dependency-rule.md`)
- The Main Component (`layers-and-dependency-rule.md`) — where a DI framework is allowed to live.
- The Database Is a Detail
- The Web Is a Detail
- REP: Reuse/Release Equivalence Principle (`component-principles.md`) — usable-before-reusable is the practical side of REP.
