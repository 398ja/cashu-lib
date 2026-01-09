# cashu-lib Agent Guide

cashu-lib is a Java 21 multi-module library that implements the core data structures, cryptographic primitives, and REST entities required by the Cashu protocol. This guide captures the repository conventions that agents must follow when extending or reviewing the project.

## Protocol References
- **NUT-00 – Token formats**: canonical JSON (`cashuA`) and CBOR (`cashuB`) token serialization rules.
- **NUT-02 – Keysets**: keyset identifiers, public key ordering, and mint key discovery.
- **NUT-09 – Restore**: payloads for `/restore` and related endpoints.
- **NUT-13 – Deterministic secrets**: mnemonic recovery, voucher counters, and keyset binding.
- The complete specification index (NUT-00 through NUT-24) lives in [cashubtc/nuts](https://github.com/cashubtc/nuts). Review the relevant document whenever behaviour intersects the protocol. Quick links:
  - [NUT-00](https://github.com/cashubtc/nuts/blob/main/00.md)
  - [NUT-01](https://github.com/cashubtc/nuts/blob/main/01.md)
  - [NUT-02](https://github.com/cashubtc/nuts/blob/main/02.md)
  - [NUT-03](https://github.com/cashubtc/nuts/blob/main/03.md)
  - [NUT-04](https://github.com/cashubtc/nuts/blob/main/04.md)
  - [NUT-05](https://github.com/cashubtc/nuts/blob/main/05.md)
  - [NUT-06](https://github.com/cashubtc/nuts/blob/main/06.md)
  - [NUT-07](https://github.com/cashubtc/nuts/blob/main/07.md)
  - [NUT-08](https://github.com/cashubtc/nuts/blob/main/08.md)
  - [NUT-09](https://github.com/cashubtc/nuts/blob/main/09.md)
  - [NUT-10](https://github.com/cashubtc/nuts/blob/main/10.md)
  - [NUT-11](https://github.com/cashubtc/nuts/blob/main/11.md)
  - [NUT-12](https://github.com/cashubtc/nuts/blob/main/12.md)
  - [NUT-13](https://github.com/cashubtc/nuts/blob/main/13.md)
  - [NUT-14](https://github.com/cashubtc/nuts/blob/main/14.md)
  - [NUT-15](https://github.com/cashubtc/nuts/blob/main/15.md)
  - [NUT-16](https://github.com/cashubtc/nuts/blob/main/16.md)
  - [NUT-17](https://github.com/cashubtc/nuts/blob/main/17.md)
  - [NUT-18](https://github.com/cashubtc/nuts/blob/main/18.md)
  - [NUT-19](https://github.com/cashubtc/nuts/blob/main/19.md)
  - [NUT-20](https://github.com/cashubtc/nuts/blob/main/20.md)
  - [NUT-21](https://github.com/cashubtc/nuts/blob/main/21.md)
  - [NUT-22](https://github.com/cashubtc/nuts/blob/main/22.md)
  - [NUT-23](https://github.com/cashubtc/nuts/blob/main/23.md)
  - [NUT-24](https://github.com/cashubtc/nuts/blob/main/24.md)
- Additional project plans and explorations are stored in the `project/` directory (for example `NUT13_CLI_INTEGRATION_GUIDE.md` and `NUT-13-IMPLEMENTATION-PLAN.md`).

## Repository Layout
- `pom.xml`: parent POM that pins dependency and plugin versions, aggregates modules, and configures JaCoCo aggregation.
- `cashu-lib-common/`: shared domain types, token codecs (`TokenV3`, `TokenV4`), keyset helpers, and JSON/CBOR utilities.
- `cashu-lib-crypto/`: cryptographic primitives (Schnorr, BDHKE) backed by Bouncy Castle and the `xyz.tcheeric.bips` helpers.
- `cashu-lib-entities/`: Jackson-annotated DTOs representing mint REST APIs (quotes, melts, swaps, restores).
- `docs/`: Diátaxis documentation (tutorials, how-to guides, reference, explanation). Keep `docs/README.md` in sync when new docs are added.
- `project/`: design notebooks, protocol analysis, and long-form planning material. Consult these when working on NUT-13 features or vouchers.
- `release-please-config.json` / `.release-please-manifest.json`: release automation inputs; do not hand-edit generated sections.
- `mvnw` / `mvnw.cmd`: wrapper scripts that ensure a consistent Maven environment.

## Tooling & Build
- Target **Java 21** (Temurin). The compiler settings live in the parent POM (`maven.compiler.release=21`).
- Use the Maven wrapper for all builds: `./mvnw -q verify` from the repository root runs unit tests, integration tests (if present), and aggregates JaCoCo coverage.
- Module-specific builds use `./mvnw -q -pl <module> -am verify`. The `-am` flag ensures dependencies are compiled.
- JaCoCo reports are aggregated under `target/site/jacoco-aggregate`. Uploads are handled in CI via Codecov.
- Dependencies are managed through the root `dependencyManagement` block. Add new versions there and import them from child modules.

## Coding

When writing code, follow Clean Code and Clean Architecture principles:

### Meaningful Names
- Use intention-revealing names that explain why something exists, what it does, and how it's used
- Avoid disinformation: don't use names that could mislead (e.g., `accountList` for something that isn't a List)
- Make meaningful distinctions: avoid noise words like `Info`, `Data`, `Manager` unless they add real meaning
- Use pronounceable and searchable names; avoid single-letter variables except for loop counters
- Class names should be nouns (`Customer`, `Account`); method names should be verbs (`postPayment`, `save`)
- Pick one word per concept and stick with it (`fetch`, `retrieve`, `get` — pick one)

### Functions
- Keep functions small: ideally under 20 lines, rarely exceeding one screen
- Functions should do one thing, do it well, and do it only
- One level of abstraction per function: don't mix high-level policy with low-level details
- Use descriptive names: a long descriptive name is better than a short cryptic one
- Minimize arguments: zero is ideal, one or two are fine, three requires justification
- Avoid flag arguments (boolean parameters that change behavior)
- Functions should either do something or answer something, never both (Command-Query Separation)
- Prefer exceptions over error codes; extract try/catch blocks into their own functions

### Comments
- Comments are a failure to express intent in code; prefer self-documenting code
- Good comments: legal comments, explanation of intent, clarification, warning of consequences, TODO notes, Javadoc for public APIs
- Bad comments: redundant comments, misleading comments, mandated comments, journal comments, noise comments, commented-out code
- If you must comment, explain *why*, not *what* — the code shows what

### Error Handling
- Use exceptions rather than return codes
- Write try-catch-finally statements first when writing code that could throw
- Use unchecked exceptions; checked exceptions violate the Open/Closed Principle
- Provide context with exceptions: include operation attempted and failure type
- Define exception classes by how they're caught, not by their source
- Don't return null — throw an exception or return a Special Case object instead
- Don't pass null as arguments unless the API explicitly expects it

### Classes
- Classes should be small, measured by responsibilities (Single Responsibility Principle)
- A class should have only one reason to change
- High cohesion: methods and variables should be closely related
- Organize for change: isolate code that's likely to change from code that's stable
- Depend on abstractions, not concretions (Dependency Inversion)
- Classes should be open for extension but closed for modification (Open/Closed Principle)

### Code Smells and Heuristics
- Avoid comments that could be replaced by better naming or structure
- Eliminate dead code, duplicate code, and code at wrong levels of abstraction
- Keep configuration data at high levels; don't bury magic numbers
- Follow the Law of Demeter: modules shouldn't know about the innards of objects they manipulate
- Make logical dependencies physical: if one module depends on another, make that explicit
- Prefer polymorphism to if/else or switch/case chains
- Follow standard conventions for the project and language
- Replace magic numbers with named constants
- Be precise: don't be lazy about decisions — if you decide to use a list, be sure you need one
- Encapsulate conditionals: extract complex boolean expressions into well-named methods
- Avoid negative conditionals: `if (buffer.shouldCompact())` is clearer than `if (!buffer.shouldNotCompact())`
- Functions should descend one level of abstraction

### SOLID Design Principles

**Single Responsibility Principle (SRP)**
A module should have one, and only one, reason to change. Each class serves one actor or stakeholder. When requirements change for one actor, only the relevant module changes.

**Open/Closed Principle (OCP)**
Software entities should be open for extension but closed for modification. Achieve this through abstraction and polymorphism — add new behavior by adding new code, not changing existing code.

**Liskov Substitution Principle (LSP)**
Subtypes must be substitutable for their base types without altering program correctness. If S is a subtype of T, objects of type T may be replaced with objects of type S without breaking the program.

**Interface Segregation Principle (ISP)**
Clients should not be forced to depend on interfaces they don't use. Prefer many small, client-specific interfaces over one general-purpose interface.

**Dependency Inversion Principle (DIP)**
High-level modules should not depend on low-level modules; both should depend on abstractions. Abstractions should not depend on details; details should depend on abstractions.

### Component Principles

**Cohesion Principles:**
- **REP (Reuse/Release Equivalence)**: The granule of reuse is the granule of release — classes in a component should be releasable together
- **CCP (Common Closure)**: Gather classes that change for the same reasons at the same times; separate classes that change at different times for different reasons
- **CRP (Common Reuse)**: Don't force users to depend on things they don't need — classes in a component should be used together

**Coupling Principles:**
- **ADP (Acyclic Dependencies)**: No cycles in the component dependency graph; use Dependency Inversion to break cycles
- **SDP (Stable Dependencies)**: Depend in the direction of stability — volatile components should depend on stable ones
- **SAP (Stable Abstractions)**: Stable components should be abstract; instability should be concrete

### Design Patterns
Apply established patterns where appropriate:
- **Creational**: Factory Method, Abstract Factory, Builder, Singleton (use sparingly), Prototype
- **Structural**: Adapter, Bridge, Composite, Decorator, Facade, Proxy
- **Behavioral**: Strategy, Observer, Command, State, Template Method, Iterator, Chain of Responsibility

Choose patterns that simplify the design; don't force patterns where simpler solutions suffice.

### General Guidelines
- When committing code, follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification
- When adding new features, ensure they are compliant with the Cashu specification (NUTs) provided above
- Make use of the Lombok library to reduce boilerplate code
- Always rely on imports rather than fully qualified class names in code to keep implementations readable

## Coding Guidelines

### General Style
- Keep packages under `xyz.tcheeric.cashu`. Do not introduce new root packages.
- Prefer Lombok to match the existing code style (`@Data`, `@Value`, `@Builder`, `@RequiredArgsConstructor`, `@NonNull`, `@Slf4j`). Lombok processors are already wired in the parent POM.
- Use descriptive validation messages and throw `IllegalArgumentException` for invalid constructor or setter input. Always state the invalid value.
- Avoid fully qualified class names inside code; rely on imports.
- Use `java.util` collections with deterministic iteration order when serializing (for example `LinkedHashSet` for proofs).
- Favour small, focused classes that mirror the Cashu entities. Prefer composition over inheritance unless matching the protocol structure.

### Serialization & Protocol Compliance
- JSON and CBOR serialization must use `JsonUtils.JSON_MAPPER` and `JsonUtils.CBOR_MAPPER` to inherit project-wide settings (module discovery, CBOR minimal integers, key ordering).
- Token serialization flows through `Token.TokenUtil.serialize`. New token versions or prefixes must extend the `Token` interface without duplicating encoding logic.
- Maintain deterministic ordering: proofs are sorted by amount, token maps respect key ordering (`t`, `d`, `m`, `u`), and mint proofs merge without losing insertion order.
- When parsing clickable URIs, strip the `cashu:` scheme but accept bare tokens as well. Ensure new parsing logic continues to reject malformed prefixes early.

### Module Notes
- **cashu-lib-common**
  - Contains `Proof`, `Secret`, `KeysetId`, and token codecs. Keep cryptographic operations out of this module (only data handling and simple helpers belong here).
  - `SecretFactory` centralises deterministic (NUT-13) secret generation. Extend it rather than duplicating mnemonic or counter handling logic.
  - When adding new `Secret` types, update the JSON deserializers under `common/json/deserializer`.
- **cashu-lib-entities**
  - DTOs map directly to mint API payloads. Annotate every field with `@JsonProperty` and supply `@NoArgsConstructor` + `@AllArgsConstructor` when used with Jackson.
  - Validate request DTOs in constructors and setters so consumers receive immediate feedback (for example counter > 0).
  - Add matching documentation in `docs/reference/` when you introduce new DTO groups.
- **cashu-lib-crypto**
  - Encapsulate cryptographic operations (Schnorr signing, point arithmetic, key derivation). Reuse the utility classes (`KeysUtils`, `Nut13Derivation`) for shared logic.
  - Do not keep mutable global state; prefer stateless utility methods or records for value objects.
  - Bouncy Castle (`bcprov-jdk18on`) is already a managed dependency; avoid shading additional crypto libraries unless absolutely required.

## Error Handling
- The library prioritises unchecked exceptions so downstream applications can decide how to react. Use `IllegalArgumentException` for validation failures and wrap checked exceptions (for example `IOException`) inside `RuntimeException` with context about the operation.
- Include what failed and the offending value in each message: `"Amount must be greater than 0. Got: " + amount`.
- When adding new public APIs, document thrown exceptions in Javadoc and keep messages stable for callers who rely on them.

## Logging
- Use Lombok’s `@Slf4j` and parameterised logging (`log.debug("Serializing TokenV3 containing {} mint proofs", mintProofs.size())`).
- Keep default logging at `debug` or `trace` levels for serialization traces; avoid `info`/`warn` unless signalling actionable library behaviour.
- Express messages as `component action outcome` with contextual key values when helpful (`token_v4 decode_failed mintUrl={}`).
- Never log secrets, private keys, or mnemonic phrases. Mask values where necessary.

## Testing
- The project uses **JUnit 5** (`org.junit.jupiter`) with **AssertJ** for fluent assertions. Import from the managed versions in the parent POM.
- Structure every test using the Arrange–Act–Assert pattern and add a short JavaDoc comment above each test that explains the behaviour under test.
- Name tests following `should[ExpectedBehavior]When[StateUnderTest]`.
- Prefer one logical assertion per test (multiple `assertThat` calls are acceptable when they validate the same behaviour).
- Test directories live inside each module (`src/test/java`). Create package mirrors of the production code (`xyz.tcheeric.cashu.*`).
- Property testing is optional; add `net.jqwik:jqwik` to the relevant module POM if a property-based test provides real value.
- Always run `./mvnw -q verify` before submitting changes. Capture and share the command output in PR descriptions.

## Documentation
- Follow the Diátaxis framework already in place. New docs belong in `docs/tutorials`, `docs/how-to`, `docs/reference`, or `docs/explanation`.
- Start each document with a `#` heading that states its purpose and keep examples minimal but runnable.
- Update `docs/README.md` with links to new documents under the appropriate section.
- When adding or modifying REST DTOs, extend the reference docs (for example `docs/reference/cashu-lib-entities.md`) and describe new fields or validation rules.

## Versioning & Release
- cashu-lib follows semantic versioning. Update the root `pom.xml` `<version>` along with the module POMs when preparing a release.
- Keep versions inside the parent `properties` block consistent. If you bump a dependency or plugin, change it in the parent and let modules inherit the new value.
- Release automation is managed by `release-please`. After merging to `main`, new tags are created from conventional commits. Do not edit `.release-please-manifest.json` manually.

## Changelog Maintenance
- **Always update `CHANGELOG.md`** after any version change or significant code modification.
- Follow the [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format:
  - Group changes under: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`
  - List versions in reverse chronological order (newest first)
  - Use `[Unreleased]` section for changes not yet in a release
  - Include the release date in ISO format: `## [1.0.0] - 2025-12-17`
- Each entry should be a concise, human-readable description of the change
- Reference related issues or PRs where applicable
- Update the changelog in the same commit as the version bump when possible

## Project Research Notes
- The `project/` directory captures ongoing protocol work (voucher compatibility, CLI integration, release phases). Review those documents when implementing NUT-13 or voucher features to stay aligned with the planned architecture.
- Many TODOs in the code reference these documents (for example the `TokenV3` merge TODO). Cross-link code changes with the corresponding plan where possible.

## Pre-Submit Checklist
- Code follows the module-specific guidelines above and keeps serialization deterministic.
- New DTOs or domain types include validation and (when necessary) JSON/CBOR deserializer updates.
- Tests cover happy path and edge conditions, include descriptive comments, and pass locally via `./mvnw -q verify`.
- Documentation and reference files are updated for new behaviour. Any new doc is linked from `docs/README.md`.
- Conventional commit rules in `commit_instructions.md` are respected, and release metadata remains consistent.
