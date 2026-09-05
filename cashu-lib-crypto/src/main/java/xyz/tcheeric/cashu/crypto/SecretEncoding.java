package xyz.tcheeric.cashu.crypto;

import lombok.NonNull;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The byte encodings under which a Cashu secret string can be fed to {@code hash_to_curve}.
 *
 * <p>NUT-00 defines {@code Y = hash_to_curve(x)} over the UTF-8 bytes of the secret message
 * {@code x}. Before this library adopted that reading it hex-decoded any secret that was not a
 * NUT-10 well-known secret, which produced a different curve point and therefore proofs that no
 * other implementation could verify.
 *
 * <p>Correcting the encoding invalidates every proof this library has already issued, so the two
 * encodings coexist: {@link #SPEC} is the only encoding used to <em>issue</em> a proof, while
 * {@link #verificationOrder()} is the ordered list of encodings tried when <em>verifying</em> one.
 * A proof issued under either encoding therefore keeps verifying, and no new proof is ever issued
 * under the legacy encoding.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/00.md">NUT-00</a>
 * @see BDHKEUtils
 */
public enum SecretEncoding {

    /**
     * The NUT-00 encoding: the UTF-8 bytes of the secret string, whatever the secret looks like.
     */
    SPEC {
        @Override
        public byte[] encode(@NonNull String secret) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    },

    /**
     * The pre-migration encoding: a hex secret was decoded to its 32 raw bytes, while a NUT-10
     * well-known secret (a JSON array) was already UTF-8 encoded.
     *
     * <p>Retained solely so that proofs issued before the migration keep verifying. Never use it
     * to issue a proof.
     */
    LEGACY_HEX {
        @Override
        public byte[] encode(@NonNull String secret) {
            return isWellKnownSecret(secret)
                    ? secret.getBytes(StandardCharsets.UTF_8)
                    : Utils.hexStringToBytes(secret);
        }

        @Override
        public boolean supports(@NonNull String secret) {
            return isWellKnownSecret(secret) || isHex(secret);
        }
    };

    /**
     * Whether proofs issued under the pre-migration encoding are still accepted.
     *
     * <p>The legacy encoding widens the set of secrets that verify (audit L-12): a secret is
     * tried under both encodings, so two distinct byte strings map to two distinct Y values that
     * both belong to one proof. That is the price of not invalidating proofs issued before the
     * encoding was corrected, and it is the right trade while such proofs are still in
     * circulation.
     *
     * <p>It should not be the price forever. Once a mint is satisfied that no pre-migration
     * proofs remain unspent, setting {@code cashu.secret.legacy-encoding.enabled=false} (or the
     * {@code CASHU_LEGACY_SECRET_ENCODING} environment variable) narrows verification to the
     * spec encoding alone. Read once at class initialisation, because the answer must not change
     * between the two halves of a swap.
     */
    private static final boolean LEGACY_ENABLED = legacyEncodingEnabled();

    private static final List<SecretEncoding> VERIFICATION_ORDER =
            LEGACY_ENABLED ? List.of(SPEC, LEGACY_HEX) : List.of(SPEC);

    private static boolean legacyEncodingEnabled() {
        String property = System.getProperty("cashu.secret.legacy-encoding.enabled");
        if (property != null && !property.isBlank()) {
            return Boolean.parseBoolean(property);
        }
        String env = System.getenv("CASHU_LEGACY_SECRET_ENCODING");
        if (env != null && !env.isBlank()) {
            return Boolean.parseBoolean(env);
        }
        // Default on: turning it off invalidates proofs that are still legitimately spendable,
        // which is a decision only the operator can make.
        return true;
    }

    /**
     * Encodes the secret string into the bytes fed to {@code hash_to_curve}.
     *
     * @throws IllegalArgumentException if the secret cannot be encoded this way
     */
    public abstract byte[] encode(@NonNull String secret);

    /**
     * Answers whether this encoding can be applied to the given secret at all.
     */
    public boolean supports(@NonNull String secret) {
        return !secret.isEmpty();
    }

    /**
     * The single encoding used when issuing a proof. Issuance is never ambiguous.
     */
    public static SecretEncoding forIssuance() {
        return SPEC;
    }

    /**
     * The encodings tried, in order, when verifying an already-issued proof. The spec encoding is
     * tried first so that the legacy encoding decays into a pure compatibility fallback.
     */
    public static List<SecretEncoding> verificationOrder() {
        return VERIFICATION_ORDER;
    }

    private static boolean isWellKnownSecret(String secret) {
        return secret.startsWith("[");
    }

    private static boolean isHex(String secret) {
        if (secret.isEmpty() || secret.length() % 2 != 0) {
            return false;
        }
        return secret.chars().allMatch(character -> Character.digit(character, 16) != -1);
    }
}
