package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;

/**
 * @deprecated Use {@link PublicKey} directly instead. PublicKey now handles all formats
 *             and always serializes as compressed. This class will be removed in a future version.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("deprecation")
public final class CompressedPublicKey extends PublicKey {

    CompressedPublicKey(@NonNull String s) {
        super();
        // Delegate to parent by calling static factory
        PublicKey pk = PublicKey.fromString(s);
        // Copy bytes - this is a compatibility shim
        setBytes(pk.getBytes());
    }

    CompressedPublicKey(byte[] bytes) {
        this(Hex.toHexString(bytes));
    }

    /**
     * @deprecated Use {@link PublicKey#fromString(String)} instead.
     */
    @Deprecated(forRemoval = true)
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CompressedPublicKey fromString(@NonNull String s) {
        return new CompressedPublicKey(s);
    }

    /**
     * @deprecated Use {@link PublicKey#fromBytes(byte[])} instead.
     */
    @Deprecated(forRemoval = true)
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CompressedPublicKey fromBytes(@NonNull byte[] bytes) {
        return new CompressedPublicKey(bytes);
    }

    @Override
    public String toString() {
        return Hex.toHexString(getBytes());
    }
}
