package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import xyz.tcheeric.cashu.crypto.Schnorr;
import xyz.tcheeric.cashu.common.json.serializer.SignatureJsonSerializer;
import xyz.tcheeric.cashu.common.json.deserializer.SignatureJsonDeserializer;

import java.nio.charset.StandardCharsets;

@JsonSerialize(using = SignatureJsonSerializer.class)
@JsonDeserialize(using = SignatureJsonDeserializer.class)
public class Signature {

    private final PublicKey publicKey;

    // Schnorr signatures are 64 bytes (128 hex chars)
    private static final int SIGNATURE_LENGTH = 128;

    protected Signature(@NonNull String value) {
        publicKey = new PublicKey(value);
        if (value.length() != 2 + SIGNATURE_LENGTH / 2) {
            throw new IllegalArgumentException("Invalid signature length");
        }
    }

    protected Signature(byte[] value) {
        publicKey = new PublicKey(value);
        if (value.length != SIGNATURE_LENGTH / 2) {
            throw new IllegalArgumentException("Invalid signature length (" + value.length + ")");
        }
    }

    public byte[] getBytes() {
        return publicKey.getBytes();
    }

    /**
     *
     * @param s the x coordinate of the signature with prefix 02 or 03.
     * @return The compressed signature.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Signature fromString(@NonNull String s) {
        return new Signature(s);
    }

    /**
     *
     * @param bytes The 64-byte (128 hex character) value, which is just the x and y coordinates concatenated (uncompressed format).
     * @return The uncompressed signature.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Signature fromBytes(byte[] bytes) {
        return new Signature(bytes);
    }

    @Override
    @JsonValue
    public String toString() {
        return publicKey.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Signature)) return false;
        Signature other = (Signature) obj;
        return this.publicKey.toString().equals(other.publicKey.toString());
    }

    @Override
    public int hashCode() {
        return this.publicKey.toString().hashCode();
    }

    public static Signature sign(@NonNull String message, @NonNull PrivateKey privateKey) throws Exception {
        byte[] signature = Schnorr.sign(message.getBytes(StandardCharsets.UTF_8), privateKey.getBytes());
        return Signature.fromBytes(signature);
    }

    public static boolean verify(@NonNull String message, @NonNull PublicKey publicKey, @NonNull Signature signature) throws Exception {
        return Schnorr.verify(message.getBytes(StandardCharsets.UTF_8), publicKey.getSchnorr(), signature.getBytes());
    }

    public boolean verify(@NonNull String message, @NonNull PublicKey publicKey) throws Exception {
        return verify(message, publicKey, this);
    }
}
