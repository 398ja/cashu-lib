package xyz.tcheeric.cashu.common;

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
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * NUT-18V Voucher Payment Request for Model B gift card transactions.
 *
 * <p>Voucher payment requests extend NUT-18 with support for offline verification,
 * issuer identification, and merchant transport methods specific to gift card scenarios.
 *
 * <p>Encoding format: {@code vreqA + base64_urlsafe(CBOR(VoucherPaymentRequest))}
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
@JsonPropertyOrder({"i", "r", "a", "u", "s", "o", "m", "d", "t", "nut10"})
public class VoucherPaymentRequest {

    /** Prefix for encoded voucher payment requests. */
    public static final String REQUEST_PREFIX = "vreq";

    /** Version code for this voucher payment request format. */
    public static final String VERSION_CODE = "A";

    /** URI scheme for clickable voucher payment requests. */
    public static final String URI_SCHEME = "cashu:";

    /**
     * Unique payment request identifier.
     */
    @JsonProperty("i")
    private String paymentId;

    /**
     * Issuer identifier (required for voucher requests).
     * This identifies the voucher issuer for validation and redemption.
     */
    @JsonProperty("r")
    private String issuerId;

    /**
     * Requested amount in the specified unit.
     */
    @JsonProperty("a")
    private Integer amount;

    /**
     * Unit for the amount (e.g., "sat", "usd").
     */
    @JsonProperty("u")
    private String unit;

    /**
     * Whether this request can only be paid once.
     */
    @JsonProperty("s")
    private Boolean singleUse;

    /**
     * Offline verification support flag.
     * When true, the payment payload should include DLEQ proofs for offline verification.
     */
    @JsonProperty("o")
    private Boolean offlineVerification;

    /**
     * List of permitted mint URLs.
     * Empty or null means any mint is acceptable.
     */
    @JsonProperty("m")
    @Builder.Default
    private List<String> mints = new ArrayList<>();

    /**
     * Human-readable description of the payment request.
     */
    @JsonProperty("d")
    private String description;

    /**
     * Transport methods in order of preference (first = most preferred).
     */
    @JsonProperty("t")
    @Builder.Default
    private List<VoucherTransport> transports = new ArrayList<>();

    /**
     * NUT-10 locking conditions for the payment.
     */
    @JsonProperty("nut10")
    private Nut10Option nut10Option;

    /**
     * Validates this voucher payment request.
     *
     * @throws IllegalStateException if required fields are missing or invalid
     */
    public void validate() {
        if (issuerId == null || issuerId.isBlank()) {
            throw new IllegalStateException("Issuer ID (r) is required for voucher payment requests");
        }
        if (amount != null && (unit == null || unit.isBlank())) {
            throw new IllegalStateException(
                    "Unit is required when amount is specified. Amount: " + amount);
        }
    }

    /**
     * Serializes this voucher payment request to the NUT-18V encoded format.
     *
     * @param clickable if true, prepends the cashu: URI scheme for clickable links
     * @return the encoded voucher payment request string
     * @throws RuntimeException if serialization fails
     */
    public String serialize(boolean clickable) {
        try {
            validate();
            log.debug("Serializing VoucherPaymentRequest with id={}, issuerId={}", paymentId, issuerId);

            byte[] cborBytes = JsonUtils.CBOR_MAPPER.writeValueAsBytes(this);
            String encoded = REQUEST_PREFIX + VERSION_CODE +
                    Base64.getUrlEncoder().withoutPadding().encodeToString(cborBytes);

            return clickable ? URI_SCHEME + encoded : encoded;
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize VoucherPaymentRequest", e);
        }
    }

    /**
     * Serializes this voucher payment request without the URI scheme.
     *
     * @return the encoded voucher payment request string
     */
    public String serialize() {
        return serialize(false);
    }

    /**
     * Deserializes a voucher payment request from the NUT-18V encoded format.
     *
     * <p>Supports both URL-safe Base64 (RFC 4648 section 5) and standard Base64 encoding.
     *
     * @param serialized the encoded voucher payment request string
     * @return the deserialized VoucherPaymentRequest
     * @throws IllegalArgumentException if the format is invalid
     * @throws RuntimeException if deserialization fails
     */
    public static VoucherPaymentRequest deserialize(@NonNull String serialized) {
        if (serialized.startsWith(URI_SCHEME)) {
            serialized = serialized.substring(URI_SCHEME.length());
        }

        String expectedPrefix = REQUEST_PREFIX + VERSION_CODE;
        if (!serialized.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException(
                    "Invalid voucher payment request format. Expected prefix: " + expectedPrefix);
        }

        String base64Payload = serialized.substring(expectedPrefix.length());
        byte[] cborBytes = decodeBase64(base64Payload);

        try {
            VoucherPaymentRequest request = JsonUtils.CBOR_MAPPER.readValue(cborBytes, VoucherPaymentRequest.class);
            log.debug("Deserialized VoucherPaymentRequest with id={}, issuerId={}",
                    request.getPaymentId(), request.getIssuerId());
            return request;
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize VoucherPaymentRequest", e);
        }
    }

    /**
     * Decodes Base64 payload, supporting both URL-safe and standard encodings.
     *
     * @param base64Payload the Base64 encoded string
     * @return the decoded bytes
     */
    private static byte[] decodeBase64(String base64Payload) {
        if (base64Payload.contains("+") || base64Payload.contains("/")) {
            return Base64.getDecoder().decode(base64Payload);
        }
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
    public void addTransport(@NonNull VoucherTransport transport) {
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
    public VoucherTransport getPreferredTransport() {
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
     * Checks if this request requires offline verification.
     *
     * @return true if offline verification flag is set to true
     */
    @JsonIgnore
    public boolean requiresOfflineVerification() {
        return Boolean.TRUE.equals(offlineVerification);
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

    /**
     * Gets the first merchant transport if available.
     *
     * @return the first MERCHANT transport, or null if none
     */
    @JsonIgnore
    public VoucherTransport getMerchantTransport() {
        if (transports == null) {
            return null;
        }
        return transports.stream()
                .filter(VoucherTransport::isMerchant)
                .findFirst()
                .orElse(null);
    }
}
