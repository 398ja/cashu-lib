package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.util.KeysUtils;

public class PrivateKey extends BaseKey {

    protected PrivateKey(@NonNull String value) {
        this(Hex.decode(value));
    }

    protected PrivateKey(byte[] value) {
        super(value);
        if (value.length != PRIVATE_KEY_LENGTH / 2) {
            throw new IllegalArgumentException("Invalid private key length. (" + value.length + ")");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PrivateKey fromString(@NonNull String s) {
        return new PrivateKey(s);
    }

    public static PrivateKey fromBytes(byte[] bytes) {
        return new PrivateKey(bytes);
    }

    public static PublicKey derivePublicKey(@NonNull PrivateKey privateKey) {
        return PublicKey.fromString(Hex.toHexString(KeysUtils.derivePublicKey(privateKey.getBytes())));
    }

    public static PrivateKey generateRandom() {
        return PrivateKey.fromBytes(KeysUtils.generatePrivateKey());
    }

}