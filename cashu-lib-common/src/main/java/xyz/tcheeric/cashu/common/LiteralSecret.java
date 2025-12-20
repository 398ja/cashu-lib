package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

/**
 * A Secret implementation that preserves the exact literal string representation.
 *
 * <p>This is used for input proofs in swap operations to ensure the secret string
 * is sent to the mint exactly as it appeared in the original token. This is critical
 * because the mint verifies proofs by computing Y = hash_to_curve(UTF-8(secret_string)),
 * and any difference in the string (including case changes in hex encoding) will
 * cause verification to fail.
 *
 * <p>This class prevents the hex case normalization bug where:
 * <ol>
 *   <li>Original secret: ["VOUCHER","AABBCC","nonce",[]]</li>
 *   <li>After parse/re-serialize: ["VOUCHER","aabbcc","nonce",[]]</li>
 *   <li>Verification fails because the strings differ</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * // Preserve exact secret from a token proof
 * String secretFromToken = tokenProof.getSecret();
 * Secret secret = LiteralSecret.of(secretFromToken);
 *
 * // When serialized, returns exact same string
 * assert secretFromToken.equals(secret.toString());
 * }</pre>
 */
@RequiredArgsConstructor
public final class LiteralSecret implements Secret {

    @NonNull
    private final String literal;

    /**
     * Creates a LiteralSecret from the exact secret string.
     *
     * @param secretString the secret string exactly as it appears in the token
     * @return a Secret that will serialize to the exact same string
     */
    public static LiteralSecret of(@NonNull String secretString) {
        return new LiteralSecret(secretString);
    }

    @Override
    public byte[] getData() {
        return literal.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void setData(@NonNull byte[] data) {
        // Immutable - ignore
    }

    @Override
    public byte[] toBytes() {
        return getData();
    }

    /**
     * Returns the exact literal string.
     * This is used by Jackson for serialization via @JsonValue.
     */
    @JsonValue
    @Override
    public String toString() {
        return literal;
    }
}
