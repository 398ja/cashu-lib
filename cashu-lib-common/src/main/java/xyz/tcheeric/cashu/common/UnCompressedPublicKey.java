package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.util.Point;

public class UnCompressedPublicKey extends PublicKey {

    UnCompressedPublicKey(@NonNull byte[] bytes) {
        super(bytes);
        if (bytes.length != PUBLIC_KEY_LENGTH_UNCOMPRESSED / 2) {
            throw new IllegalArgumentException("Invalid uncompressed public key length (" + bytes.length + ")");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UnCompressedPublicKey fromBytes(@NonNull byte[] bytes) {
        return new UnCompressedPublicKey(bytes);
    }

    @Override
    public String toString() {
        return compress(this).toString();
    }

    static CompressedPublicKey compress(UnCompressedPublicKey unCompressedPublicKey) {
        Point point = new Point(Hex.toHexString(unCompressedPublicKey.getBytes()));
        String prefix = point.hasEvenY() ? "02" : "03";
        byte[] bytes = toFixed32(point.getX().toByteArray());
        return CompressedPublicKey.fromString(prefix + Hex.toHexString(bytes));
    }

}
