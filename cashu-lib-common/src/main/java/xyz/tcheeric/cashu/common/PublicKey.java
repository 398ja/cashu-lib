package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NonNull;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.util.Point;

import java.util.Arrays;

/**
 * Represents a public key in the Cashu protocol.
 *
 * <p>Public keys are secp256k1 elliptic curve points used for:
 * <ul>
 *   <li>Mint public keys (keyset keys for each denomination)</li>
 *   <li>NUT-11 P2PK recipient keys</li>
 *   <li>Schnorr signature verification</li>
 *   <li>BDHKE blind signatures (Y, B', C', C points)</li>
 * </ul>
 *
 * <p><b>Input formats accepted:</b>
 * <ul>
 *   <li>33 bytes: Compressed (02/03 prefix + x-coordinate)</li>
 *   <li>64 bytes: Uncompressed without prefix (x || y)</li>
 *   <li>65 bytes: SEC1 uncompressed (04 prefix + x || y)</li>
 *   <li>66 hex chars: Compressed as hex string</li>
 *   <li>128 hex chars: Uncompressed (x || y) as hex string</li>
 * </ul>
 *
 * <p><b>Output format:</b> Always serialized as compressed (33 bytes / 66 hex chars)
 * per NUT-00 specification.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/00.md">NUT-00</a>
 */
public sealed class PublicKey extends BaseKey permits CompressedPublicKey, UnCompressedPublicKey {

    /**
     * Length of compressed public key in bytes (prefix + x-coordinate).
     */
    private static final int COMPRESSED_BYTES = 33;

    /**
     * Length of uncompressed coordinates in bytes (x || y, no prefix).
     */
    private static final int UNCOMPRESSED_XY_BYTES = 64;

    /**
     * Length of SEC1 uncompressed in bytes (04 prefix + x || y).
     */
    private static final int SEC1_UNCOMPRESSED_BYTES = 65;

    /**
     * The public key stored in compressed format (33 bytes).
     */
    private byte[] compressedBytes;

    /**
     * Protected no-args constructor for subclass compatibility.
     * @deprecated Subclasses are deprecated; use PublicKey directly.
     */
    @Deprecated(forRemoval = true)
    protected PublicKey() {
        // For deprecated subclass compatibility
    }

    /**
     * Creates a PublicKey from compressed bytes (33 bytes).
     *
     * @param compressedBytes 33-byte array with 02/03 prefix
     */
    private PublicKey(byte[] compressedBytes) {
        if (compressedBytes.length != COMPRESSED_BYTES) {
            throw new IllegalArgumentException(
                    "Invalid compressed public key length: " + compressedBytes.length +
                    ". Expected " + COMPRESSED_BYTES + " bytes.");
        }
        byte prefix = compressedBytes[0];
        if (prefix != 0x02 && prefix != 0x03) {
            throw new IllegalArgumentException(
                    "Invalid public key prefix: " + String.format("0x%02x", prefix) +
                    ". Expected 0x02 or 0x03.");
        }
        this.compressedBytes = Arrays.copyOf(compressedBytes, compressedBytes.length);
        setBytes(this.compressedBytes);
    }

    /**
     * Creates a PublicKey from a compressed hex string (66 chars).
     *
     * @param compressedHex hex string with 02/03 prefix
     */
    private PublicKey(@NonNull String compressedHex) {
        this(Hex.decode(compressedHex));
    }

    /**
     * Returns the public key bytes in compressed format (33 bytes).
     *
     * @return copy of compressed bytes
     */
    @Override
    public byte[] getBytes() {
        if (compressedBytes != null) {
            return Arrays.copyOf(compressedBytes, compressedBytes.length);
        }
        // Fallback for deprecated subclasses using BaseKey.bytes
        return super.getBytes();
    }

    /**
     * Returns the x-coordinate for Schnorr signature verification.
     *
     * <p>BIP-340 Schnorr signatures use x-only public keys (32 bytes).
     *
     * @return 32-byte x-coordinate (without prefix)
     */
    public byte[] getSchnorr() {
        byte[] bytes = getBytes();
        return Arrays.copyOfRange(bytes, 1, bytes.length);
    }

    /**
     * Returns the uncompressed coordinates (x || y) without prefix.
     *
     * <p>Used for NUT-12 DLEQ proof hashing which requires uncompressed points.
     *
     * @return 64-byte array (x || y coordinates)
     */
    public byte[] getUncompressedBytes() {
        byte[] bytes = getBytes();

        // Decompress using Point utility
        Point point = Point.liftX(Arrays.copyOfRange(bytes, 1, bytes.length));
        if (point == null) {
            throw new IllegalStateException("Failed to decompress public key");
        }

        // Determine correct Y based on prefix parity
        boolean needEvenY = bytes[0] == 0x02;
        if (point.hasEvenY() != needEvenY) {
            // Negate Y to get correct parity
            point = new Point(point.getX(), Point.getp().subtract(point.getY()));
        }

        byte[] x = toFixed32(point.getX().toByteArray());
        byte[] y = toFixed32(point.getY().toByteArray());

        byte[] result = new byte[UNCOMPRESSED_XY_BYTES];
        System.arraycopy(x, 0, result, 0, 32);
        System.arraycopy(y, 0, result, 32, 32);
        return result;
    }

    /**
     * Returns the SEC1 uncompressed format (04 prefix + x || y).
     *
     * @return 65-byte array in SEC1 uncompressed format
     */
    public byte[] getSec1Uncompressed() {
        byte[] xy = getUncompressedBytes();
        byte[] result = new byte[SEC1_UNCOMPRESSED_BYTES];
        result[0] = 0x04;
        System.arraycopy(xy, 0, result, 1, xy.length);
        return result;
    }

