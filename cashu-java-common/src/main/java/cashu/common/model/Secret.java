package cashu.common.model;

import cashu.common.json.deserializer.SecretDeserializer;
import cashu.util.Utils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;

import java.math.BigInteger;

@JsonDeserialize(using = SecretDeserializer.class)
public class Secret extends Hex {

    private Secret(@NonNull String value) {
        super(value, 64);
    }

    private Secret(@NonNull byte[] value) {
        super(value, 64);
    }

    public static Secret fromString(@NonNull String s) {
        Hex hex = Hex.fromString(s);
        return fromHex(hex);
    }

    public static Secret fromBytes(@NonNull byte[] bytes) {
        return fromString(Utils.bytesToHex(bytes));
    }

    public static Secret fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHex(b.toByteArray()));
    }

    private static Secret fromHex(@NonNull Hex hex) {
        if (hex.toString().length() != 64) {
            throw new IllegalArgumentException(String.format("Invalid length: %d", hex.toString().length()));
        }
        return new Secret(hex.getBytes());
    }
}