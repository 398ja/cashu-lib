package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import xyz.tcheeric.cashu.crypto.Schnorr;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    public static Signature sign(@NonNull String message, @NonNull PrivateKey privateKey) throws Exception {
        byte[] signature = Schnorr.sign(message.getBytes(StandardCharsets.UTF_8), privateKey.getBytes());
        return Signature.fromBytes(signature);
    }

    public static boolean verify(@NonNull String message, @NonNull PublicKey publicKey, @NonNull Signature signature) throws Exception {
        byte[] pubKeyBytes = publicKey.getBytes();
        byte[] xOnlyPublicKey =
                pubKeyBytes.length == 33 ? Arrays.copyOfRange(pubKeyBytes, 1, 33) : pubKeyBytes;
        return Schnorr.verify(message.getBytes(StandardCharsets.UTF_8), xOnlyPublicKey, signature.getBytes());
    }

    public boolean verify(@NonNull String message, @NonNull PublicKey publicKey) throws Exception {
        return verify(message, publicKey, this);
    }
}