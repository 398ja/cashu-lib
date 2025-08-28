package xyz.tcheeric.cashu.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PublicKeyTest {

  private static final String PRIVATE_KEY_HEX =
      "0000000000000000000000000000000000000000000000000000000000000001";
  private static final String EXPECTED_PUBLIC_KEY_HEX =
      "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

  // Ensures deriving a public key from a known PrivateKey produces the expected generator point.
  @Test
  public void deriveFromPrivateKey() {
    PrivateKey privateKey = PrivateKey.fromString(PRIVATE_KEY_HEX);
    PublicKey actual = PublicKey.derivePublicKey(privateKey);
    assertEquals(PublicKey.fromString(EXPECTED_PUBLIC_KEY_HEX), actual);
  }

  // Ensures deriving a public key directly from a known private key string matches the expected
  // generator point.
  @Test
  public void deriveFromPrivateKeyString() {
    PublicKey actual = PublicKey.derivePublicKey(PRIVATE_KEY_HEX);
    assertEquals(PublicKey.fromString(EXPECTED_PUBLIC_KEY_HEX), actual);
  }
}
