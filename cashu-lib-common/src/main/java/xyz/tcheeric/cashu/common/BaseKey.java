package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.bouncycastle.util.encoders.Hex;

@Getter
@Setter(AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public abstract class BaseKey {

    protected static final int PRIVATE_KEY_LENGTH = 64;
    protected static final int PUBLIC_KEY_LENGTH_UNCOMPRESSED = 66;
    protected static final int PUBLIC_KEY_LENGTH_COMPRESSED = 64;
    protected static final int SECRET_LENGTH = 64;


    @EqualsAndHashCode.Include
    private byte[] bytes;


    protected BaseKey(@NonNull String hexStr) {
        this.bytes = Hex.decode(hexStr.substring(2));
    }

    protected BaseKey(byte[] bytes) {
        this.bytes = bytes;
    }

    public boolean equalsIgnoreCase(@NonNull BaseKey key) {
        return this.toString().equalsIgnoreCase(key.toString());
    }

    @JsonValue
    @Override
    public String toString() {
        return Hex.toHexString(bytes);
    }

    public byte[] toBytes() {
        return bytes;
    }
}