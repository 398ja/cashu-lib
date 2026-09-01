package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * One entry of the NUT-02 {@code GET /v1/keysets} listing.
 *
 * <p>The listing is the only endpoint that names every keyset, active or not, so it carries the
 * {@code input_fee_ppk} a wallet needs to compute transaction fees, and the optional
 * {@code final_expiry} after which the keyset's proofs are no longer accepted.
 */
@Data
@NoArgsConstructor
@JsonPropertyOrder({"id", "unit", "active", "input_fee_ppk", "final_expiry"})
public class ActiveKeySet {

    @JsonProperty
    private String id;

    @JsonProperty
    private String unit;

    @JsonProperty
    private boolean active;

    @JsonProperty("input_fee_ppk")
    private int inputFeePpk;

    /**
     * Unix timestamp after which this keyset's proofs are refused, or null when it never expires.
     */
    @JsonProperty("final_expiry")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long finalExpiry;

    public static ActiveKeySet fromKeySet(@NonNull KeySet keySet, boolean active) {
        return fromKeySet(keySet, active, null);
    }

    public static ActiveKeySet fromKeySet(@NonNull KeySet keySet, boolean active, Long finalExpiry) {
        ActiveKeySet activeKeySet = new ActiveKeySet();
        activeKeySet.setId(keySet.getId());
        activeKeySet.setUnit(keySet.getUnit());
        activeKeySet.setActive(active);
        activeKeySet.setInputFeePpk(keySet.getPartPerThousand());
        activeKeySet.setFinalExpiry(finalExpiry);
        return activeKeySet;
    }
}
