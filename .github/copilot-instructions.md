## Copilot Instructions – Hytale mods

### General mindset

* Always think **mod Hytale long-term maintainable**, not quick hacks.
* Prioritize **Clean Architecture**, **Clean Code**, and **SOLID** over speed.
* Assume the plugin is:

    * Modular
    * Extensible
    * Actively maintained
    * Tested
* Favor explicit design over “magic” behavior.

---

## Architecture & project structure

### Mandatory architecture rules

* Enforce **Clean Architecture**:

    * **Domain**

        * Pure Java
        * No Hytale dependencies
        * Contains entities, value objects, domain events, domain rules
    * **Application (Use cases / Services)**

        * Orchestrates domain logic
        * No direct Hytale API usage
        * Depends only on domain abstractions
    * **Infrastructure**

        * Hytale implementations
        * Repositories, persistence, schedulers, listeners
    * **Interface / Adapter layer**

        * Commands, listeners, GUIs, packet handlers
* The **Hytale API is an implementation detail**, never a core dependency.

### Dependency rules

* Domain → nothing
* Application → domain only
* Infrastructure → application + domain
* Interface → application + domain

No exceptions.

---

## Data & persistence

* Domain models must be **persistence-agnostic**.
* Repositories:

    * Defined as interfaces in the application layer
    * Implemented in infrastructure
* Avoid:
    * Serializing live server objects directly
* Favor:

    * Identifiers (UUID)
    * Snapshots / DTOs
* Configuration files:

    * Versioned
    * Validated on load
    * Fail fast with clear errors

---

## Clean Code rules (strict)

### Classes & methods

* One responsibility per class.
* Methods:

    * Prefer under **30 lines**
    * Do one thing
    * Early returns over nested conditions
* Avoid:

    * God services
    * Utility classes full of statics
    * Boolean flags that change behavior
* Prefer:

    * Small focused interfaces
    * Composition over inheritance

### Naming

* Names must reflect **intent**, not implementation.
* Avoid abbreviations unless universally known.
* Commands and services should be named after **use cases**, not technical actions.

---

## Exception & error handling

* Never swallow exceptions.
* Never print stack traces directly.
* Always:

    * Log with context
    * Use domain-specific exceptions for business rules
* Translate exceptions between layers:

    * Domain → application
    * Application → interface (messages / feedback)

---

## Performance & Minecraft constraints

* Always consider:

    * Tick cost
    * Player count scalability
* Avoid:

    * Excessive allocations in tick-based logic
    * Repeated scans of large collections
    * Recomputing values every tick if they can be cached
* Justify any micro-optimizations with comments.

---

## Security & robustness

* Never trust:

    * Player input
    * Command arguments
    * Client-side data
* Validate all external inputs.
* Never expose:

    * Internal state
    * Sensitive data in logs or messages
* Ensure permission checks are centralized and consistent.

---

## Modern Java usage

* Use:

    * Streams when they improve clarity
    * `Optional` for return values only
* Avoid:

    * Complex stream chains
    * Streams in hot paths if readability or performance suffers
* Favor immutability where possible.

---

## Testing & extensibility

* Code must be **testable without a Minecraft server**:

    * No hard dependency on static Bukkit access
    * Use abstractions for server interactions
* Encourage:

    * Unit tests for domain & use cases
    * Integration tests for infrastructure
* Design with future extensions in mind (new roles, mechanics, systems).

---

## When generating or refactoring code (not review)

* Always propose:

    * A clean package structure
    * Clear responsibilities per class
* Explain **why** a design choice is made when non-trivial.
* Prefer fewer, well-designed classes over many weak ones.
* Never generate monolithic listener/service classes.
