package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Value object representing a keyset identifier.
 *
 * <p>The identifier is a hexadecimal string whose length identifies its NUT-02 version:
 * 16 characters for version 1 ids and 66 characters for version 2 ids.
 */
@EqualsAndHashCode
public class KeysetId {

    public static final int KEYSET_ID_LENGTH = 16; // hexadecimal characters

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]+$");

    private final String value;

    private KeysetId(@NonNull String value) {
        if (!HEX_PATTERN.matcher(value).matches() || !hasKnownVersionLength(value)) {
            throw new IllegalArgumentException("Invalid keyset id: " + value);
        }
        this.value = value.toLowerCase();
    }

    private static boolean hasKnownVersionLength(String value) {
        for (KeysetIdVersion version : KeysetIdVersion.values()) {
            if (value.length() == version.getHexLength()) {
                return true;
            }
        }
        return false;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static KeysetId fromString(@NonNull String value) {
        return new KeysetId(value);
    }

    /**
     * Returns the NUT-02 version this identifier belongs to.
     *
     * @return the keyset id version
     */
    public KeysetIdVersion getVersion() {
        return KeysetIdVersion.of(value);
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }

    public int toInt() {
        BigInteger bi = new BigInteger(value, 16);
        BigInteger mod = BigInteger.valueOf(Integer.MAX_VALUE); // 2^31 - 1
        return bi.mod(mod).intValue();
    }
}
