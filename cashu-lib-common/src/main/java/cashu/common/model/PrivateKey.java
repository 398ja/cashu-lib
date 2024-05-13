package cashu.common.model;

import cashu.common.json.deserializer.SecretDeserializer;
import cashu.util.Utils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;

import java.math.BigInteger;

@JsonDeserialize(using = SecretDeserializer.class)
public class PrivateKey extends Hex {

    private PrivateKey(@NonNull String value) {
        super(value, PRIVATE_KEY_LENGTH);
    }

    private PrivateKey(@NonNull byte[] value) {
        super(value, PRIVATE_KEY_LENGTH);
    }

    public static PrivateKey fromString(@NonNull String s) {
        Hex hex = Hex.fromString(s);
        return fromHex(hex);
    }

    public static PrivateKey fromBytes(@NonNull byte[] bytes) {
        return fromString(Utils.bytesToHexString(bytes));
    }

    public static PrivateKey fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHexString(b.toByteArray()));
    }

    private static PrivateKey fromHex(@NonNull Hex hex) {
        if (hex.toString().length() != PRIVATE_KEY_LENGTH) {
            throw new IllegalArgumentException(String.format("Invalid length: %d", hex.toString().length()));
        }
        return new PrivateKey(hex.getBytes());
    }
}