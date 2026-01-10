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

import java.util.ArrayList;
import java.util.List;

/**
 * NUT-10 locking condition options for NUT-18 payment requests.
 *
 * <p>Specifies conditions that proofs in the payment must satisfy,
 * such as P2PK (Pay to Public Key) or HTLC (Hash Time-Locked Contract).
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/10.md">NUT-10 Specification</a>
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18 Specification</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"k", "d", "t"})
public class Nut10Option {

    @JsonProperty("k")
    private WellKnownSecret.Kind kind;

    @JsonProperty("d")
    private String data;

    @JsonProperty("t")
    @Builder.Default
    private List<List<String>> tags = new ArrayList<>();

    /**
     * Creates a P2PK locking condition.
     *
     * <p>The sender must create proofs locked to the specified public key.
     *
     * @param publicKeyHex the receiver's public key in hex format
     * @return a P2PK Nut10Option
     */
    public static Nut10Option forP2PK(@NonNull String publicKeyHex) {
        return Nut10Option.builder()
                .kind(WellKnownSecret.Kind.P2PK)
                .data(publicKeyHex)
                .build();
    }

    /**
     * Creates an HTLC locking condition.
     *
     * <p>The sender must create proofs locked to the specified hash.
     *
     * @param hashHex the hash in hex format (preimage required for spending)
     * @return an HTLC Nut10Option
     */
    public static Nut10Option forHTLC(@NonNull String hashHex) {
        return Nut10Option.builder()
                .kind(WellKnownSecret.Kind.HTLC)
                .data(hashHex)
                .build();
    }

    /**
     * Creates a Voucher locking condition.
     *
     * <p>The sender must create proofs using voucher secrets.
     *
     * @param voucherDataHex the voucher data in hex format
     * @return a Voucher Nut10Option
     */
    public static Nut10Option forVoucher(@NonNull String voucherDataHex) {
        return Nut10Option.builder()
                .kind(WellKnownSecret.Kind.VOUCHER)
                .data(voucherDataHex)
                .build();
    }

    /**
     * Adds a key-value tag to this option.
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
     * Checks if this option is for P2PK locking.
     *
     * @return true if kind is P2PK
     */
    @JsonIgnore
    public boolean isP2PK() {
        return WellKnownSecret.Kind.P2PK.equals(kind);
    }

    /**
     * Checks if this option is for HTLC locking.
     *
     * @return true if kind is HTLC
     */
    @JsonIgnore
    public boolean isHTLC() {
        return WellKnownSecret.Kind.HTLC.equals(kind);
    }

    /**
     * Checks if this option is for Voucher locking.
     *
     * @return true if kind is VOUCHER
     */
    @JsonIgnore
    public boolean isVoucher() {
        return WellKnownSecret.Kind.VOUCHER.equals(kind);
    }
}
