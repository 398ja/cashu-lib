package xyz.tcheeric.cashu.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

/**
 * Shared ObjectMapper instances for JSON and CBOR.
 */
public final class JsonUtils {

    /** Mapper configured for JSON serialization/deserialization. */
    public static final ObjectMapper JSON_MAPPER;

    /** Mapper configured for CBOR serialization/deserialization. */
    public static final ObjectMapper CBOR_MAPPER;

    static {
        JSON_MAPPER = new ObjectMapper();
        JSON_MAPPER.findAndRegisterModules();

        CBOR_MAPPER = new ObjectMapper(new CBORFactory());
        CBOR_MAPPER.findAndRegisterModules();
    }

    private JsonUtils() {
    }
}
