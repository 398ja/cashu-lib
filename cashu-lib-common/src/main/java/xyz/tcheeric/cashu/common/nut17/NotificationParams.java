package xyz.tcheeric.cashu.common.nut17;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parameters for NUT-17 notification messages.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class NotificationParams {
    /**
     * The subscription ID that triggered this notification.
     */
    @JsonProperty("subId")
    private String subId;

    /**
     * The notification payload (state-specific data).
     */
    private Object payload;
}
