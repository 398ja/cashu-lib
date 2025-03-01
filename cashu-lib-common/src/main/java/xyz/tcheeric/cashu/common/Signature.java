package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.json.deserializer.SignatureDeserializer;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;

@JsonDeserialize(using = SignatureDeserializer.class)
public class Signature extends CryptoElement {

    private Signature(@NonNull String value) {
        super(value, SIGNATURE_LENGTH);
    }

    private Signature(byte[] value) {
        super(value, SIGNATURE_LENGTH);
    }

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