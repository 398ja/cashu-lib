# Quickstart

This tutorial guides you through verifying your environment, adding the necessary dependencies, and running a basic Schnorr signature example with cashu-lib.

## Step 1: Verify your setup

Ensure the following tools are installed:

- Java 21
- Maven 3.8+

Check the versions to confirm:

```bash
java -version
mvn -version
```

## Step 2: Add Maven dependencies

Add the modules you need to your project's `pom.xml`:

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-common</artifactId>
    <version>0.3.0</version>
</dependency>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-crypto</artifactId>
    <version>0.3.0</version>
</dependency>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-entities</artifactId>
    <version>0.3.0</version>
</dependency>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-test</artifactId>
    <version>0.3.0</version>
    <scope>test</scope>
</dependency>
```

## Step 3: Run a usage example

```java
import xyz.tcheeric.cashu.common.KeySet;
import xyz.tcheeric.cashu.common.Keys;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.crypto.Schnorr;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

byte[] priv = Schnorr.generatePrivateKey();
byte[] pub = Schnorr.genPubKey(priv);

KeySet keyset = KeySet.builder()
        .id("keyset1")
        .unit("sat")
        .keys(new Keys().put(BigInteger.ONE, PublicKey.fromBytes(pub)))
        .partPerThousand(0)
        .build();

byte[] msg = "hello cashu".getBytes(StandardCharsets.UTF_8);
byte[] sig = Schnorr.sign(msg, priv);
boolean valid = Schnorr.verify(msg, pub, sig);

System.out.println("Signature valid: " + valid);
```

## Further Reading

- [cashu-lib-common API Reference](https://javadoc.io/doc/xyz.tcheeric/cashu-lib-common)
- [cashu-lib-crypto API Reference](https://javadoc.io/doc/xyz.tcheeric/cashu-lib-crypto)
- [cashu-lib-entities API Reference](https://javadoc.io/doc/xyz.tcheeric/cashu-lib-entities)
- [Cashu protocol tutorials](https://cashu.space/docs/)
