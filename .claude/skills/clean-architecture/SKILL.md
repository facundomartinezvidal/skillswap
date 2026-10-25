---
name: clean-architecture
description: Reviews, designs and explains software structure using Robert C. Martin's Clean Architecture — the Dependency Rule, entities and use cases, ports and adapters (hexagonal), SOLID at the module level, component cohesion and coupling (REP/CCP/CRP, ADP/SDP/SAP, instability and abstractness metrics), boundaries, Humble Object, Main as composition root, and packaging. Use it whenever someone asks where code should live, how to structure or layer a project, whether their framework, ORM or database is leaking into business logic, how to split a monolith or draw service boundaries, how to make code testable without the DB or UI, to review a PR or repo for architecture or dependency problems, or what a SOLID or component principle really means — even if they never say "clean architecture". Also covers Spanish requests like "cómo estructuro el proyecto", "revisá la arquitectura", "dónde va esta lógica", "capas", "casos de uso", "puertos y adaptadores", "acoplamiento".
---

# Clean Architecture

This skill carries a personal, chapter-by-chapter study of *Clean Architecture* (Robert C. Martin) — paradigms, SOLID, component principles, architecture and "details" — as reference files with worked examples. Use it to **review** existing code or designs, **design** new structures, or **explain** concepts. The value it adds over general knowledge is precision: the book's actual definitions (which differ from the popular ones), its trade-offs, and its insistence that every boundary must pay for itself.

## The core model

Keep these in mind on every request; the reference files hold the detail.

- **The goal is cheap change.** Architecture exists to minimize the people-cost of building and maintaining a system. Judge every structural choice by *which future change it makes cheaper or more expensive*. This is also the language to use with the user: "this makes swapping the payment provider a one-file change" lands better than "this violates DIP".
- **Policy vs. details.** Business rules are where the value lives; the database, web, frameworks, devices and delivery mechanism are details. Keep policy ignorant of details so those decisions can be deferred or reversed.
- **The Dependency Rule.** Source-code dependencies point only inward, toward higher-level policy (level = distance from inputs and outputs). When control must flow outward, the inner layer owns an interface and the outer one implements it (DIP).
- **Data crossing a boundary is plain and shaped for the inner side.** Request/response records in, view models out. Never pass Entities outward, never pass ORM rows or framework request objects inward.
- **Actors are the axis of change.** SRP means "answerable to one actor", not "does one thing". Boundaries go where two sides change for different reasons at different rates. Code that looks duplicated but serves different actors is *accidental* duplication — leave it.
- **Boundaries cost money.** A full boundary (interfaces both ways, DTOs, separate deployment) is expensive; partial boundaries and no boundary are legitimate choices. Implement a boundary at the point where ignoring it starts costing more than building it, and say explicitly which ones are being deferred.
- **Main is the dirtiest component.** All concrete wiring (and any DI framework) lives in a composition root at the edge, so the rest of the code never names a concrete class.

## Choosing the mode

Read the request and pick one; mixed requests (e.g. "review this and tell me how to restructure it") do review first, then design.

### Review — the user shows code, a repo, a PR, or a diagram

First understand what is there before judging it: identify the components or packages, what each one is for (which actor), and which way the dependencies actually point — read the imports, constructor parameters, annotations and return types, since that is where violations hide. A use case that returns an ORM entity or accepts an `HttpServletRequest` looks innocent in a quick skim.

Then report findings, most expensive first. For each:

- **What and where** — the concrete dependency or construct, with `file:line` when there is code.
- **Principle** — the one that names it (Dependency Rule, SRP, SDP, …), in a few words.
- **Cost** — the specific change this makes expensive or risky ("switching from Mongoose to Prisma means editing every use case").
- **Fix** — concrete: the interface to introduce and *which layer owns it*, the DTO to add, what moves where. Show code for the important ones, in the user's language and framework.

Close with what is fine as it is, and any boundary you would deliberately *not* add yet and why. Reviews that flag everything push teams into over-engineering, which the book treats as a failure just as real as under-engineering. If the codebase is small or early, say so and scale the advice to it.

### Design — the user is starting something or restructuring

Work from the business inward-out, not from the stack:

1. **Actors and use cases** — who asks for changes, and the operations the system performs for them.
2. **Entities** — the critical business rules and data that would exist even without software.
3. **Ports** — the interfaces the use cases need (gateways, presenters/output boundaries, external services), owned by the inner layers.
4. **Adapters and details** — web, persistence, messaging, third parties; and which decisions (database, framework, services vs. monolith) can stay open for now.
5. **Packaging** — the folder/module layout, choosing between by-layer, by-feature, ports-and-adapters and by-component with the trade-off stated, and how the compiler or module system will enforce it (visibility, separate modules). An unenforced layout decays.
6. **Main** — where wiring happens, and how tests get a fake-backed composition.

Include a dependency diagram (mermaid or ASCII) where every arrow crossing a boundary points inward. Name the boundaries that are full, partial, or deliberately absent. The top-level structure should scream the domain, not the framework.

### Explain — the user asks what something means

Give the book's definition (quote it when the reference has the quote), what it means in practice, one example — in the user's language and stack if known — and how it connects to neighbouring principles. Correct the popular misreadings head-on, because they are usually why the question is being asked: SRP is about actors, not "doing one thing"; "stable" means hard to change (many dependents), not rarely changed; the data model matters but the database is a detail; microservices are not an architecture by themselves; Martin's Entity is not a JPA `@Entity` or a DDD entity.

## Reference files

Read the one that matches the topic before answering in depth; each has a table of contents, so jump to the section you need. Most reviews only need `layers-and-dependency-rule.md`; designs usually add `drawing-boundaries.md` and `packaging.md`.

| File | Read when the topic is… |
|---|---|
| `references/paradigms-and-values.md` | why architecture matters, behavior vs. structure, structured/OO/functional programming and what each gives architecture |
| `references/solid.md` | SRP, OCP, LSP, ISP, DIP — class/module-level design |
| `references/component-principles.md` | what belongs in a package/module/library, dependency cycles between modules, stability, instability (I), abstractness (A), distance from the main sequence (D) |
| `references/layers-and-dependency-rule.md` | where code goes: the circles, Dependency Rule, entities vs. use cases, request/response models, Humble Object (presenters, gateways), Main / composition root, accidental duplication |
| `references/drawing-boundaries.md` | whether and how strongly to separate: policy vs. details, level, vertical slices by use case, where boundaries go, decoupling modes (monolith → services), partial boundaries, when a boundary is worth paying for |
| `references/details.md` | microservices, testing strategy and fragile tests, embedded/hardware, database, web/UI, frameworks and ORMs |
| `references/packaging.md` | folder/package structure, screaming architecture, by-layer vs. by-feature vs. ports-and-adapters vs. by-component, enforcing it |

The examples in the references are mostly Java; translate them to the user's language and framework rather than pasting Java at a TypeScript or Python user.
