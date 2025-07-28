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
- ```cashu-lib-gateway:``` Contains interfaces for payment gateways that interact with the cashu mints and wallets.
- ```cashu-lib-test:``` Contains unit test classes.

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

## Generating a test coverage report
Run the project's tests and produce an aggregated JaCoCo report with:

```bash
mvn verify
```

The HTML report will be generated at `cashu-lib-test/target/site/jacoco-aggregate/index.html`.

## Publishing to Maven Central
This project's POM is configured with distribution management and signing plugins for deployment to Sonatype OSSRH.
Ensure your `~/.m2/settings.xml` contains credentials for the `ossrh` server and run:

```bash
mvn clean deploy
```

After closing and releasing the staging repository in the Sonatype UI, the artifacts will appear on Maven Central.


## Todo
- Add more unit tests.

## License
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

## Disclaimer
This project is a work in progress and is not yet ready for production use. Use at your own risk.

