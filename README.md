# cashu-lib
Cashu-Lib implements the core functionalities of the [Cashu](https://cashu.space/) protocol, and provides the building blocks specified in [NUT-00](https://github.com/cashubtc/nuts/blob/main/00.md) for by mints and wallets applications.

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
- ```cashu-lib-crypto:``` Contains cryptographic functions and utilities used by other modules.
- ```cashu-lib-gateway:``` Contains interfaces for gateways to interact with the Cashu protocol.
- ```cashu-lib-gateway-mock:``` Contains a mock gateway implementation for testing purpose only.
- ```cashu-lib-util:``` Contains utility classes and functions used by other modules.
- ```cashu-lib-test:``` Contains unit test classes.

## Usage
Include the following dependencies in your project's pom.xml file:

```xml
<dependency>
    <groupId>cashu-lib</groupId>
    <artifactId>cashu-lib-common</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>

<dependency><dependency>
    <groupId>cashu-lib</groupId>
    <artifactId>cashu-lib-crypto</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>

<groupId>cashu-lib</groupId>
    <artifactId>cashu-lib-gateway-mock</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>

```

## Todo
- Add more unit tests.
- Configure JaCoCo for code coverage.

## License
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

## Disclaimer
This project is a work in progress and is not yet ready for production use. Use at your own risk.

