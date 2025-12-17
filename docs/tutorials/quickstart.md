# Quickstart

This tutorial guides you through verifying your environment, adding the dependencies, and running a basic Schnorr signature example with cashu-lib.

## Step 1: Verify your setup

Ensure the following tools are installed:

- Java 21
- Maven Wrapper (bundled in the repository)

Check the versions to confirm:

```bash
java -version
./mvnw -v
```

## Step 2: Add Maven dependencies

Add the modules you need to your project's `pom.xml` (replace `0.6.2` with the latest tag as needed):

```xml
<repositories>
    <repository>
        <id>cashu-lib</id>
        <url>https://maven.398ja.xyz/releases</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-common</artifactId>
    <version>0.6.2</version>
</dependency>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-crypto</artifactId>
    <version>0.6.2</version>
</dependency>

<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-entities</artifactId>
    <version>0.6.2</version>
</dependency>
```

## Step 3: Run a usage example

```java
import xyz.tcheeric.cashu.common.KeySet;
import xyz.tcheeric.cashu.common.Keys;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.crypto.Schnorr;
import xyz.tcheeric.cashu.crypto.util.KeysUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class QuickstartExample {
    public static void main(String[] args) throws Exception {
        byte[] secretKey = Schnorr.generatePrivateKey();

        // Use the compressed key for KeySet and derive the x-only key for Schnorr verification
        PublicKey publicKey = PublicKey.fromBytes(KeysUtils.derivePublicKey(secretKey));
        byte[] schnorrPublicKey = PublicKey.getSchnorr(publicKey);

        KeySet keyset = KeySet.builder()
                .id("keyset1")
                .unit("sat")
                .keys(new Keys().put(BigInteger.ONE, publicKey))
                .partPerThousand(0)
                .build();

        byte[] message = Utils.sha256("hello cashu".getBytes(StandardCharsets.UTF_8));
        byte[] signature = Schnorr.sign(message, secretKey);
        boolean valid = Schnorr.verify(message, schnorrPublicKey, signature);

        System.out.println("Keyset id: " + keyset.getId());
        System.out.println("Signature valid: " + valid);
    }
}
```

## Further Reading

- [cashu-lib-common API Reference](https://javadoc.io/doc/xyz.tcheeric/cashu-lib-common)
- [cashu-lib-crypto API Reference](https://javadoc.io/doc/xyz.tcheeric/cashu-lib-crypto)
- [cashu-lib-entities API Reference](https://javadoc.io/doc/xyz.tcheeric/cashu-lib-entities)
- [Cashu protocol tutorials](https://cashu.space/docs/)
