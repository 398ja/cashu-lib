package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.json.deserializer.PrivateKeyDeserializer;
import xyz.tcheeric.cashu.crypto.util.KeysUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;

@JsonDeserialize(using = PrivateKeyDeserializer.class)
public class PrivateKey extends CryptoElement {

    protected PrivateKey(@NonNull String value) {
        super(value, PRIVATE_KEY_LENGTH);
    }

    protected PrivateKey(byte[] value) {
        super(value, PRIVATE_KEY_LENGTH);
    }

    public static PrivateKey fromString(@NonNull String s) {
        return new PrivateKey(s);
    }

    public static PrivateKey fromBytes(byte[] bytes) {
        return new PrivateKey(bytes);
    }

    public static PrivateKey fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHexString(b.toByteArray()));
    }

    public static PublicKey derivePublicKey(@NonNull PrivateKey privateKey) {
        return PublicKey.fromBytes(KeysUtils.derivePublicKey(privateKey.toBytes()));
    }

    public static PrivateKey generateRandom() {
        return PrivateKey.fromBytes(KeysUtils.generatePrivateKey());
    }
}