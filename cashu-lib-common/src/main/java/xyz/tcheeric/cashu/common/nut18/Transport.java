package xyz.tcheeric.cashu.common.nut18;

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
 * Transport method for NUT-18 payment requests.
 *
 * <p>Defines how payment payloads should be transmitted from sender to receiver.
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
public class Transport {

    @JsonProperty("t")
    private String typeValue;

    /**
     * Gets the transport type as an enum.
     *
     * @return the TransportType enum value
     */
    public TransportType getType() {
        return typeValue != null ? TransportType.fromValue(typeValue) : null;
    }

    /**
     * Sets the transport type from an enum.
     *
     * @param type the TransportType enum value
     */
    public void setType(TransportType type) {
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
    public static Transport nostrNip17(@NonNull String nprofile) {
        Transport transport = new Transport();
        transport.setTypeValue(TransportType.NOSTR.getValue());
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
    public static Transport httpPost(@NonNull String url) {
        Transport transport = new Transport();
        transport.setTypeValue(TransportType.POST.getValue());
        transport.setTarget(url);
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
        return TransportType.NOSTR.equals(getType());
    }

    /**
     * Checks if this transport is an HTTP POST transport.
     *
     * @return true if type is POST
     */
    public boolean isHttpPost() {
        return TransportType.POST.equals(getType());
    }
}
