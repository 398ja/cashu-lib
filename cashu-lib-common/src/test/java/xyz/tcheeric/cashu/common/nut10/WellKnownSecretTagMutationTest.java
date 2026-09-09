package xyz.tcheeric.cashu.common.nut10;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A secret's rendered form must never disagree with its contents.
 *
 * <p>{@link WellKnownSecret} caches the exact string it was parsed from, so that hashing and
 * signing use the bytes that actually arrived rather than a re-encoding. Every mutator on the
 * secret discards that cache. Tags, though, are reachable via {@code getTags()} and were
 * independently mutable (audit L-7), so changing a tag left {@code toString()} returning a
 * string that no longer described the object.
 *
 * <p>That matters because {@code toString()} is what {@code hash_to_curve} consumes: a secret
 * whose identity (Y) and content disagree is one the mint and the wallet would compute
 * differently.
 */
@DisplayName("Mutating a tag invalidates the cached wire string")
class WellKnownSecretTagMutationTest {

    private static final String PUBKEY =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    private static String wire(String tags) {
        return "[\"P2PK\",{\"nonce\":\"" + "aa".repeat(16)
                + "\",\"data\":\"" + PUBKEY + "\",\"tags\":" + tags + "}]";
    }

    @Test
    @DisplayName("addValue on a tag reached through getTags() invalidates the cache")
    void addValueInvalidates() {
        Secret parsed = SecretUtil.toSecret(wire("[[\"sigflag\",\"SIG_INPUTS\"]]"));
        WellKnownSecret secret = (WellKnownSecret) parsed;
        String before = secret.toString();

        secret.getTags().get(0).addValue("SIG_ALL");

        assertThat(secret.toString())
                .as("the rendered secret must reflect the mutation, not the stale parsed string")
                .isNotEqualTo(before);
    }

    @Test
    @DisplayName("setValues on a tag reached through getTags() invalidates the cache")
    void setValuesInvalidates() {
        Secret parsed = SecretUtil.toSecret(wire("[[\"sigflag\",\"SIG_INPUTS\"]]"));
        WellKnownSecret secret = (WellKnownSecret) parsed;
        String before = secret.toString();

        secret.getTags().get(0).setValues(List.of("SIG_ALL"));

        assertThat(secret.toString()).isNotEqualTo(before);
    }

    @Test
    @DisplayName("setKey on a tag reached through getTags() invalidates the cache")
    void setKeyInvalidates() {
        Secret parsed = SecretUtil.toSecret(wire("[[\"sigflag\",\"SIG_INPUTS\"]]"));
        WellKnownSecret secret = (WellKnownSecret) parsed;
        String before = secret.toString();

        secret.getTags().get(0).setKey("locktime");

        assertThat(secret.toString()).isNotEqualTo(before);
    }

    @Test
    @DisplayName("an untouched parsed secret still renders byte-for-byte as it arrived")
    void untouchedSecretIsVerbatim() {
        String original = wire("[[\"sigflag\",\"SIG_INPUTS\"]]");

        Secret parsed = SecretUtil.toSecret(original);

        assertThat(parsed.toString())
                .as("the whole point of the cache: an untouched secret hashes to the same Y the "
                        + "sender computed, whatever this library's own encoding would produce")
                .isEqualTo(original);
    }

    @Test
    @DisplayName("a tag detached from its secret no longer invalidates it")
    void removedTagDoesNotInvalidate() {
        Secret parsed = SecretUtil.toSecret(wire("[[\"sigflag\",\"SIG_INPUTS\"]]"));
        WellKnownSecret secret = (WellKnownSecret) parsed;
        WellKnownSecret.Tag tag = secret.getTags().get(0);
        secret.removeTag(tag);
        String afterRemoval = secret.toString();

        tag.addValue("SIG_ALL");

        assertThat(secret.toString())
                .as("a tag that is no longer part of the secret must not affect it")
                .isEqualTo(afterRemoval);
    }
}
