package xyz.tcheeric.cashu.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.WellKnownSecret;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for converting between generic secret representations and
 * {@link Secret} implementations.
 */
public final class SecretUtil<T extends Secret> {

    private static final ObjectMapper MAPPER = JsonUtils.JSON_MAPPER;

    public SecretUtil() {
    }

    /**
     * Convert a generic representation of a secret into a {@link Secret}.
     * <p>
     * The input may be one of the following:
     * <ul>
     *     <li>a {@link String} representing a {@link RandomStringSecret}</li>
     *     <li>a {@link java.util.Map} describing a {@link WellKnownSecret} with a "kind" field</li>
     *     <li>a {@link java.util.List} of the form {@code [kind, { ... }]} where the second element
     *         contains the fields of a {@link WellKnownSecret}</li>
     * </ul>
     * </p>
     *
     * @param value the serialized secret
     * @return the deserialised secret implementation
     */
    @SuppressWarnings("unchecked")
    public static <T extends Secret> T toSecret(@NonNull Object value) {
        if (value instanceof Secret secret) {
            return (T) secret;
        }
        if (value instanceof String str) {
            return (T) RandomStringSecret.fromString(str);
        }
        if (value instanceof Map<?, ?> map) {
            return mapToSecret(map);
        }
        if (value instanceof List<?> list) {
            return listToSecret(list);
        }
        throw new IllegalArgumentException("Unknown secret type");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Secret> T listToSecret(List<?> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Unknown secret type");
        }
        String kind = String.valueOf(list.get(0));
        Object second = list.size() > 1 ? list.get(1) : Map.of();
        if (!(second instanceof Map<?, ?> data)) {
            throw new IllegalArgumentException("Unknown secret type");
        }
        Map<String, Object> map = new HashMap<>();
        data.forEach((k, v) -> map.put(String.valueOf(k), v));
        map.put("kind", kind);
        return (T) MAPPER.convertValue(map, WellKnownSecret.class);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Secret> T mapToSecret(Map<?, ?> src) {
        Map<String, Object> map = new HashMap<>();
        src.forEach((k, v) -> map.put(String.valueOf(k), v));
        if (!map.containsKey("kind")) {
            throw new IllegalArgumentException("Unknown secret type");
        }
        return (T) MAPPER.convertValue(map, WellKnownSecret.class);
    }
}

