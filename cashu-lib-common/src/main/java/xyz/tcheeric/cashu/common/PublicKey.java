package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.json.deserializer.PublicKeyDeserializer;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;

@JsonDeserialize(using = PublicKeyDeserializer.class)
public class PublicKey extends CryptoElement {

    private PublicKey(@NonNull String value) {
        super(value, PUBLIC_KEY_LENGTH);
    }

    private PublicKey(byte[] value) {
        super(value, PUBLIC_KEY_LENGTH);
    }

    public static PublicKey fromString(@NonNull String s) {
        return new PublicKey(s);
    }

    public static PublicKey fromBytes(byte[] bytes) {
        return new PublicKey(bytes);
    }

    public static PublicKey fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHexString(b.toByteArray()));
    }
}