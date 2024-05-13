package cashu.common.model;

import cashu.common.json.deserializer.PublicKeyDeserializer;
import cashu.util.Utils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;

import java.math.BigInteger;

@JsonDeserialize(using = PublicKeyDeserializer.class)
public class PublicKey extends Hex {

    private PublicKey(@NonNull String value) {
        super(value, PUBLIC_KEY_LENGTH);
    }

    private PublicKey(@NonNull byte[] value) {
        super(value, PUBLIC_KEY_LENGTH);
    }

    public static PublicKey fromString(@NonNull String s) {
        Hex hex = Hex.fromString(s);
        return fromHex(hex);
    }

    public static PublicKey fromBytes(@NonNull byte[] bytes) {
        return fromString(Utils.bytesToHexString(bytes));
    }

    public static PublicKey fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHexString(b.toByteArray()));
    }

    private static PublicKey fromHex(@NonNull Hex hex) {
        if (hex.toString().length() != 66) {
            throw new IllegalArgumentException(String.format("Invalid length: %d", hex.toString().length()));
        }
        return new PublicKey(hex.getBytes());
    }
}