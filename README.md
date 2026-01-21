[![CI](https://github.com/398ja/cashu-lib/actions/workflows/ci.yml/badge.svg)](https://github.com/398ja/cashu-lib/actions/workflows/ci.yml)
[![Qodana](https://github.com/398ja/cashu-lib/actions/workflows/code_quality.yml/badge.svg)](https://github.com/398ja/cashu-lib/actions/workflows/code_quality.yml)
# cashu-lib

cashu-lib is a Java library implementing the Cashu protocol. It provides common entities, cryptographic primitives, and data structures for building Cashu mints and wallets.

## Documentation
See the [installation tutorial](docs/tutorials/installation.md) to build the project and the [quickstart tutorial](docs/tutorials/quickstart.md) for usage examples. Full docs follow the Diátaxis structure:

- [Tutorials](docs/tutorials/)
- [How-to Guides](docs/how-to/)
- [Reference](docs/reference/)
- [Explanation](docs/explanation/)

## Requirements & Build
- Java 21 (Temurin recommended)
- Build and test all modules with `./mvnw -q verify` (coverage: `target/site/jacoco-aggregate`)
- Build a specific module with dependencies via `./mvnw -q -pl <module> -am verify`

## Virtual Thread Compatibility
All cashu-lib modules are compatible with Java 21+ Virtual Threads (Project Loom):
- No `synchronized` blocks that could cause carrier thread pinning
- No `ThreadLocal` usage that could cause state inheritance issues
- Thread-safe crypto operations using per-call `MessageDigest` instances

See [Virtual Thread Compatibility](docs/explanation/virtual-thread-compatibility.md) for the full audit report and recommendations.

## Use in your project
Add the releases repository and depend on the modules you need (replace `0.12.0` with the latest tag):

```xml
<repositories>
    <repository>
        <id>cashu-lib</id>
        <url>https://maven.398ja.xyz/releases</url>
    </repository>
</repositories>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-common</artifactId>
    <version>0.12.0</version>
</dependency>
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-crypto</artifactId>
    <version>0.12.0</version>
</dependency>
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-entities</artifactId>
    <version>0.12.0</version>
</dependency>
```

## Modules
- `cashu-lib-common`: Token codecs (V3/V4), keysets, deterministic secrets (NUT-13), and JSON/CBOR utilities.
- `cashu-lib-crypto`: BIP-340 Schnorr helpers, BDHKE utilities, and key derivation primitives.
- `cashu-lib-entities`: REST DTOs for mint APIs (quotes, swaps, melts, restores) with Jackson annotations.

## Protocol alignment

- Keyset IDs follow the NUT-02 16-hex-character format; `PublicKey` preserves compressed SEC encoding for hashing/serialization.
- TokenV3: proofs are sorted by `amount` for deterministic JSON serialization; clickable `cashu:` URIs are accepted.
- TokenV4: CBOR maps are ordered `t`, `d`, `m`, `u` per NUT-00 and trailing slashes are removed from mint URLs; clickable URIs and URL-safe Base64 without padding are supported.
- Restore: `/restore` entities are provided for NUT-09; deterministic secret generation helpers follow the NUT-13 derivation path.

Token prefixes remain `cashuA` (V3, JSON) and `cashuB` (V4, CBOR). See the [NUTs](https://github.com/cashubtc/nuts) for the full protocol.

## Versioning
- Releases are published to `https://maven.398ja.xyz/releases`.
- Tags follow release-please: root tags use `vX.Y.Z`; component tags use `cashu-lib-common-vX.Y.Z`, `cashu-lib-crypto-vX.Y.Z`, and `cashu-lib-entities-vX.Y.Z`.
- Changelogs and version bumps are managed by `release-please` (`release-please-config.json`, `.release-please-manifest.json`).

## Contributing
Open tasks live in [GitHub Issues](https://github.com/398ja/cashu-lib/issues). Run `./mvnw -q verify` before sending a PR. See [docs/how-to/releasing.md](docs/how-to/releasing.md) for release steps.

## License
This project is licensed under the MIT License – see [LICENSE.md](LICENSE.md).
