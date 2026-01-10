package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NonNull;

/**
 * Transport type for NUT-18 payment requests.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18 Specification</a>
 */
public enum TransportType {

    /**
     * Nostr transport. Target is an nprofile.
     */
    NOSTR("nostr"),

    /**
     * HTTP POST transport. Target is a URL.
     */
    POST("post");

    private final String value;

    TransportType(@NonNull String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransportType fromValue(@NonNull String value) {
        for (TransportType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transport type: " + value);
    }
}
