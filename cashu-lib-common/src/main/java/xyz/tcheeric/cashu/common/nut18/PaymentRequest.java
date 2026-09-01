package xyz.tcheeric.cashu.common.nut18;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.cashu.common.nut10.Nut10Option;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * NUT-18 Payment Request for receiver-initiated transactions.
 *
 * <p>Payment requests allow receivers to specify payment requirements including
 * amount, unit, permitted mints, transport methods, and optional locking conditions.
 *
 * <p>Encoding format: {@code creqA + base64_urlsafe(CBOR(PaymentRequest))}
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18 Specification</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"i", "a", "u", "s", "m", "d", "t", "nut10"})
public class PaymentRequest {

    /** Prefix for encoded payment requests. */
    public static final String REQUEST_PREFIX = "creq";

    /** Version code for this payment request format. */
    public static final String VERSION_CODE = "A";

    /** URI scheme for clickable payment requests. */
    public static final String URI_SCHEME = "cashu:";

    @JsonProperty("i")
    private String paymentId;

    @JsonProperty("a")
    private Integer amount;

    @JsonProperty("u")
    private String unit;

    @JsonProperty("s")
    private Boolean singleUse;

    @JsonProperty("m")
    @Builder.Default
    private List<String> mints = new ArrayList<>();

    @JsonProperty("d")
    private String description;

    @JsonProperty("t")
    @Builder.Default
    private List<Transport> transports = new ArrayList<>();

    @JsonProperty("nut10")
    private Nut10Option nut10Option;

    /**
     * Validates that unit is present when amount is specified.
     *
     * @throws IllegalStateException if amount is set but unit is missing
     */
    public void validate() {
        if (amount != null && (unit == null || unit.isBlank())) {
            throw new IllegalStateException(
                    "Unit is required when amount is specified. Amount: " + amount);
        }
    }

    /**
     * Serializes this payment request to the NUT-18 encoded format.
     *
     * @param clickable if true, prepends the cashu: URI scheme for clickable links
     * @return the encoded payment request string
     * @throws RuntimeException if serialization fails
     */
    public String serialize(boolean clickable) {
        validate();
        log.debug("Serializing PaymentRequest with id={}", paymentId);

        byte[] cborBytes = PaymentRequestCborEncoder.encode(this);
        String encoded = REQUEST_PREFIX + VERSION_CODE
                + Base64.getUrlEncoder().withoutPadding().encodeToString(cborBytes);

        return clickable ? URI_SCHEME + encoded : encoded;
    }

    /**
     * Serializes this payment request without the URI scheme.
     *
     * @return the encoded payment request string
     */
    public String serialize() {
        return serialize(false);
    }

    /**
     * Deserializes a payment request from the NUT-18 encoded format.
     *
     * <p>Supports both URL-safe Base64 (RFC 4648 section 5) and standard Base64 encoding.
     *
     * @param serialized the encoded payment request string
     * @return the deserialized PaymentRequest
     * @throws IllegalArgumentException if the format is invalid
     * @throws RuntimeException if deserialization fails
     */
    public static PaymentRequest deserialize(@NonNull String serialized) {
        if (serialized.startsWith(URI_SCHEME)) {
            serialized = serialized.substring(URI_SCHEME.length());
        }

        String expectedPrefix = REQUEST_PREFIX + VERSION_CODE;
        if (!serialized.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException(
                    "Invalid payment request format. Expected prefix: " + expectedPrefix);
        }

        String base64Payload = serialized.substring(expectedPrefix.length());
        byte[] cborBytes = decodeBase64(base64Payload);

        try {
            PaymentRequest request = JsonUtils.CBOR_MAPPER.readValue(cborBytes, PaymentRequest.class);
            log.debug("Deserialized PaymentRequest with id={}", request.getPaymentId());
            return request;
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize PaymentRequest", e);
        }
    }

    /**
     * Decodes Base64 payload, supporting both URL-safe and standard encodings.
     *
     * @param base64Payload the Base64 encoded string
     * @return the decoded bytes
     */
    private static byte[] decodeBase64(String base64Payload) {
        // Check if it contains standard Base64 characters (+ or /)
        if (base64Payload.contains("+") || base64Payload.contains("/")) {
            // Use standard Base64 decoder
            return Base64.getDecoder().decode(base64Payload);
        }
        // Use URL-safe Base64 decoder
        return Base64.getUrlDecoder().decode(base64Payload);
    }

    /**
     * Adds a mint URL to the permitted mints list.
     * Trailing slashes are automatically stripped.
     *
     * @param mintUrl the mint URL to add
     */
    public void addMint(@NonNull String mintUrl) {
        if (this.mints == null) {
            this.mints = new ArrayList<>();
        }
        this.mints.add(mintUrl.replaceAll("/+$", ""));
    }

    /**
     * Adds a transport method to the transports list.
     * Transports are sorted by preference (first = most preferred).
     *
     * @param transport the transport to add
     */
    public void addTransport(@NonNull Transport transport) {
        if (this.transports == null) {
            this.transports = new ArrayList<>();
        }
        this.transports.add(transport);
    }

    /**
     * Gets the preferred transport (first in the list).
     *
     * @return the preferred transport, or null if none configured
     */
    @JsonIgnore
    public Transport getPreferredTransport() {
        if (transports == null || transports.isEmpty()) {
            return null;
        }
        return transports.get(0);
    }

    /**
     * Checks if this request specifies a particular amount.
     *
     * @return true if amount is set
     */
    @JsonIgnore
    public boolean hasAmount() {
        return amount != null;
    }

    /**
     * Checks if this request is single-use.
     *
     * @return true if single-use flag is set to true
     */
    @JsonIgnore
    public boolean isSingleUseRequest() {
        return Boolean.TRUE.equals(singleUse);
    }

    /**
     * Checks if this request has NUT-10 locking conditions.
     *
     * @return true if nut10Option is set
     */
    @JsonIgnore
    public boolean hasNut10Locking() {
        return nut10Option != null;
    }

    /**
     * Checks if a given mint URL is permitted by this request.
     *
     * @param mintUrl the mint URL to check
     * @return true if the mint is permitted or no mints are specified
     */
    public boolean isMintPermitted(@NonNull String mintUrl) {
        if (mints == null || mints.isEmpty()) {
            return true;
        }
        String normalizedUrl = mintUrl.replaceAll("/+$", "");
        return mints.stream()
                .map(m -> m.replaceAll("/+$", ""))
                .anyMatch(m -> m.equalsIgnoreCase(normalizedUrl));
    }
}
