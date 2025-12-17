package xyz.tcheeric.cashu.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.cashu.common.CompressedPublicKey;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.UnCompressedPublicKey;
import xyz.tcheeric.cashu.common.WellKnownSecret;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for converting between generic secret representations and
 * {@link Secret} implementations.
 */
@Slf4j
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
            // Check if the string is a NUT-10 JSON array (e.g., ["VOUCHER","data","nonce",[]])
            String trimmed = str.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                try {
                    // Parse as JSON array and convert to WellKnownSecret
                    List<?> list = MAPPER.readValue(trimmed, new TypeReference<List<?>>() {});
                    return listToSecret(list);
                } catch (Exception e) {
                    log.debug("secret_util to_secret json_parse_failed secret_preview={} error={}",
                            trimmed.length() > 40 ? trimmed.substring(0, 40) + "..." : trimmed,
                            e.getMessage());
                    // Fall through to treat as hex string
                }
            }
            // Treat as random hex string (NUT-00)
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

    /**
     * Computes Y = hash_to_curve(secret_string) per Cashu spec.
     * <p>
     * The secret string is the UTF-8 encoding of {@code secret.toString()}, which:
     * <ul>
     *   <li>For RandomStringSecret: returns 64-char hex string of the random bytes</li>
     *   <li>For WellKnownSecret: returns JSON array like ["VOUCHER","hexdata","nonce",[]]</li>
     * </ul>
     *
     * @param secret the secret
     * @return hex-encoded Y point on secp256k1 curve
     */
    public static <T extends Secret> String toY(@NonNull T secret) {
        // Per Cashu spec: Y = hash_to_curve(secret_string)
        // The secret_string is the UTF-8 representation of the secret
        byte[] secretStringBytes = secret.toString().getBytes(StandardCharsets.UTF_8);
        return PublicKey.fromPoint(
                BDHKEUtils.hashToCurve(secretStringBytes),
                true).toString();
    }

    /**
     * Alternative toY that takes the raw secret string directly.
     * Use this when you have the secret string from a Proof (which is already the string representation).
     *
     * @param secretString the secret string (e.g., "64charhex" or "[\"VOUCHER\",\"data\",\"nonce\",[]]")
     * @return hex-encoded Y point on secp256k1 curve
     */
    public static String toYFromString(@NonNull String secretString) {
        byte[] secretStringBytes = secretString.getBytes(StandardCharsets.UTF_8);
        return PublicKey.fromPoint(
                BDHKEUtils.hashToCurve(secretStringBytes),
                true).toString();
    }

    /**
     * Converts a JSON array to a WellKnownSecret.
     * <p>
     * Supports two formats:
     * <ol>
     *   <li>Legacy format: ["KIND", {"nonce": "...", "data": "...", "tags": [...]}]</li>
     *   <li>NUT-10 format: ["KIND", "hexdata", "nonce", [[tag_arrays]]]</li>
     * </ol>
     */
    @SuppressWarnings("unchecked")
    private static <T extends Secret> T listToSecret(List<?> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Empty list cannot be converted to secret");
        }
        String kind = String.valueOf(list.get(0));
        Object second = list.size() > 1 ? list.get(1) : null;

        // Check if this is legacy format (second element is a Map)
        if (second instanceof Map<?, ?> data) {
            // Legacy format: ["KIND", {nonce: ..., data: ..., tags: [...]}]
            Map<String, Object> map = new HashMap<>();
            data.forEach((k, v) -> map.put(String.valueOf(k), v));
            map.put("kind", kind);
            return (T) MAPPER.convertValue(map, WellKnownSecret.class);
        }

        // NUT-10 format: ["KIND", "hexdata", "nonce", [tags]]
        // Element 1 is hex-encoded data string
        String hexData = second != null ? String.valueOf(second) : "";
        String nonce = list.size() > 2 ? String.valueOf(list.get(2)) : "";
        List<?> tags = list.size() > 3 && list.get(3) instanceof List<?> ? (List<?>) list.get(3) : List.of();

        // Convert to WellKnownSecret format for Jackson
        Map<String, Object> map = new HashMap<>();
        map.put("kind", kind);
        map.put("nonce", nonce);
        // Data is hex-encoded in NUT-10, decode to bytes for WellKnownSecret.data
        try {
            byte[] data = org.bouncycastle.util.encoders.Hex.decode(hexData);
            map.put("data", data);
        } catch (Exception e) {
            log.warn("secret_util list_to_secret hex_decode_failed kind={} error={}", kind, e.getMessage());
            map.put("data", new byte[0]);
        }
        map.put("tags", tags);

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

