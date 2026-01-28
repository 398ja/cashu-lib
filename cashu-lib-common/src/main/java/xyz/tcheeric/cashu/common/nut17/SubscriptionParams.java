package xyz.tcheeric.cashu.common.nut17;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Parameters for NUT-17 subscribe/unsubscribe requests.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubscriptionParams {
    /**
     * The subscription kind (bolt11_mint_quote, bolt11_melt_quote, proof_state).
     */
    private SubscriptionKind kind;

    /**
     * Filters for the subscription. Each filter contains a list of IDs to match.
     */
    private List<SubscriptionFilter> filters;

    /**
     * Subscription ID for unsubscribe requests.
     */
    @JsonProperty("subId")
    private String subId;
}
