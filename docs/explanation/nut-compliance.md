# Relation to NUT Specifications

This document explains how `cashu-lib` maps its implementation to the Cashu NUT specifications.

## Token Serialization (NUT-00)

[`cashu-lib-entities`](../../cashu-lib-entities/) serializes and deserializes tokens deterministically. Proofs are ordered by amount and signatures are encoded as hex strings, matching the rules in [NUT-00](https://github.com/cashubtc/nuts/blob/main/00.md).

## Keyset Identification (NUT-02)

Keyset identifiers are derived from compressed public keys sorted by amount and hashed with SHA-256. This logic lives in [`cashu-lib-common`](../../cashu-lib-common/) and follows [NUT-02](https://github.com/cashubtc/nuts/blob/main/02.md).

## Cryptographic Primitives

[`cashu-lib-crypto`](../../cashu-lib-crypto/) provides BIP-340 Schnorr signature helpers used across the project. These primitives satisfy the cryptographic requirements outlined in the early NUTs.

## Restore Signatures (NUT-09)

Support for the `/restore` endpoint entities is included so that clients can recover blind signatures as described in [NUT-09](https://github.com/cashubtc/nuts/blob/main/09.md).

The library aims to track new NUTs as they are published, keeping the implementation aligned with the evolving protocol.
