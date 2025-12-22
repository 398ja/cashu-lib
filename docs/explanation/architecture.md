# Architecture

This document outlines the overall architecture of `cashu-lib`. The library is organised as a multi-module Maven project that separates concerns across modules and promotes clear boundaries.

## Modules

- [`cashu-lib-common`](../../cashu-lib-common/): Shared utilities and base types. This module depends on `cashu-lib-crypto` for cryptographic primitives.
- [`cashu-lib-crypto`](../../cashu-lib-crypto/): Low-level cryptographic routines such as BIP-340 Schnorr helpers and key management.
- [`cashu-lib-entities`](../../cashu-lib-entities/): Domain objects for the Cashu protocol, including tokens, proofs, and keyset identifiers.

Module dependencies flow inward following Clean Architecture: higher-level modules rely only on modules closer to the core, keeping the domain logic independent of external concerns.

## Patterns

The project adopts Clean Architecture principles and commonly used design patterns:

- **Builders and immutability** for constructing domain objects in `cashu-lib-entities`.
- **Utility classes** for stateless helpers in `cashu-lib-common` and `cashu-lib-crypto`.
- **Separation of concerns** to keep cryptographic code isolated from protocol entities.

These choices make the library easier to maintain and extend while preserving a clear mental model for contributors.
