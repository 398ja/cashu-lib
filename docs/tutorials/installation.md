# Installation

cashu-lib targets Java 21 and ships with the Maven Wrapper, so you only need a JDK installed.

## Requirements

Verify your environment:

```bash
$ java -version
$ ./mvnw -v
```

## Build and verify cashu-lib

Clone the repository and run the full build with the wrapper:

```bash
$ git clone https://github.com/398ja/cashu-lib.git
$ cd cashu-lib
$ ./mvnw -q verify
```

The command compiles all modules, runs tests, and writes aggregated coverage to `target/site/jacoco-aggregate`.

To build a specific module with its dependencies, use:

```bash
$ ./mvnw -q -pl cashu-lib-common -am verify
```
