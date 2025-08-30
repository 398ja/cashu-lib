package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.util.Point;

public class CompressedPublicKey extends PublicKey {

    CompressedPublicKey(@NonNull String s) {
        super(s);
        if (s.length() != 2 + PUBLIC_KEY_LENGTH_COMPRESSED) {
            throw new IllegalArgumentException("Invalid compressed public key length (" + s.length() + ")");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CompressedPublicKey fromString(@NonNull String s) {
        return new CompressedPublicKey(s);
    }

    @Override
    public String toString() {
        // Already compressed; return prefix + x-coordinate
        return Hex.toHexString(getBytes());
    }

    protected static String toString(CompressedPublicKey publicKey) {
        return publicKey.toString();
    }
}
