package cashu.common.model;

import cashu.common.json.deserializer.SignatureDeserializer;
import cashu.util.Utils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;

import java.math.BigInteger;

@JsonDeserialize(using = SignatureDeserializer.class)
public class Signature extends Hex {

    private Signature(@NonNull String value) {
        super(value, 66);
    }

    private Signature(@NonNull byte[] value) {
        super(value, 66);
    }

    public static Signature fromString(@NonNull String s) {
        Hex hex = Hex.fromString(s);
        return fromHex(hex);
    }

    public static Signature fromBytes(@NonNull byte[] bytes) {
        return fromString(Utils.bytesToHex(bytes));
    }

    public static Signature fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHex(b.toByteArray()));
    }

    private static Signature fromHex(@NonNull Hex hex) {
        if (hex.toString().length() != 66) {
            throw new IllegalArgumentException(String.format("Invalid length: %d", hex.toString().length()));
        }
        return new Signature(hex.getBytes());
    }
}