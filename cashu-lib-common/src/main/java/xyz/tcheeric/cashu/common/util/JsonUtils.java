package xyz.tcheeric.cashu.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

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

        CBORFactory cborFactory = CBORFactory.builder()
                .enable(CBORGenerator.Feature.WRITE_MINIMAL_INTS)
                .build();
        CBOR_MAPPER = new ObjectMapper(cborFactory);
        CBOR_MAPPER.findAndRegisterModules();
        CBOR_MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    private JsonUtils() {
    }
}
