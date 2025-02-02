package xyz.tcheeric.cashu.common.model;

import cashu.util.Utils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.json.deserializer.RandomStringSecretDeserializer;

import java.math.BigInteger;
import java.security.SecureRandom;


@JsonDeserialize(using = RandomStringSecretDeserializer.class)
public class RandomStringSecret extends PrivateKey implements Secret {

    private RandomStringSecret(@NonNull String value) {
        super(value);
    }

    private RandomStringSecret(byte[] value) {
        super(value);
    }

    public static RandomStringSecret create() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return new RandomStringSecret(bytes);
    }

    @Override
    public String getData() {
        return this.toString();
    }

    @Override
    public void setData(@NonNull String data) {
        // Do nothing
    }

    public static RandomStringSecret fromString(@NonNull String s) {
        return new RandomStringSecret(s);
    }

    public static RandomStringSecret fromBytes(byte[] bytes) {
        return new RandomStringSecret(bytes);
    }

    public static RandomStringSecret fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHexString(b.toByteArray()));
    }
}
