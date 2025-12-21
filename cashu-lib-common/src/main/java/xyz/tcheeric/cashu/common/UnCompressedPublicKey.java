package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;

/**
 * @deprecated Use {@link PublicKey} directly instead. PublicKey now handles all formats
 *             including uncompressed input, and provides {@link PublicKey#getUncompressedBytes()}
 *             when uncompressed output is needed. This class will be removed in a future version.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("deprecation")
public class UnCompressedPublicKey extends PublicKey {

    /**
     * Expected byte length for uncompressed coordinates (x || y without prefix).
     */
    private static final int UNCOMPRESSED_XY_BYTES = 64;

    UnCompressedPublicKey(@NonNull byte[] bytes) {
        super();
        if (bytes.length != UNCOMPRESSED_XY_BYTES) {
            throw new IllegalArgumentException(
                    "Invalid uncompressed public key length: " + bytes.length +
                    ". Expected " + UNCOMPRESSED_XY_BYTES + " bytes (x || y coordinates).");
        }
        // Convert to PublicKey and get compressed bytes
        PublicKey pk = PublicKey.fromBytes(bytes);
        setBytes(pk.getBytes());
    }

    UnCompressedPublicKey(@NonNull String s) {
        this(Hex.decode(s));
    }

    /**
     * @deprecated Use {@link PublicKey#fromBytes(byte[])} instead.
     */
    @Deprecated(forRemoval = true)
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UnCompressedPublicKey fromBytes(@NonNull byte[] bytes) {
        return new UnCompressedPublicKey(bytes);
    }

    /**
     * @deprecated Use {@link PublicKey#fromString(String)} instead.
     */
    @Deprecated(forRemoval = true)
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UnCompressedPublicKey fromString(@NonNull String s) {
        return new UnCompressedPublicKey(s);
    }

    @Override
    public String toString() {
        return Hex.toHexString(getBytes());
    }
}
