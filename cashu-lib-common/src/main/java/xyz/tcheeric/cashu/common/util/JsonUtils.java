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
        // Deliberately do NOT findAndRegisterModules() on the CBOR mapper. CBOR is used
        // only for wire tokens/payment-requests (TokenV4, PaymentRequest, VoucherPaymentRequest),
        // whose fields are simple String/byte[]/Integer/List — no module is needed. Auto-registering
        // whatever Jackson modules happen to be on the *consumer's* classpath makes CBOR decoding
        // non-deterministic across services: e.g. jackson-module-blackbird (pulled transitively by
        // Spring Boot in the gateways/wallet-lib) mis-deserializes definite-length CBOR maps and
        // silently drops fields (observed: TokenV4.unit decoded as null). Keeping the CBOR mapper
        // module-free guarantees identical decode behaviour everywhere. (JSON_MAPPER, used for
        // richer types, keeps findAndRegisterModules above.)
        CBOR_MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    private JsonUtils() {
    }
}
