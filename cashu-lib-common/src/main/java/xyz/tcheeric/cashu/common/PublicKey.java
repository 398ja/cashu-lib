package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigInteger;
import lombok.NonNull;
import xyz.tcheeric.cashu.crypto.util.Utils;

public class PublicKey extends CryptoElement {

  private PublicKey(@NonNull String value) {
    super(value, PUBLIC_KEY_LENGTH);
  }

  private PublicKey(byte[] value) {
    super(value, PUBLIC_KEY_LENGTH);
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static PublicKey fromString(@NonNull String s) {
    return new PublicKey(s);
  }

  public static PublicKey fromBytes(byte[] bytes) {
    return new PublicKey(bytes);
  }

  public static PublicKey fromBigInteger(@NonNull BigInteger b) {
    return fromString(Utils.bytesToHexString(b.toByteArray()));
  }

  public static PublicKey derivePublicKey(@NonNull PrivateKey privateKey) {
    return PrivateKey.derivePublicKey(privateKey);
  }

  public static PublicKey derivePublicKey(@NonNull String privateKey) {
    return derivePublicKey(PrivateKey.fromString(privateKey));
  }
}
