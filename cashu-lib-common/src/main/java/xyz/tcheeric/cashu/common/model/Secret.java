package xyz.tcheeric.cashu.common.model;

import xyz.tcheeric.cashu.common.json.deserializer.SecretDeserializer;
import cashu.util.Utils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;

import java.math.BigInteger;
import java.security.SecureRandom;

@JsonDeserialize(using = SecretDeserializer.class)
public class Secret extends PrivateKey {

    private Secret(@NonNull String value) {
        super(value);
    }

    private Secret(byte[] value) {
        super(value);
    }

    public static Secret create() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return new Secret(bytes);
    }

    public static Secret fromString(@NonNull String s) {
        return new Secret(s);
    }

    public static Secret fromBytes(byte[] bytes) {
        return new Secret(bytes);
    }

    public static Secret fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHexString(b.toByteArray()));
    }
}