package xyz.tcheeric.cashu.common.nut17;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 response for NUT-17 WebSocket subscriptions.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    private Object result;

    private JsonRpcError error;

    private String id;

    /**
     * Creates a successful response.
     */
    public static JsonRpcResponse success(String id, Object result) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setResult(result);
        return response;
    }

    /**
     * Creates an error response.
     */
    public static JsonRpcResponse error(String id, int code, String message) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setError(new JsonRpcError(code, message));
        return response;
    }
}
