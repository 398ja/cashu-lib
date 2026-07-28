package xyz.tcheeric.cashu.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut11.MalformedP2PKSecretException;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;
import xyz.tcheeric.cashu.common.nut18.VoucherSecret;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;

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
                } catch (MalformedP2PKSecretException e) {
                    // MUST NOT fall through. The fall-through treats the input as a NUT-00 random
                    // string, which carries no spending condition at all - so a P2PK lock we just
                    // rejected as malformed would come back as a bearer secret, spendable by
                    // anyone holding the proof. Refusing to parse is the safe outcome; silently
                    // dropping the lock is strictly worse than the malformed lock itself.
                    throw e;
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
        byte[] y = BDHKEUtils.hashToCurve(secret.toString());
        return PublicKey.fromBytes(y).toString();
    }

    /**
     * Alternative toY that takes the raw secret string directly.
     * Use this when you have the secret string from a Proof (which is already the string representation).
     *
     * @param secretString the secret string (e.g., "64charhex" or "[\"VOUCHER\",\"data\",\"nonce\",[]]")
     * @return hex-encoded Y point on secp256k1 curve
     */
    public static String toYFromString(@NonNull String secretString) {
        byte[] y = BDHKEUtils.hashToCurve(secretString);
        return PublicKey.fromBytes(y).toString();
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
        String kindStr = String.valueOf(list.get(0));
        WellKnownSecret.Kind kind;
        try {
            kind = WellKnownSecret.Kind.valueOf(kindStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown secret kind: " + kindStr, e);
        }
        Object second = list.size() > 1 ? list.get(1) : null;

        // Check if this is legacy format (second element is a Map)
        if (second instanceof Map<?, ?> data) {
            // Legacy format: ["KIND", {nonce: ..., data: ..., tags: [...]}]
            return legacyMapToSecret(kind, data);
        }

        // NUT-10 format: ["KIND", "hexdata", "nonce", [tags]]
        // Element 1 is hex-encoded data string
        String hexData = second != null ? String.valueOf(second) : "";
        // Note: String.valueOf(null) returns "null" (string), so we must check explicitly
        Object nonceObj = list.size() > 2 ? list.get(2) : null;
        String nonce = nonceObj != null ? String.valueOf(nonceObj) : null;
        List<?> tags = list.size() > 3 && list.get(3) instanceof List<?> ? (List<?>) list.get(3) : List.of();

        // Hex-decode the data
        byte[] data;
        try {
            data = org.bouncycastle.util.encoders.Hex.decode(hexData);
        } catch (Exception e) {
            log.warn("secret_util list_to_secret hex_decode_failed kind={} error={}", kind, e.getMessage());
            data = new byte[0];
        }

        // Directly construct the WellKnownSecret subclass to avoid Jackson's
        // serialization/deserialization cycle which causes double hex-decode
        WellKnownSecret secret = createSecret(kind, data, nonce);
        addTagsToSecret(secret, tags, kind);
        return (T) validated(secret);
    }

    /**
     * NUT-11 enforcement point for this parse path.
     *
     * <p>{@code SecretUtil} reimplements secret construction rather than delegating to
     * {@code WellKnownSecretDeserializer} — see the comment above about avoiding Jackson's
     * double-hex-decode cycle — so it is a second, independent ingress for P2PK secrets and needs
     * its own validation. Validating in only one of the two would leave a malformed lock
     * reachable through the other.
     */
    private static WellKnownSecret validated(WellKnownSecret secret) {
        if (secret instanceof P2PKSecret p2pk) {
            p2pk.validate();
        }
        return secret;
    }

    /**
     * Creates the appropriate WellKnownSecret subclass.
     */
    private static WellKnownSecret createSecret(WellKnownSecret.Kind kind, byte[] data, String nonce) {
        return switch (kind) {
            case VOUCHER -> {
                VoucherSecret voucher = new VoucherSecret();
                voucher.setData(data);
                voucher.setNonce(nonce);
                yield voucher;
            }
            case P2PK -> {
                P2PKSecret p2pk = new P2PKSecret();
                p2pk.setData(data);
                p2pk.setNonce(nonce);
                yield p2pk;
            }
            default -> throw new IllegalArgumentException("Unsupported secret kind: " + kind);
        };
    }

    /**
     * Adds tags from a list of tag arrays to the secret.
     */
    private static void addTagsToSecret(WellKnownSecret secret, List<?> tags, WellKnownSecret.Kind kind) {
        for (Object tagObj : tags) {
            if (tagObj instanceof List<?> tagList && !tagList.isEmpty()) {
                String key = String.valueOf(tagList.get(0));
                WellKnownSecret.Tag tag = new WellKnownSecret.Tag(key);
                for (int i = 1; i < tagList.size(); i++) {
                    Object value = tagList.get(i);
                    if (value instanceof Number n) {
                        // Preserve fractional values for doubles/floats
                        if (value instanceof Double || value instanceof Float) {
                            double d = n.doubleValue();
                            // Validate: reject NaN and Infinity values
                            if (Double.isNaN(d) || Double.isInfinite(d)) {
                                throw new IllegalArgumentException(
                                        "Invalid floating-point value in tag: NaN or Infinity not allowed");
                            }
                            if (d != Math.floor(d)) {
                                tag.addValue(d);
                            } else {
                                tag.addValue(n.longValue());
                            }
                        } else {
                            tag.addValue(n.longValue());
                        }
                    } else {
                        tag.addValue(String.valueOf(value));
                    }
                }
                // Convert P2PK tag values to proper types
                if (kind == WellKnownSecret.Kind.P2PK) {
                    convertP2PKTagValues(tag);
                }
                secret.addTag(tag);
            }
        }
    }

    /**
     * Converts P2PK tag values to their proper types.
     */
    private static void convertP2PKTagValues(WellKnownSecret.Tag tag) {
        switch (tag.getKey()) {
            case "sigflag" -> {
                java.util.List<Object> values = new java.util.ArrayList<>();
                for (Object v : tag.getValues()) {
                    if (v instanceof String s) {
                        values.add(P2PKSecret.SignatureFlag.valueOf(s));
                    } else {
                        values.add(v);
                    }
                }
                tag.setValues(values);
            }
            case "n_sigs", "n_sigs_refund", "locktime" -> {
                java.util.List<Object> values = new java.util.ArrayList<>();
                for (Object v : tag.getValues()) {
                    if (v instanceof Number n) {
                        values.add(n.intValue());
                    } else {
                        values.add(v);
                    }
                }
                tag.setValues(values);
            }
        }
    }

    /**
     * Converts legacy format map to WellKnownSecret.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Secret> T legacyMapToSecret(WellKnownSecret.Kind kind, Map<?, ?> data) {
        // Note: Use null instead of empty string to preserve JSON null nonce
        Object nonceObj = data.get("nonce");
        String nonce = nonceObj != null ? String.valueOf(nonceObj) : null;
        Object dataObj = data.get("data");
        byte[] dataBytes;
        if (dataObj instanceof byte[]) {
            dataBytes = (byte[]) dataObj;
        } else if (dataObj instanceof String hexStr) {
            try {
                dataBytes = org.bouncycastle.util.encoders.Hex.decode(hexStr);
            } catch (Exception e) {
                log.warn("secret_util legacy_map_to_secret hex_decode_failed kind={} error={}", kind, e.getMessage());
                dataBytes = new byte[0];
            }
        } else {
            dataBytes = new byte[0];
        }

        WellKnownSecret secret = createSecret(kind, dataBytes, nonce);

        Object tagsObj = data.get("tags");
        if (tagsObj instanceof List<?> tags) {
            addTagsToSecret(secret, tags, kind);
        }
        return (T) validated(secret);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Secret> T mapToSecret(Map<?, ?> src) {
        Map<String, Object> map = new HashMap<>();
        src.forEach((k, v) -> map.put(String.valueOf(k), v));
        if (!map.containsKey("kind")) {
            throw new IllegalArgumentException("Unknown secret type");
        }
        String kindStr = String.valueOf(map.get("kind"));
        WellKnownSecret.Kind kind;
        try {
            kind = WellKnownSecret.Kind.valueOf(kindStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown secret kind: " + kindStr, e);
        }
        return legacyMapToSecret(kind, map);
    }
}
