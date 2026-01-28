package xyz.tcheeric.cashu.common.nut18;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NonNull;

/**
 * Transport type for NUT-18V voucher payment requests.
 *
 * <p>Extends the standard NUT-18 transport types with MERCHANT for
 * direct merchant integration in Model B gift card scenarios.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18 Specification</a>
 */
public enum VoucherTransportType {

    /**
     * Nostr transport. Target is an nprofile.
     */
    NOSTR("nostr"),

    /**
     * HTTP POST transport. Target is a URL.
     */
    POST("post"),

    /**
     * Merchant transport for direct voucher redemption.
     * Target is a merchant endpoint URL.
     */
    MERCHANT("merchant");

    private final String value;

    VoucherTransportType(@NonNull String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static VoucherTransportType fromValue(@NonNull String value) {
        for (VoucherTransportType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown voucher transport type: " + value);
    }
}
