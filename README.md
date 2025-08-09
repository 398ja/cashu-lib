[![CI](https://github.com/tcheeric/cashu-lib/actions/workflows/ci.yml/badge.svg)](https://github.com/tcheeric/cashu-lib/actions/workflows/ci.yml)

# cashu-lib
```cashu-Lib``` implements the core functionalities of the [Cashu](https://cashu.space/) protocol, and provides the building blocks for [cashu-mint](https://github.com/tcheeric/cashu-mint) and [cashu-wallet](https://github.com/tcheeric/cashu-wallet).

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
- ```cashu-lib-common:``` Contains common entity classes and utilities used by other modules.
- ```cashu-lib-crypto:``` Contains the foundational cryptographic functions and utilities used by other modules.
- ```cashu-lib-entities:``` Contains entity classes representing the core data structures of the Cashu protocol.
- ```cashu-lib-test:``` Contains unit test classes.

## Version Management
All dependency and plugin versions are declared in the parent POM's `<properties>` section, providing a single source of truth for version numbers. Update the values there when upgrading dependencies.

## Usage
Include the following dependencies in your project's pom.xml file:

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

## CI, Coverage, and Releases
The project uses a GitHub Actions workflow defined in
[`ci.yml`](.github/workflows/ci.yml) that runs on pushes and pull requests to
`main`. The workflow sets up JDK&nbsp;21, caches Maven dependencies, and executes
`mvn -B verify` to build all modules, run the tests, and aggregate code coverage
via JaCoCo. The resulting HTML report is stored as a workflow artifact and is
also generated at `cashu-lib-test/target/site/jacoco-aggregate/index.html`.

To reproduce the CI build locally and generate the same coverage report, run:

```bash
mvn -q verify
```

### Release process
Artifacts are deployed to Sonatype OSSRH and synchronized to Maven Central.
Releasing requires credentials and signing keys to be configured in the
environment:

- `OSSRH_USERNAME` and `OSSRH_PASSWORD`
- `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE`

Contact the project maintainers to obtain these secrets. Once configured (for
example via GitHub secrets or your local `~/.m2/settings.xml`), publish a
release with:

```bash
mvn -q deploy -Dgpg.keyname=<YOUR_GPG_KEY_ID>
```

## Todo
- Add more unit tests.

## License
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

## Disclaimer
This project is a work in progress and is not yet ready for production use. Use at your own risk.

