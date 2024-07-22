package cashu.common.model;

import cashu.common.json.deserializer.SignatureDeserializer;
import cashu.util.Utils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;

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