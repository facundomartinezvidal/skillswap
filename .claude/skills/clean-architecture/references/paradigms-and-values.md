# Design, values and programming paradigms

Chapters 1–6: what architecture is for, and the three paradigms it is built on.

## Contents

- [Design vs. Architecture](#design-vs-architecture)
- [Two Values of Software: Behavior vs. Architecture](#two-values-of-software-behavior-vs-architecture)
- [Structured Programming](#structured-programming)
- [Object-Oriented Programming](#object-oriented-programming)
- [Functional Programming](#functional-programming)

## Design vs. Architecture

> The goal of software architecture is to minimize the human resources required to build and maintain the required system.

*Chapter 1.*

### What it means in practice

Martin treats "design" and "architecture" as the same continuum of decisions — from the shape of a single function up to the boundaries between major subsystems — not two separate disciplines. Design quality isn't about aesthetics; it's measured by how much effort each new feature costs over the system's life. A well-designed system keeps that cost flat release after release; a badly designed one makes each release more expensive than the last, until adding anything becomes prohibitively slow.

"Move fast and clean up later" is a false trade: a mess slows a team down almost immediately, not just eventually, and rewriting from scratch just repeats the same overconfidence that produced the original mess.

### Example: the cost-per-line signature of a mess

A real company Martin cites tracked 8 releases: engineering headcount grew steadily while output (lines of code per release) flattened toward zero, and cost-per-line rose roughly 40x. That curve — more people producing less — is what "we'll clean it up later" actually looks like on a graph, not a one-time slowdown but a compounding one.

Separately, a controlled experiment (disciplined TDD vs. not, across 6 days) found the disciplined days ran *faster*, not slower — including on day one. Going well and going fast aren't in tension; a mess is what's actually slow.

### Related

- Two Values of Software: Behavior vs. Architecture
- Keep Options Open: Policy vs. Details (`drawing-boundaries.md`) — what architecture's shape is *for*: development, deployment, operation, maintenance.
- Architectural Boundaries and the Plugin Architecture (`drawing-boundaries.md`)
- Anticipating Boundaries: When to Pay for One (`drawing-boundaries.md`)

## Two Values of Software: Behavior vs. Architecture

> Every software system provides two different values to the stakeholders: behavior and structure.

*Chapter 2.*

### What it means in practice

**Behavior** is whether the software does what the spec says (features, bug fixes). **Architecture** is how easy the software is to change. Teams chronically over-invest in behavior because it's *urgent* — a stakeholder is waiting on it — while architecture, though the more *important* value long-term, never feels urgent on any given day, so it quietly loses every prioritization fight and the codebase stiffens release by release.

Martin maps this onto Eisenhower's urgent/important matrix: behavior is urgent-but-not-always-important, architecture is important-but-never-urgent. Business stakeholders are well positioned to judge urgency, but usually can't judge architectural importance — so it falls to the engineering team, as legitimate stakeholders themselves, to protect it. "The difficulty in making a change should be proportional only to the scope of the change, not to its shape" — i.e. a small feature request should cost a small amount of work, regardless of how the codebase happens to be organized today.

### Example: features that quietly erode architecture

A team under deadline pressure ships every sprint's features (urgent, satisfies stakeholders today) while skipping the refactors that would keep the codebase easy to extend (important, satisfies stakeholders over the next two years). Nobody decided to sacrifice architecture — it just always loses to whatever's due this week, until a simple feature starts requiring a week of archaeology before the first line of new code gets written.

### Related

- Design vs. Architecture
- OCP: Open-Closed Principle (`solid.md`) — the concrete practice for protecting architecture: structure dependencies so new behavior doesn't require touching stable code.
- Keep Options Open: Policy vs. Details (`drawing-boundaries.md`) — structure is the greater value because it keeps software soft; leaving options open is how you protect it.

## Structured Programming

> Structured programming imposes discipline on direct transfer of control.

*Chapters 3–4.*

### What it means in practice

Dijkstra found that unrestricted `goto` makes a program impossible to decompose into provably-correct units — you can't reason about a piece of it in isolation if control can jump in or out from anywhere. Restricting control flow to sequence, selection (`if`), and iteration (`while`/`for`) — proven sufficient by the Böhm–Jacopini theorem — makes a program decomposable into small units that can each be checked independently. That's the whole paradigm: it doesn't add a capability, it removes one (unrestricted jumps).

Dijkstra's original goal was mathematical proof of correctness; that never really worked in practice. What structured programming actually gives modern developers is **falsifiability**: "testing shows the presence, not the absence, of bugs" — you can never prove a program correct by testing, but restricting control flow to these three constructs is what makes a unit small and well-defined enough to be meaningfully testable at all. This is the direct ancestor of why every mainstream language still forbids (or strongly discourages) arbitrary jumps.

### Example: why every function you write is already "structured"

Any function built purely from `if`/`else`, `for`/`while`, and sequential statements — with no raw jumps — is structured programming in action, whether or not you think about it. The discipline is invisible today because virtually every language enforces it by construction (no `goto` at all, or a heavily restricted one); the paradigm won by becoming the default rather than a technique you opt into.

### Related

- Object-Oriented Programming
- Functional Programming
- Policy and Level (`drawing-boundaries.md`) — the "central transform" from structured design is the highest-level policy.

## Object-Oriented Programming

> OO is the ability, through the use of polymorphism, to gain absolute control over every source code dependency in the system.

*Chapters 3, 5.*

### What it means in practice

Martin rejects the standard "encapsulation + inheritance + polymorphism" definition of OO piece by piece: C already had strong encapsulation (a `.h`/`.c` split can hide a struct's internals completely — arguably *more* than C++, which had to expose member variables in the header for the compiler to compute an object's size); and C programmers could already fake inheritance by matching struct field order and casting pointers. The one genuine addition is **polymorphism made safe and convenient** — a vtable generated by the compiler instead of a hand-rolled table of function pointers (which C programs, e.g. the Unix `FILE` struct, already did manually).

The architectural payoff of that convenience is huge: because a caller can depend on an *interface* instead of a concrete type, the **source code dependency can point opposite to the flow of control** — this is dependency inversion. A high-level module (business rules) can call a low-level module (database, UI) through an interface the high-level module itself owns, so the low-level module depends on the high-level one, never the reverse. That's what makes independently deployable, independently developable components possible — and it's the mechanism Program To An Interface and Favor Composition Over Inheritance both rely on, and what Adapter and Strategy are concrete instances of.

### Example: inverting the arrow with an interface

```java
// Without polymorphism: high-level policy depends directly on a low-level detail
public class OrderService {
    private final StripeClient stripeClient = new StripeClient(); // dependency points toward Stripe
}
```

```java
// With polymorphism: the dependency is inverted
public interface PaymentGateway {       // owned by the high-level module
    void charge(BigDecimal amount);
}

public class OrderService {
    private final PaymentGateway gateway; // depends on its own interface, not Stripe

    public OrderService(PaymentGateway gateway) {
        this.gateway = gateway;
    }
}

public class StripeAdapter implements PaymentGateway { /* ... */ } // depends on OrderService's interface
```

`StripeAdapter` now depends on `OrderService`'s interface — the opposite of the call direction. `OrderService` never mentions Stripe.

### Related

- DIP: Dependency Inversion Principle (`solid.md`)
- Structured Programming
- Functional Programming
- The Dependency Rule (`layers-and-dependency-rule.md`) — polymorphism is what lets source dependencies oppose the flow of control across every boundary.
- Decoupling Modes and Boundary Anatomy (`drawing-boundaries.md`) — dynamic polymorphism is what makes in-process boundaries safe.

## Functional Programming

> Functional programming imposes discipline upon assignment — variables in functional languages do not vary.

*Chapters 3, 6.*

### What it means in practice

Why should an architect care about mutability? Because "all race conditions, deadlock conditions, and concurrent update problems are due to mutable variables. You cannot have a race condition or a concurrent update problem if no variable is ever updated." Immutability doesn't just avoid bugs — it removes an entire category of them by construction.

The practical architectural move: segregate a system into immutable (purely functional) components — which can run concurrently with zero coordination — and a smaller set of components that must allow mutation, protected by a controlled, transactional mechanism (compare-and-swap, an actor model, or similar), and pushed to the edges of the system. Push as much logic as possible into the immutable side.

A more radical, storage-is-cheap-now version of this is **event sourcing**: never mutate state at all — store only the sequence of transactions (deposits/withdrawals, not a balance), and recompute current state on demand by replaying them. "Our applications are not CRUD; they are just CR." No concurrent-update problem exists because nothing is ever updated or deleted.

### Example: mutable loop vs. no mutable state at all

```java
// Imperative: state lives in a mutable loop variable
List<Integer> squares = new ArrayList<>();
for (int i = 0; i < 25; i++) {
    squares.add(i * i);
}
```

```clojure
;; Functional: no variable is ever assigned twice
(take 25 (map (fn [x] (* x x)) (range)))
```

Neither snippet is wrong — the point is that the second one has no mutable state to race on, lock, or get corrupted by a concurrent write, by construction.

### Related

- Structured Programming
- Object-Oriented Programming
