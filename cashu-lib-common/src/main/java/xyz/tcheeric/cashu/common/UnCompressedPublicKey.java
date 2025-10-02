package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.util.Point;

public class UnCompressedPublicKey extends PublicKey {

    UnCompressedPublicKey(@NonNull byte[] bytes) {
        setBytes(bytes);
        // Expect 64 bytes (x || y) for uncompressed point form
        if (bytes.length != 64) {
            throw new IllegalArgumentException("Invalid uncompressed public key length (" + bytes.length + ")");
        }
    }

    UnCompressedPublicKey(@NonNull String s) {
        this(Hex.decode(s));
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UnCompressedPublicKey fromBytes(@NonNull byte[] bytes) {
        return new UnCompressedPublicKey(bytes);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UnCompressedPublicKey fromString(@NonNull String s) {
        return new UnCompressedPublicKey(s);
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
