package xyz.tcheeric.cashu.common.nut17;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 request for NUT-17 WebSocket subscriptions.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class JsonRpcRequest {
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    private String method;

    private SubscriptionParams params;

    private String id;
}
