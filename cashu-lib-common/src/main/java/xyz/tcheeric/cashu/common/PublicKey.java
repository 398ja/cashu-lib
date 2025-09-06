package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.util.Point;

import java.util.Arrays;


public class PublicKey extends BaseKey {

    private Object data;

    /**
     *
     * @param data String or byte[]
     */
    protected PublicKey(@NonNull Object data) {
        this.data = data;

        if (data instanceof String) { // compressed (includes 0x02/0x03 prefix)
            this.setBytes(Hex.decode((String) data));
        } else if (data instanceof byte[]) { // uncompressed (x||y)
            this.setBytes((byte[]) data);
        } else {
            throw new IllegalArgumentException("data must be String or byte[]");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PublicKey fromString(@NonNull String s) {
        return new CompressedPublicKey(s);
    }

    public static PublicKey fromBytes(byte[] bytes) {
        return new UnCompressedPublicKey(bytes);
    }

    public static PublicKey fromPoint(ECPoint ecPoint) {
        byte[] uncompressed = ecPoint.getEncoded(false); // 0x04 || X || Y
        return fromBytes(Arrays.copyOfRange(uncompressed, 1, uncompressed.length));
    }

    public byte[] getSchnorr() {
        return Hex.decode(toString().substring(2));
    }

    public static byte[] getSchnorr(@NonNull PublicKey publicKey) {
        return publicKey.getSchnorr();
    }

    public static PublicKey derivePublicKey(@NonNull PrivateKey privateKey) {
        return PrivateKey.derivePublicKey(privateKey);
    }

    public static PublicKey derivePublicKey(@NonNull String privateKey) {
        return derivePublicKey(PrivateKey.fromString(privateKey));
    }

    @Override
    public String toString() {
        if (isUnCompressed()) {
            return UnCompressedPublicKey.compress(new UnCompressedPublicKey(getBytes())).toString();
        }
        // Already compressed: return as provided (normalized to lowercase)
        return Hex.toHexString(getBytes());
    }

    // Sometimes BigInteger.toByteArray() returns a 33-byte array (a leading 0x00 sign byte is added when the highest bit is set).
    // The compressed public key code then builds a string with a 1-byte prefix plus a 33-byte X coordinate, and later the validator expects 32 bytes for X (64 hex chars),
    // toFixed32 will always encode the X coordinate as exactly 32 bytes (unsigned, zero-left-padded).
    protected static byte[] toFixed32(byte[] src) {
        // Strip possible sign byte and left pad to 32 bytes
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

    private boolean isUnCompressed() {
        return data instanceof byte[];
    }
}
