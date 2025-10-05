package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.json.deserializer.RandomStringSecretDeserializer;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.security.SecureRandom;


@JsonDeserialize(using = RandomStringSecretDeserializer.class)
public class RandomStringSecret extends BaseKey implements Secret {

    private RandomStringSecret(@NonNull String value) {
        // Secrets are UTF-8 strings, often hex-encoded
        // NUT-00: "use of a 64 character hex string generated from 32 random bytes is recommended"
        this(org.bouncycastle.util.encoders.Hex.decode(value));
    }

    private RandomStringSecret(byte[] value) {
        // Secrets can be any length per NUT-00 spec (recommendation: 64 hex chars/32 bytes)
        super(value);
    }

    public static RandomStringSecret create() {
        return create(32);
    }

    public static RandomStringSecret create(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive, got: " + length);
        }
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return new RandomStringSecret(bytes);
    }


    @Override
    public byte[] getData() {
        return this.getBytes();
    }

    @Override
    public void setData(@NonNull byte[] data) {
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
