package xyz.tcheeric.cashu.common.nut17;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 error object.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class JsonRpcError {
    /**
     * Standard JSON-RPC error codes.
     */
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    private int code;
    private String message;
}