    /**
     * Creates a PublicKey from a hex string.
     *
     * <p>Accepts compressed (66 chars) or uncompressed (128 chars) format.
     *
     * @param s hex string
     * @return PublicKey instance
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PublicKey fromString(@NonNull String s) {
        int len = s.length();
        if (len == COMPRESSED_BYTES * 2) {
            // Compressed: 66 hex chars
            return new PublicKey(s);
        } else if (len == UNCOMPRESSED_XY_BYTES * 2) {
            // Uncompressed x||y: 128 hex chars
            return fromBytes(Hex.decode(s));
        } else {
            throw new IllegalArgumentException(
                    "Invalid public key hex length: " + len +
                    ". Expected 66 (compressed) or 128 (uncompressed).");
        }
    }

    /**
     * Creates a PublicKey from bytes.
     *
     * <p>Accepts:
     * <ul>
     *   <li>33 bytes: Compressed (02/03 prefix + x)</li>
     *   <li>64 bytes: Uncompressed (x || y, no prefix)</li>
     *   <li>65 bytes: SEC1 uncompressed (04 + x || y)</li>
     * </ul>
     *
     * @param bytes public key bytes
     * @return PublicKey instance
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PublicKey fromBytes(byte[] bytes) {
        if (bytes.length == COMPRESSED_BYTES) {
            return new PublicKey(bytes);
        } else if (bytes.length == UNCOMPRESSED_XY_BYTES) {
            // Compress x||y to 33 bytes
            return new PublicKey(compressXY(bytes));
        } else if (bytes.length == SEC1_UNCOMPRESSED_BYTES) {
            // Strip 04 prefix and compress
            if (bytes[0] != 0x04) {
                throw new IllegalArgumentException(
                        "Invalid SEC1 uncompressed prefix: " + String.format("0x%02x", bytes[0]));
            }
            byte[] xy = Arrays.copyOfRange(bytes, 1, bytes.length);
            return new PublicKey(compressXY(xy));
        } else {
            throw new IllegalArgumentException(
                    "Invalid public key bytes length: " + bytes.length +
                    ". Expected 33 (compressed), 64 (x||y), or 65 (SEC1 uncompressed).");
        }
    }

    /**
     * Creates a PublicKey from an EC point.
     *
     * @param ecPoint the elliptic curve point
     * @return PublicKey instance
     */
    public static PublicKey fromPoint(ECPoint ecPoint) {
        byte[] encoded = ecPoint.getEncoded(true); // compressed
        return new PublicKey(encoded);
    }

    /**
     * @deprecated Use {@link #fromBytes(byte[])} instead - format is auto-detected.
     */
    @Deprecated(forRemoval = true)
    public static PublicKey fromBytes(byte[] bytes, boolean compressed) {
        return fromBytes(bytes);
    }

    /**
     * @deprecated Use {@link #fromString(String)} instead - format is auto-detected.
     */
    @Deprecated(forRemoval = true)
    public static PublicKey fromString(String str, boolean compressed) {
        return fromString(str);
    }

    /**
     * @deprecated Use {@link #fromPoint(ECPoint)} instead.
     */
    @Deprecated(forRemoval = true)
    public static PublicKey fromPoint(ECPoint ecPoint, boolean compressed) {
        return fromPoint(ecPoint);
    }

    /**
     * Derives the public key from a private key.
     *
     * @param privateKey the private key
     * @return the corresponding public key
     */
    public static PublicKey derivePublicKey(@NonNull PrivateKey privateKey) {
        return PrivateKey.derivePublicKey(privateKey);
    }

    /**
     * Derives the public key from a private key hex string.
     *
     * @param privateKey the private key as hex string
     * @return the corresponding public key
     */
    public static PublicKey derivePublicKey(@NonNull String privateKey) {
        try (PrivateKey key = PrivateKey.fromString(privateKey)) {
            return derivePublicKey(key);
        }
    }

    /**
     * Static helper to get x-coordinate for Schnorr verification.
     *
     * @param publicKey the public key
     * @return 32-byte x-coordinate
     */
    public static byte[] getSchnorr(@NonNull PublicKey publicKey) {
        return publicKey.getSchnorr();
    }

    /**
     * Compresses 64-byte (x || y) coordinates to 33-byte compressed format.
     *
     * @param xy 64-byte uncompressed coordinates
     * @return 33-byte compressed public key
     */
    private static byte[] compressXY(byte[] xy) {
        Point point = new Point(Hex.toHexString(xy));
        String prefix = point.hasEvenY() ? "02" : "03";
        byte[] x = toFixed32(point.getX().toByteArray());

        byte[] result = new byte[COMPRESSED_BYTES];
        result[0] = (byte) Integer.parseInt(prefix, 16);
        System.arraycopy(x, 0, result, 1, 32);
        return result;
    }

    /**
     * Normalizes a byte array to exactly 32 bytes.
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
            System.arraycopy(src, src.length - 32, out, 0, 32);
        } else {
            System.arraycopy(src, 0, out, 32 - src.length, src.length);
        }
        return out;
    }

    /**
     * Returns the public key as a compressed hex string (66 characters).
     *
     * @return hex string with 02/03 prefix
     */
    @JsonValue
    @Override
    public String toString() {
        return Hex.toHexString(getBytes());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PublicKey other)) return false;
        return Arrays.equals(this.getBytes(), other.getBytes());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getBytes());
    }
}
