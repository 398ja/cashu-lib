# Quickstart

## Requirements

- Java 21
- Maven 3.8+

Verify your setup:

```bash
java -version
mvn -version
```

## Maven Dependencies
Add the modules you need to your project's `pom.xml`:

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-common</artifactId>
    <version>0.1.1</version>
</dependency>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-crypto</artifactId>
    <version>0.1.1</version>
</dependency>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-entities</artifactId>
    <version>0.1.1</version>
</dependency>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-test</artifactId>
    <version>0.1.1</version>
    <scope>test</scope>
</dependency>
```

## Usage Example

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
