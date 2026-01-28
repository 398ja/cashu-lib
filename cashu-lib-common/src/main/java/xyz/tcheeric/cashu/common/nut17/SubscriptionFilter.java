package xyz.tcheeric.cashu.common.nut17;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Filter for NUT-17 subscriptions. Contains IDs to match for notifications.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubscriptionFilter {
    /**
     * List of IDs to match. For proof_state subscriptions, these are proof secrets (Y values).
     * For quote subscriptions, these are quote IDs.
     */
    private List<String> ids;
}
