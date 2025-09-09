# Key Design Decisions

This document records major design choices made in `cashu-lib` and the rationale behind them.

## Modular Separation

The project is split into focused modules to keep responsibilities clear:

- [`cashu-lib-common`](../../cashu-lib-common/) centralises shared utilities and JSON/CBOR helpers.
- [`cashu-lib-crypto`](../../cashu-lib-crypto/) isolates cryptographic code, allowing it to evolve independently.
- [`cashu-lib-entities`](../../cashu-lib-entities/) contains immutable protocol entities that model Cashu concepts.

This separation reduces coupling and enables consumers to depend only on the modules they need.

## Serialization Strategy

Jackson is used for both JSON and CBOR formats. Token serialization is deterministic so that the same data structure always yields the same string. This stability is required for interoperability and aligns with NUT-00.

## Lombok for Boilerplate Reduction

The library uses Lombok to keep entity classes concise while retaining immutability and builder patterns. This keeps the domain model readable and easier to audit.

## Clean Architecture Practices

Dependencies point toward the domain: outer modules reference inner ones but not vice versa. This decision keeps core protocol logic free from framework or infrastructure concerns.
