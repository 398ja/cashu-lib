[![CI](https://github.com/tcheeric/cashu-lib/actions/workflows/ci.yml/badge.svg)](https://github.com/tcheeric/cashu-lib/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/398ja/cashu-lib/graph/badge.svg?token=BV77LKDNGE)](https://codecov.io/gh/398ja/cashu-lib)
# cashu-lib

For a quick start, see [docs/how-to/quickstart.md](docs/how-to/quickstart.md).

## Requirements

    $ java -version
```    
openjdk version "21.0.2" 2024-01-16
OpenJDK Runtime Environment (build 21.0.2+13-Ubuntu-123.10.1)
OpenJDK 64-Bit Server VM (build 21.0.2+13-Ubuntu-123.10.1, mixed mode, sharing)
```

    $ mvn -version
```
Apache Maven 3.8.7
Maven home: /usr/share/maven
Java version: 21.0.2, vendor: Private Build, runtime: /usr/lib/jvm/java-21-openjdk-amd64
Default locale: en_GB, platform encoding: UTF-8
OS name: "linux", version: "6.5.0-28-generic", arch: "amd64", family: "unix"
```

## Build and install cashu-lib

```
$ cd <your_git_home_dir>
$ git clone https://github.com/tcheeric/cashu-lib.git
$ cd cashu-lib
$ mvn clean install
```

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

