package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.Schnorr;
import xyz.tcheeric.cashu.crypto.util.Point;
import xyz.tcheeric.cashu.common.json.serializer.SignatureJsonSerializer;
import xyz.tcheeric.cashu.common.json.deserializer.SignatureJsonDeserializer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents a cryptographic signature in the Cashu protocol.
 *
 * <p>This class handles two distinct signature types:
 * <ul>
 *   <li><b>BDHKE signatures</b> (C' and C): EC points on secp256k1, stored in compressed
 *       format (33 bytes: 02/03 prefix + 32-byte x-coordinate)</li>
 *   <li><b>Schnorr signatures</b> (NUT-11 P2PK): 64-byte raw signatures (r || s)</li>
 * </ul>
 *
 * <p>Input formats accepted:
 * <ul>
 *   <li>33 bytes: Compressed EC point (02/03 prefix + x-coordinate)</li>
 *   <li>64 bytes: Uncompressed EC point (x || y) OR Schnorr signature</li>
 *   <li>66 hex chars: Compressed EC point as hex string</li>
 * </ul>
 *
 * <p>Output format: Serialized as compressed EC point (66 hex chars with 02/03 prefix)
 * for JSON. Raw format is preserved internally for Schnorr signature verification.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/00.md">NUT-00: BDHKE</a>
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/11.md">NUT-11: P2PK with Schnorr</a>
 */
@JsonSerialize(using = SignatureJsonSerializer.class)
@JsonDeserialize(using = SignatureJsonDeserializer.class)
public final class Signature {

    /**
     * Length of a compressed EC point in bytes (1-byte prefix + 32-byte x-coordinate).
     */
    private static final int COMPRESSED_LENGTH = 33;

    /**
     * Length of an uncompressed EC point (x || y) or Schnorr signature in bytes.
     */
    private static final int UNCOMPRESSED_LENGTH = 64;

    /**
     * The signature bytes stored in compressed format (33 bytes) for serialization.
     */
    private final byte[] compressedBytes;

    /**
     * The raw signature bytes in original format (64 bytes for Schnorr, 33 for compressed EC).
     * Preserved for Schnorr signature verification which requires the original 64-byte format.
     */
    private final byte[] rawBytes;

    /**
     * Creates a Signature from a compressed hex string.
     *
     * @param compressedHex hex string (66 chars) with 02/03 prefix
     * @throws IllegalArgumentException if the format is invalid
     */
    private Signature(@NonNull String compressedHex) {
        if (compressedHex.length() != COMPRESSED_LENGTH * 2) {
            throw new IllegalArgumentException(
                    "Invalid compressed signature length: " + compressedHex.length() +
                    ". Expected " + (COMPRESSED_LENGTH * 2) + " hex chars.");
        }
        String prefix = compressedHex.substring(0, 2).toLowerCase();
        if (!prefix.equals("02") && !prefix.equals("03")) {
            throw new IllegalArgumentException(
                    "Invalid signature prefix: " + prefix + ". Expected 02 or 03.");
        }
        this.compressedBytes = Hex.decode(compressedHex);
        this.rawBytes = this.compressedBytes; // Same format
    }

    /**
     * Creates a Signature from compressed bytes (33 bytes with prefix).
     *
     * @param compressedBytes 33-byte array with 02/03 prefix
     * @param rawBytes the original bytes (may be same as compressedBytes or 64-byte Schnorr)
     */
    private Signature(byte[] compressedBytes, byte[] rawBytes) {
        if (compressedBytes.length != COMPRESSED_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid compressed signature length: " + compressedBytes.length +
                    ". Expected " + COMPRESSED_LENGTH + " bytes.");
        }
        byte prefix = compressedBytes[0];
        if (prefix != 0x02 && prefix != 0x03) {
            throw new IllegalArgumentException(
                    "Invalid signature prefix byte: " + String.format("0x%02x", prefix) +
                    ". Expected 0x02 or 0x03.");
        }
        this.compressedBytes = Arrays.copyOf(compressedBytes, compressedBytes.length);
        this.rawBytes = Arrays.copyOf(rawBytes, rawBytes.length);
    }

    /**
     * Returns the signature bytes in the format needed for the operation.
     *
     * <p>For Schnorr signature verification, returns the raw 64-byte format if available.
     * For EC point operations (BDHKE), returns the compressed 33-byte format.
     *
     * @return the signature bytes (raw format if 64-byte Schnorr, otherwise compressed)
     */
    public byte[] getBytes() {
        // Return raw format if it's a 64-byte Schnorr signature
        if (rawBytes.length == UNCOMPRESSED_LENGTH) {
            return Arrays.copyOf(rawBytes, rawBytes.length);
        }
        // Otherwise return compressed format
        return Arrays.copyOf(compressedBytes, compressedBytes.length);
    }

    /**
     * Returns the signature bytes in compressed format (33 bytes).
     *
     * @return copy of the compressed signature bytes
     */
    public byte[] getCompressedBytes() {
        return Arrays.copyOf(compressedBytes, compressedBytes.length);
    }

    /**
     * Creates a Signature from a compressed hex string.
     *
     * @param s hex string (66 chars) representing a compressed EC point (02/03 prefix + x-coordinate)
     * @return the Signature
     * @throws IllegalArgumentException if the format is invalid
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Signature fromString(@NonNull String s) {
        return new Signature(s);
    }

    /**
     * Creates a Signature from raw bytes.
     *
     * <p>Accepts both compressed and uncompressed formats:
     * <ul>
     *   <li>33 bytes: Compressed EC point (02/03 prefix + 32-byte x-coordinate)</li>
     *   <li>64 bytes: Uncompressed EC point (x || y coordinates) OR Schnorr signature</li>
     * </ul>
     *
     * <p>For 64-byte input, the original bytes are preserved for Schnorr signature
     * verification, while a compressed form is also computed for serialization.
     *
     * @param bytes signature bytes in compressed (33) or uncompressed (64) format
     * @return the Signature
     * @throws IllegalArgumentException if bytes length is neither 33 nor 64
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Signature fromBytes(byte[] bytes) {
        if (bytes.length == COMPRESSED_LENGTH) {
            // Already compressed: 0x02/0x03 prefix + 32-byte x-coordinate
            return new Signature(bytes, bytes);
        } else if (bytes.length == UNCOMPRESSED_LENGTH) {
            // Uncompressed format: x || y without prefix - compress it but preserve raw
            byte[] compressed = compressPoint(bytes);
            return new Signature(compressed, bytes);
        } else {
            throw new IllegalArgumentException(
                    "Invalid signature bytes length: " + bytes.length +
                    ". Expected " + COMPRESSED_LENGTH + " (compressed) or " +
                    UNCOMPRESSED_LENGTH + " (uncompressed/Schnorr).");
        }
    }

    /**
     * Compresses a 64-byte uncompressed point (x || y) to 33-byte compressed format.
     *
     * @param uncompressed 64-byte array containing x || y coordinates
     * @return 33-byte compressed point (prefix + x-coordinate)
     */
    private static byte[] compressPoint(byte[] uncompressed) {
        // Parse as EC point to determine Y parity
        Point point = new Point(Hex.toHexString(uncompressed));
        String prefix = point.hasEvenY() ? "02" : "03";

        // Get x-coordinate as fixed 32 bytes
        byte[] xBytes = toFixed32(point.getX().toByteArray());

        // Build compressed format
        byte[] compressed = new byte[COMPRESSED_LENGTH];
        compressed[0] = (byte) Integer.parseInt(prefix, 16);
        System.arraycopy(xBytes, 0, compressed, 1, 32);

        return compressed;
    }

    /**
     * Normalizes a byte array to exactly 32 bytes (left-padded or truncated).
     *
     * @param src source byte array
     * @return 32-byte array
     */
    private static byte[] toFixed32(byte[] src) {
        if (src.length == 32) {
            return src;
        }
        byte[] out = new byte[32];
        if (src.length > 32) {
            // Truncate leading bytes (handles BigInteger sign byte)
            System.arraycopy(src, src.length - 32, out, 0, 32);
        } else {
            // Left-pad with zeros
            System.arraycopy(src, 0, out, 32 - src.length, src.length);
        }
        return out;
    }

    /**
     * Returns the signature as a compressed hex string (66 characters).
     *
     * @return hex string with 02/03 prefix + 32-byte x-coordinate
     */
    @Override
    @JsonValue
    public String toString() {
        return Hex.toHexString(compressedBytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Signature other)) return false;
        // Compare compressed form for equality (canonical representation)
        return Arrays.equals(this.compressedBytes, other.compressedBytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(compressedBytes);
    }

    /**
     * Creates a Schnorr signature for the given message.
     *
     * <p>Used for NUT-11 P2PK signing. The resulting 64-byte Schnorr signature
     * is preserved for verification while a compressed form is computed for serialization.
     *
     * @param message the message to sign
     * @param privateKey the signing private key
     * @return the Schnorr signature
     * @throws Exception if signing fails
     * @see <a href="https://github.com/cashubtc/nuts/blob/main/11.md">NUT-11: P2PK</a>
     */
    public static Signature sign(@NonNull String message, @NonNull PrivateKey privateKey) throws Exception {
        byte[] signature = Schnorr.sign(message.getBytes(StandardCharsets.UTF_8), privateKey.getBytes());
        return Signature.fromBytes(signature);
    }

    /**
     * Verifies a Schnorr signature against a message and public key.
     *
     * @param message the original message
     * @param publicKey the signer's public key
     * @param signature the signature to verify
     * @return true if the signature is valid
     * @throws Exception if verification fails
     */
    public static boolean verify(@NonNull String message, @NonNull PublicKey publicKey, @NonNull Signature signature) throws Exception {
        return Schnorr.verify(message.getBytes(StandardCharsets.UTF_8), publicKey.getSchnorr(), signature.getBytes());
    }

    /**
     * Verifies this signature against a message and public key.
     *
     * @param message the original message
     * @param publicKey the signer's public key
     * @return true if the signature is valid
     * @throws Exception if verification fails
     */
    public boolean verify(@NonNull String message, @NonNull PublicKey publicKey) throws Exception {
        return verify(message, publicKey, this);
    }
}
