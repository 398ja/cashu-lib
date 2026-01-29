package xyz.tcheeric.cashu.common.nut17;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result returned when a subscription is successfully created.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubscriptionResult {
    /**
     * The subscription result status.
     */
    private String status;

    /**
     * The assigned subscription ID.
     */
    @JsonProperty("subId")
    private String subId;

    /**
     * Creates a successful subscription result.
     */
    public static SubscriptionResult ok(String subId) {
        SubscriptionResult result = new SubscriptionResult();
        result.setStatus("OK");
        result.setSubId(subId);
        return result;
    }

    /**
     * Creates an unsubscribe result.
     */
    public static SubscriptionResult unsubscribed(String subId) {
        SubscriptionResult result = new SubscriptionResult();
        result.setStatus("OK");
        result.setSubId(subId);
        return result;
    }
}
