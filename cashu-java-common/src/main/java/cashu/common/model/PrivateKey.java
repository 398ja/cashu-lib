package cashu.common.model;

import cashu.common.json.deserializer.SecretDeserializer;
import cashu.util.Utils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;

import java.math.BigInteger;

@JsonDeserialize(using = SecretDeserializer.class)
public class PrivateKey extends Hex {

    private PrivateKey(@NonNull String value) {
        super(value, 66);
    }

    private PrivateKey(@NonNull byte[] value) {
        super(value, 66);
    }

    public static PrivateKey fromString(@NonNull String s) {
        Hex hex = Hex.fromString(s);
        return fromHex(hex);
    }

    public static PrivateKey fromBytes(@NonNull byte[] bytes) {
        return fromString(Utils.bytesToHex(bytes));
    }

    public static PrivateKey fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHex(b.toByteArray()));
    }

    private static PrivateKey fromHex(@NonNull Hex hex) {
        if (hex.toString().length() != 66) {
            throw new IllegalArgumentException(String.format("Invalid length: %d", hex.toString().length()));
        }
        return new PrivateKey(hex.getBytes());
    }
}