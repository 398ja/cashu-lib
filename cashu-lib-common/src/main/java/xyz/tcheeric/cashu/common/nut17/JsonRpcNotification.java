package xyz.tcheeric.cashu.common.nut17;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 notification (server-initiated message without id).
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class JsonRpcNotification {
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    private String method = "notification";

    private NotificationParams params;

    /**
     * Creates a notification for a subscription event.
     */
    public static JsonRpcNotification of(String subId, Object payload) {
        JsonRpcNotification notification = new JsonRpcNotification();
        notification.setParams(new NotificationParams(subId, payload));
        return notification;
    }
}
