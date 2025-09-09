[![CI](https://github.com/tcheeric/cashu-lib/actions/workflows/ci.yml/badge.svg)](https://github.com/tcheeric/cashu-lib/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/398ja/cashu-lib/graph/badge.svg?token=BV77LKDNGE)](https://codecov.io/gh/398ja/cashu-lib)
[![CI](https://github.com/398ja/cashu-lib/actions/workflows/ci.yml/badge.svg)](https://github.com/398ja/cashu-lib/actions/workflows/ci.yml)
[![Qodana](https://github.com/398ja/cashu-lib/actions/workflows/code_quality.yml/badge.svg)](https://github.com/398ja/cashu-lib/actions/workflows/code_quality.yml)
# cashu-lib

For a quick start, see [docs/how-to/quickstart.md](docs/how-to/quickstart.md).

## Requirements

Requires Java 21 and Maven 3.8+.

## Build and install cashu-lib

```
$ cd <your_git_home_dir>
$ git clone https://github.com/tcheeric/cashu-lib.git
$ cd cashu-lib
$ mvn clean install
```

## Modules
- `cashu-lib-common`: Common entity classes and utilities, including BIP-340 Schnorr signature helpers.
- `cashu-lib-crypto`: Foundational cryptographic functions and utilities.
- `cashu-lib-entities`: Core data structures of the Cashu protocol.

## Usage
Include the following dependencies in your project's `pom.xml`:

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-common</artifactId>
    <version>[VERSION]</version>
</dependency>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-crypto</artifactId>
    <version>[VERSION]</version>
</dependency>

```

### Deserialize Tokens (clickable and non-clickable)

```java
import xyz.tcheeric.cashu.common.TokenV3;
import xyz.tcheeric.cashu.common.TokenV4;

// V3 (JSON) non-clickable
TokenV3<?> v3a = TokenV3.deserialize("cashuA...");

// V3 clickable (cashu:cashuA...)
TokenV3<?> v3b = TokenV3.deserialize("cashu:cashuA...");

// V4 (CBOR) non-clickable
TokenV4 v4a = TokenV4.deserialize("cashuB...");

// V4 clickable (cashu:cashuB...)
TokenV4 v4b = TokenV4.deserialize("cashu:cashuB...");
```

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
Outstanding work and planned enhancements are tracked in
[GitHub Issues](https://github.com/tcheeric/cashu-lib/issues). See
[docs/how-to/releasing.md](docs/how-to/releasing.md) for steps to publish a
release.

## License
This project is licensed under the MIT License – see [LICENSE.md](LICENSE.md).
