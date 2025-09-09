[![CI](https://github.com/398ja/cashu-lib/actions/workflows/ci.yml/badge.svg)](https://github.com/398ja/cashu-lib/actions/workflows/ci.yml)
[![Qodana](https://github.com/398ja/cashu-lib/actions/workflows/code_quality.yml/badge.svg)](https://github.com/398ja/cashu-lib/actions/workflows/code_quality.yml)
[![codecov](https://codecov.io/gh/398ja/cashu-lib/graph/badge.svg?token=BV77LKDNGE)](https://codecov.io/gh/398ja/cashu-lib)
# cashu-lib

cashu-lib is a Java library implementing the Cashu protocol. It provides common entities, cryptographic primitives, and data structures for building Cashu mints and wallets.

## Documentation
See the [installation tutorial](docs/tutorials/installation.md) to build the project and the [quickstart guide](docs/how-to/quickstart.md) for usage examples.

- [Tutorials](docs/tutorials/)
- [How-to Guides](docs/how-to/)
- [Reference](docs/reference/)
- [Explanations](docs/explanations/)

## Modules
- `cashu-lib-common`: Common entity classes and utilities, including BIP-340 Schnorr signature helpers.
- `cashu-lib-crypto`: Foundational cryptographic functions and utilities.
- `cashu-lib-entities`: Core data structures of the Cashu protocol.

## Spec Compliance Notes

- NUT-02 Keyset ID: The keyset id equals `00` plus the first 14 hex characters of SHA-256 over the concatenation of the SEC-compressed public key encodings (33 bytes with 0x02/0x03 prefix), sorted by amount (key). This library preserves the compressed prefix for public keys constructed from strings and concatenates raw bytes (not ASCII hex) for hashing.

- NUT-00 TokenV3 Serialization: For stable cashu-encoded tokens, proofs are serialized in a deterministic order (by `amount`), and Schnorr signatures (`C`) are serialized as hex strings.

- NUT-00 TokenV4 Serialization: TokenV4 tokens serialize deterministically and are validated against [official single- and multi-keyset test vectors](https://github.com/cashubtc/nuts/blob/main/tests/00-tests.md).

- NUT-09 Restore Signatures: Includes REST entities for the `/restore` endpoint to recover blind signatures.

## Token Formats

- Prefixes: Tokens use `cashuA` for V3 (JSON) and `cashuB` for V4 (CBOR), per NUT-00.
- Clickable URIs: Both V3 and V4 deserializers accept clickable URIs with the scheme `cashu:` (e.g., `cashu:cashuB...`) and plain tokens without the scheme (e.g., `cashuB...`).
- Encoding: Serialization uses URL-safe Base64 without padding.
- TokenV4 order: Top-level CBOR map preserves the NUT-00 key order `t` (token data), `d` (memo), `m` (mint URL), `u` (unit) for deterministic output.
- TokenV3 order: Proofs are serialized in a deterministic order (by `amount`) to ensure stable token strings.
- References: See NUT-00 and related NUTs at https://github.com/cashubtc/nuts.

## Versioning

- Root tags: Releases for the repository root are tagged as `vX.Y.Z`.
- Module tags: Module releases are tagged as `cashu-lib-common-vX.Y.Z`, `cashu-lib-crypto-vX.Y.Z`, and `cashu-lib-entities-vX.Y.Z`.
- Snapshot policy: Active development on `develop` uses `-SNAPSHOT` versions in POMs (for example, `0.1.2-SNAPSHOT`) while the manifest tracks the last released versions (for example, `0.1.1`).
- Release automation: Versions and changelogs are managed by release-please (`release-please-config.json`, `.release-please-manifest.json`) and are cut after CI passes on `main`.

## Contributing
Outstanding work and planned enhancements are tracked in [GitHub Issues](https://github.com/tcheeric/cashu-lib/issues). See [docs/how-to/releasing.md](docs/how-to/releasing.md) for steps to publish a release.

## License
This project is licensed under the MIT License – see [LICENSE.md](LICENSE.md).
