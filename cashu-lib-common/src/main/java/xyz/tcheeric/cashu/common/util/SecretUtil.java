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
import xyz.tcheeric.cashu.common.nut11.P2PKVoucherSecret;
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
                List<?> list;
                try {
                    list = MAPPER.readValue(trimmed, new TypeReference<List<?>>() {});
                } catch (Exception notJson) {
                    // Not actually JSON despite the brackets, so it never claimed to be a
                    // structured secret. A NUT-00 bearer string is the correct reading.
                    log.debug("secret_util to_secret json_parse_failed secret_preview={} error={}",
                            preview(trimmed), notJson.getMessage());
                    return (T) RandomStringSecret.fromString(str);
                }
                if (!claimsToBeStructured(list)) {
                    // A JSON array that is not of the form [kind, ...] is not a NUT-10 secret.
                    log.debug("secret_util to_secret not_structured secret_preview={}", preview(trimmed));
                    return (T) RandomStringSecret.fromString(str);
                }
                // From here the input declared a kind, so it IS a structured secret and any
                // failure to build it MUST propagate.
                //
                // Falling through to RandomStringSecret would hand back a NUT-00 bearer secret
                // carrying no spending condition at all: a P2PK or HTLC lock we could not parse
                // would come back spendable by anyone holding the proof, and `Y` is derived from
                // the same wire string so the mint would happily accept the swap. The sender, who
                // knows the secret string, could then spend behind the recipient's back. Refusing
                // to parse is the safe outcome; silently dropping the lock is strictly worse than
                // the malformed lock itself.
                try {
                    return rememberingWireString(listToSecret(list), str);
                } catch (MalformedP2PKSecretException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw new MalformedP2PKSecretException(
                            "Refusing to parse a structured secret that declares kind '"
                                    + declaredKind(list) + "': " + e.getMessage(), e);
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
     * Whether the parsed JSON array declares a NUT-10 secret kind, i.e. is of the form
     * {@code [kind, ...]} with a string first element.
     *
     * <p>This is deliberately a structural test rather than a test against the {@code Kind} enum:
     * an array declaring an unknown or future kind still claims to be a structured secret, and
     * must be rejected rather than silently downgraded to a bearer secret.
     */
    private static boolean claimsToBeStructured(List<?> list) {
        return list != null && !list.isEmpty() && list.get(0) instanceof String;
    }

    /**
     * The kind string a list declared, for diagnostics. Never includes secret material.
     */
    private static String declaredKind(List<?> list) {
        return claimsToBeStructured(list) ? String.valueOf(list.get(0)) : "<none>";
    }

    /**
     * A short, bounded excerpt of an unparsable secret for diagnostics.
     *
     * <p>16 characters, not 40 (audit L-6). A NUT-00 secret is 64 hex characters, so a 40-char
     * preview published nearly two thirds of it to the log; 16 is enough to correlate two log
     * lines about the same input and not enough to be worth harvesting.
     */
    private static String preview(String s) {
        return s.length() > 16 ? s.substring(0, 16) + "..." : s;
    }

    /**
     * Records the string a secret was parsed from, so that hashing and signing use the bytes that
     * actually arrived rather than this library's re-encoding of them.
     *
     * @return the same secret, for chaining
     */
    private static <T extends Secret> T rememberingWireString(T secret, String wireString) {
        if (secret instanceof WellKnownSecret wellKnown) {
            wellKnown.rememberWireString(wireString);
        }
        return secret;
    }

    /**
     * Computes {@code Y = hash_to_curve(secret_string)} per NUT-00.
     * <p>
     * The secret string is {@code secret.toString()}, which for a secret parsed off the wire is the
     * exact string that arrived, and for a constructed secret is its canonical encoding: verbatim
     * for a {@link RandomStringSecret}, and NUT-10's {@code [kind, {nonce, data, tags}]} for a
     * {@link WellKnownSecret}.
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
     *   <li>the NUT-10 form: ["KIND", {"nonce": "...", "data": "...", "tags": [...]}]</li>
     *   <li>the flattened form emitted up to 0.23.0: ["KIND", "hexdata", "nonce", [[tag_arrays]]]</li>
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

        if (second instanceof Map<?, ?> data) {
            // The NUT-10 form: ["KIND", {nonce: ..., data: ..., tags: [...]}]
            return conditionObjectToSecret(kind, data);
        }

        // The pre-0.24.0 flattened form: ["KIND", "hexdata", "nonce", [tags]]
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
            case P2PK_VOUCHER -> {
                // Absent until now, and silently so. This parse path is how a
                // MINT reads a proof off the wire, and an unsupported kind here
                // throws — after which the caller falls back to a condition
                // that checks no lock at all. So a P2PK_VOUCHER was accepted and
                // spent WITHOUT its witness: the exact failure the composite
                // kind exists to prevent, reached by the one route nobody
                // thought to add it to.
                //
                // Observed against a real mint before this fix: swapping a
                // locked proof with `witness=null` returned 200.
                P2PKVoucherSecret locked = new P2PKVoucherSecret();
                locked.setData(data);
                locked.setNonce(nonce);
                yield locked;
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
                        // Leave an unrecognised flag as the raw string rather than throwing here.
                        // P2PKSecret.validate() -> requireKnownSigFlag() is the enforcement point
                        // and raises a typed MalformedP2PKSecretException. Throwing a bare
                        // IllegalArgumentException from this conversion ran *before* validate()
                        // ever got a chance, and the caller's catch treated the whole secret as
                        // an unstructured bearer string, dropping the lock entirely.
                        values.add(parseSigFlagOrKeep(s));
                    } else {
                        values.add(v);
                    }
                }
                tag.setValues(values);
            }
            case "n_sigs", "n_sigs_refund" -> {
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
            case "locktime" -> {
                java.util.List<Object> values = new java.util.ArrayList<>();
                for (Object v : tag.getValues()) {
                    if (v instanceof Number n) {
                        // Keep the full width. Narrowing to int silently wrapped any timestamp
                        // past 2038-01-19 into a negative number, which every "has the locktime
                        // passed?" check then read as long expired, unlocking the proof.
                        values.add(n.longValue());
                    } else {
                        values.add(v);
                    }
                }
                tag.setValues(values);
            }
        }
    }

    /**
     * Parses a signature flag, returning the original string when it is not a known flag so that
     * {@code P2PKSecret.validate()} can reject it with a typed exception.
     */
    private static Object parseSigFlagOrKeep(String s) {
        try {
            return P2PKSecret.SignatureFlag.valueOf(s);
        } catch (IllegalArgumentException unknownFlag) {
            return s;
        }
    }

    /**
     * Converts NUT-10's second element, the spending-condition object, to a WellKnownSecret.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Secret> T conditionObjectToSecret(WellKnownSecret.Kind kind, Map<?, ?> data) {
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
                // Not empty bytes (audit M-11). `data` in a P2PK secret is the lock key, so
                // substituting an empty array turns "I could not read the lock" into "there is
                // no lock", which is the same class of silent downgrade as the bearer-secret
                // fallback in toSecret(). A secret whose data cannot be decoded is malformed.
                log.warn("secret_util condition_object_to_secret hex_decode_failed kind={} error={}",
                        kind, e.getMessage());
                throw new MalformedP2PKSecretException(
                        "secret data is not valid hex for kind " + kind, e);
            }
        } else {
            throw new MalformedP2PKSecretException(
                    "secret data must be a hex string for kind " + kind);
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
        return conditionObjectToSecret(kind, map);
    }
}
