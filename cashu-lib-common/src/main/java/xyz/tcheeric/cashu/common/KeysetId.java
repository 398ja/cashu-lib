package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * Value object representing a keyset identifier.
 * The identifier must be a hexadecimal string of length 16.
 */
@EqualsAndHashCode
public class KeysetId {

    public static final int KEYSET_ID_LENGTH = 16; // hexadecimal characters

    private final String value;

    private KeysetId(@NonNull String value) {
        if (!value.matches("^[0-9a-fA-F]{" + KEYSET_ID_LENGTH + "}$")) {
            throw new IllegalArgumentException("Invalid keyset id: " + value);
        }
        this.value = value.toLowerCase();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static KeysetId fromString(@NonNull String value) {
        return new KeysetId(value);
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
