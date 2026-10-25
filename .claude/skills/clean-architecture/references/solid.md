# SOLID principles

Chapters 7–11: the module-level design principles.

## Contents

- [SRP: Single Responsibility Principle](#srp-single-responsibility-principle)
- [OCP: Open-Closed Principle](#ocp-open-closed-principle)
- [LSP: Liskov Substitution Principle](#lsp-liskov-substitution-principle)
- [ISP: Interface Segregation Principle](#isp-interface-segregation-principle)
- [DIP: Dependency Inversion Principle](#dip-dependency-inversion-principle)

## SRP: Single Responsibility Principle

> A module should be responsible to one, and only one, actor.

*Chapter 7.*

### What it means in practice

SRP is widely misquoted as "a module should do one thing" — that's a different, function-level principle. The real SRP is about *organizational* coupling: a module should answer to exactly one **actor** (a stakeholder or group of stakeholders who'd request the same kind of change). "Gather together those things that change at the same times and for the same reasons; separate those things that change at different times or for different reasons."

When code serving two different actors is crammed into one class, a change requested by actor A can accidentally break behavior actor B depends on — because both changes touch the same file, get merged by the same team, and share state neither actor intended to share. The fix is splitting by actor, even if it means more, smaller classes.

### Example: a class three stakeholders depend on

```java
// Violates SRP: HR, payroll, and DBAs all have a reason to change this class
public class Employee {
    public Money calculatePay() { /* payroll's concern */ }
    public Report reportHours() { /* HR's concern */ }
    public void save() { /* DBA's concern */ }
}
```

```java
// Split by actor — each class now has exactly one reason to change
public class PayCalculator {
    public Money calculatePay(EmployeeData data) { /* ... */ }
}

public class HoursReporter {
    public Report reportHours(EmployeeData data) { /* ... */ }
}

public class EmployeeRepository {
    public void save(EmployeeData data) { /* ... */ }
}
```

A change to how hours get reported no longer risks breaking how pay gets calculated — they're not even in the same file anymore, and different teams can own each one independently.

### Related

- CCP: Common Closure Principle (`component-principles.md`) — SRP restated at the component level: group classes that change for the same reason into the same component.
- Architectural Boundaries and the Plugin Architecture (`drawing-boundaries.md`) — SRP tells you where to draw boundaries: along axes of change.
- Decoupling Layers and Use Cases (`drawing-boundaries.md`) — actors as the vertical axis of a whole system.
- True vs. Accidental Duplication (`layers-and-dependency-rule.md`) — the SRP question (same actor, same reason?) decides whether duplication is real.

## OCP: Open-Closed Principle

> A software artifact should be open for extension but closed for modification.

*Chapter 8.*

### What it means in practice

At the architecture level this isn't a small class-design tweak — Martin calls it "the most fundamental reason we study software architecture." A good design lets new requirements be met by **adding** code, not editing existing, already-tested code. That requires two things working together: separating things that change for different reasons (SRP), and then directing the dependencies between those pieces (via interfaces) so that important, stable components never depend on volatile, low-level ones. "If component A should be protected from changes in component B, then component B should depend on component A" — not the other way around.

This produces a hierarchy of "levels": the most protected component (business rules) sits at the top, shielded by interfaces from everything underneath it (UI, database, frameworks), which are free to change without ever forcing a change in the business rules.

### Example: adding a payment method without touching checkout

```java
public interface PaymentStrategy {
    void pay(BigDecimal amount);
}

public class Checkout {
    private final PaymentStrategy paymentStrategy;
    public Checkout(PaymentStrategy paymentStrategy) { this.paymentStrategy = paymentStrategy; }
    public void completeOrder(BigDecimal amount) { paymentStrategy.pay(amount); }
}
```

Adding cryptocurrency payments later means writing `CryptoPayment implements PaymentStrategy` — `Checkout` is never opened, recompiled, or redeployed. It's open for extension (new `PaymentStrategy` implementations) and closed for modification (nothing about `Checkout` changes). See Strategy Pattern for the full walkthrough — Strategy is OCP applied directly.

### Related

- SRP: Single Responsibility Principle
- DIP: Dependency Inversion Principle — the dependency-direction mechanism that makes OCP possible.
- Two Values of Software: Behavior vs. Architecture (`paradigms-and-values.md`) — OCP is the concrete practice for protecting the "architecture" value.
- SAP: Stable Abstractions Principle (`component-principles.md`) — OCP is why a stable component can still be flexible: make it abstract.
- Keep Options Open: Policy vs. Details (`drawing-boundaries.md`) — device independence was OCP before it had a name.
- Services Are Not an Architecture (`details.md`) — new features as new components, not edits to existing services.

## LSP: Liskov Substitution Principle

> If for each object o1 of type S there is an object o2 of type T such that for all programs P defined in terms of T, the behavior of P is unchanged when o1 is substituted for o2, then S is a subtype of T.

*Chapter 9. (Barbara Liskov, 1988.)*

### What it means in practice

Plainly: if code is written against an interface/supertype, any implementation of it must be swappable in without the calling code noticing a behavior difference. This started as guidance about inheritance (the textbook illustration is a `Square` that "is-a" `Rectangle` but breaks any code that sets width and height independently), but Martin extends it to any interface/implementation relationship — Java interfaces, REST APIs sharing a contract, plugins.

Violations don't stay contained: when one implementation doesn't honor the contract clients assume, calling code fills up with special-case `if`s to route around it, and that special-casing tends to leak into the architecture as extra mechanisms (config tables, feature flags) just to keep incompatible implementations from breaking each other.

### Example: an "add" that lies about its own interface

```java
List<String> names = getNames(); // could be an ArrayList — or an unmodifiable view
names.add("New Name");
```

If `getNames()` sometimes returns `Collections.unmodifiableList(...)`, this throws `UnsupportedOperationException` at runtime — even though the code only depends on `List`, per Program To An Interface. `List` promises `add()` works; an unmodifiable implementation silently breaks that promise. Any code written generically against `List` is an LSP violation waiting to happen the moment it meets that implementation.

### Related

- ISP: Interface Segregation Principle

## ISP: Interface Segregation Principle

> Clients should not be forced to depend on methods they do not use.

*Chapter 10.*

### What it means in practice

A fat interface with unrelated methods forces every caller to depend on the whole thing, even the parts it never calls — "depending on something that carries baggage you don't need can cause you troubles you didn't expect." In statically typed languages this is sharpest: a change to any method's signature forces every dependent to recompile and redeploy, even callers that never touch that method. Martin generalizes this past interfaces to architecture: depending on a framework that itself drags in something unrelated (say, a specific database library) means unrelated failures in that dependency can still ripple up into your system.

### Example: a fat gateway interface

```java
// Violates ISP: Checkout only calls charge(), but depends on all four methods
public interface PaymentGateway {
    void charge(BigDecimal amount);
    void refund(String transactionId);
    Report generateComplianceReport();
    void syncWithAccounting();
}
```

```java
// Segregated: each client depends only on what it actually uses
public interface Payments {
    void charge(BigDecimal amount);
    void refund(String transactionId);
}

public interface PaymentReporting {
    Report generateComplianceReport();
    void syncWithAccounting();
}

public class StripeAdapter implements Payments, PaymentReporting { /* ... */ }
```

`Checkout` now depends only on `Payments`. A signature change in `generateComplianceReport()` no longer forces `Checkout` to recompile or redeploy — it was never using that method anyway.

### Related

- LSP: Liskov Substitution Principle
- CRP: Common Reuse Principle (`component-principles.md`) — the same idea, one level up: don't depend on a *component* that bundles things you don't need.
- The Dependency Rule (`layers-and-dependency-rule.md`)

## DIP: Dependency Inversion Principle

> The most flexible systems are those in which source code dependencies refer only to abstractions, not to concretions.

*Chapter 11.*

### What it means in practice

This is the SOLID, formally-named version of Program To An Interface, Not An Implementation — same idea, one book calling it a naming convention, the other calling it an architectural law. DIP isn't absolute: depending on a genuinely stable concretion (`java.lang.String`) is harmless. The danger is depending on a **volatile** concrete class — one you're actively changing. Four rules keep that in check: don't refer to, derive from, or override volatile concrete classes, and don't name anything concrete-and-volatile if you can avoid it.

The one problem this creates: *something* has to call `new` on the real implementation eventually. DIP's answer is an **Abstract Factory** — a factory interface that creates the abstraction, implemented by a concrete factory that's the only place in the system allowed to mention the volatile concrete class. Most systems push every DIP violation like this into one place (often called `main`), so the rest of the codebase never mentions a concrete type at all.

### Example: the same idea as Program To An Interface, plus construction

```java
public interface PaymentGateway { void charge(BigDecimal amount); }

public interface PaymentGatewayFactory {
    PaymentGateway create();
}

public class StripeGatewayFactory implements PaymentGatewayFactory {
    public PaymentGateway create() {
        return new StripeAdapter(new StripeClient()); // the only place that names StripeAdapter
    }
}
```

`OrderService` depends on `PaymentGateway` and `PaymentGatewayFactory` — both abstractions. Nothing outside `StripeGatewayFactory` ever names `StripeAdapter` or `StripeClient`.

### Related

- Object-Oriented Programming (`paradigms-and-values.md`) — polymorphism is the language mechanism that makes dependency inversion possible at all.
- OCP: Open-Closed Principle
- SAP: Stable Abstractions Principle (`component-principles.md`) — SDP + SAP are DIP restated for components.
- ADP: Acyclic Dependencies Principle (`component-principles.md`) — DIP is one of the two ways to break a dependency cycle.
- The Dependency Rule (`layers-and-dependency-rule.md`) — DIP is the mechanism that lets dependencies point inward while control flows outward.
- The Main Component (`layers-and-dependency-rule.md`) — the "one place" where every DIP violation is pushed.
- Architectural Boundaries and the Plugin Architecture (`drawing-boundaries.md`)
