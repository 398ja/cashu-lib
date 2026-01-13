package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Transport method for NUT-18V voucher payment requests.
 *
 * <p>Extends the standard NUT-18 transport with MERCHANT type for
 * direct merchant voucher redemption in Model B gift card scenarios.
 * Transports are sorted by receiver preference (first = most preferred).
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18 Specification</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"t", "a", "g"})
public class VoucherTransport {

    @JsonProperty("t")
    private String typeValue;

    /**
     * Gets the transport type as an enum.
     *
     * @return the VoucherTransportType enum value
     */
    public VoucherTransportType getType() {
        return typeValue != null ? VoucherTransportType.fromValue(typeValue) : null;
    }

    /**
     * Sets the transport type from an enum.
     *
     * @param type the VoucherTransportType enum value
     */
    public void setType(VoucherTransportType type) {
        this.typeValue = type != null ? type.getValue() : null;
    }

    @JsonProperty("a")
    private String target;

    @JsonProperty("g")
    @Builder.Default
    private List<List<String>> tags = new ArrayList<>();

    /**
     * Creates a Nostr transport with NIP-17 direct message support.
     *
     * @param nprofile the Nostr nprofile bech32 string
     * @return a configured Nostr transport
     */
    public static VoucherTransport nostrNip17(@NonNull String nprofile) {
        VoucherTransport transport = new VoucherTransport();
        transport.setTypeValue(VoucherTransportType.NOSTR.getValue());
        transport.setTarget(nprofile);
        transport.addTag("n", "17");
        return transport;
    }

    /**
     * Creates an HTTP POST transport.
     *
     * @param url the callback URL to POST payment payloads to
     * @return a configured HTTP POST transport
     */
    public static VoucherTransport httpPost(@NonNull String url) {
        VoucherTransport transport = new VoucherTransport();
        transport.setTypeValue(VoucherTransportType.POST.getValue());
        transport.setTarget(url);
        return transport;
    }

    /**
     * Creates a MERCHANT transport for direct voucher redemption.
     *
     * @param merchantEndpoint the merchant's redemption endpoint URL
     * @return a configured MERCHANT transport
     */
    public static VoucherTransport merchant(@NonNull String merchantEndpoint) {
        VoucherTransport transport = new VoucherTransport();
        transport.setTypeValue(VoucherTransportType.MERCHANT.getValue());
        transport.setTarget(merchantEndpoint);
        return transport;
    }

    /**
     * Creates a MERCHANT transport with additional metadata.
     *
     * @param merchantEndpoint the merchant's redemption endpoint URL
     * @param merchantId optional merchant identifier
     * @return a configured MERCHANT transport
     */
    public static VoucherTransport merchant(@NonNull String merchantEndpoint, String merchantId) {
        VoucherTransport transport = merchant(merchantEndpoint);
        if (merchantId != null && !merchantId.isBlank()) {
            transport.addTag("merchant_id", merchantId);
        }
        return transport;
    }

    /**
     * Adds a key-value tag to this transport.
     *
     * @param key the tag key
     * @param value the tag value
     */
    public void addTag(@NonNull String key, @NonNull String value) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(List.of(key, value));
    }

    /**
     * Gets the first value for a given tag key.
     *
     * @param key the tag key to look up
     * @return the tag value, or null if not found
     */
    public String getTagValue(@NonNull String key) {
        if (tags == null) {
            return null;
        }
        return tags.stream()
                .filter(tag -> tag.size() >= 2 && key.equals(tag.get(0)))
                .map(tag -> tag.get(1))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if this transport is a Nostr transport.
     *
     * @return true if type is NOSTR
     */
    public boolean isNostr() {
        return VoucherTransportType.NOSTR.equals(getType());
    }

    /**
     * Checks if this transport is an HTTP POST transport.
     *
     * @return true if type is POST
     */
    public boolean isHttpPost() {
        return VoucherTransportType.POST.equals(getType());
    }

    /**
     * Checks if this transport is a MERCHANT transport.
     *
     * @return true if type is MERCHANT
     */
    public boolean isMerchant() {
        return VoucherTransportType.MERCHANT.equals(getType());
    }
}
