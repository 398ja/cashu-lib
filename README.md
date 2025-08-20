[![CI](https://github.com/tcheeric/cashu-lib/actions/workflows/ci.yml/badge.svg)](https://github.com/tcheeric/cashu-lib/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/398ja/cashu-lib/graph/badge.svg?token=BV77LKDNGE)](https://codecov.io/gh/398ja/cashu-lib)
# cashu-lib

`cashu-lib` implements the core functionalities of the [Cashu](https://cashu.space/) protocol and provides the building blocks for [cashu-mint](https://github.com/tcheeric/cashu-mint) and [cashu-wallet](https://github.com/tcheeric/cashu-wallet).

## Modules
- `cashu-lib-common`: Common entity classes and utilities.
- `cashu-lib-crypto`: Foundational cryptographic functions and utilities.
- `cashu-lib-entities`: Core data structures of the Cashu protocol.
- `cashu-lib-test`: Unit test classes.

## Usage
Include the following dependencies in your project's `pom.xml`:

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-common</artifactId>
    <version>0.1.0</version>
</dependency>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-crypto</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Documentation
- [Installation and build guide](docs/tutorials/installation.md)
- [CI and release process](docs/how-to/ci-release.md)

## License
This project is licensed under the MIT License – see [LICENSE.md](LICENSE.md).

