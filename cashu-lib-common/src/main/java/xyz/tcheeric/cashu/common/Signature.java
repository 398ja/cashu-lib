package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;

public class Signature extends CryptoElement {

    private Signature(@NonNull String value) {
        super(value, SIGNATURE_LENGTH);
    }

    private Signature(byte[] value) {
        super(value, SIGNATURE_LENGTH);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Signature fromString(@NonNull String s) {
        return new Signature(s);
    }

    public static Signature fromBytes(byte[] bytes) {
        return new Signature(bytes);
    }

    public static Signature fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHexString(b.toByteArray()));
    }
}