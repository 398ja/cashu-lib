package xyz.tcheeric.cashu.common.nut10;

import com.fasterxml.jackson.databind.JsonNode;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the NUT-10 secret encoding decided in ADR 0003.
 *
 * <p>Two properties are load-bearing and are tested separately because they fail independently: a
 * secret we <em>construct</em> must be encoded in the spec's two-element form, and a secret we
 * <em>receive</em> must never be re-encoded at all.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/10.md">NUT-10</a>
 */
class WellKnownSecretSerializationTest {

    /** NUT-11's basic-case secret, exactly as the spec prints it inside its proof. */
    private static final String PUBLISHED_SECRET =
            "[\"P2PK\",{\"nonce\":\"859d4935c4907062a6297cf4e663e2835d90d97ecdd510745d32f6816323a41f\","
                    + "\"data\":\"0249098aa8b9d2fbec49ff8598feb17b592b986e62319a4fa488a3dc36387157a7\","
                    + "\"tags\":[[\"sigflag\",\"SIG_INPUTS\"]]}]";

    /**
     * Y = hash_to_curve of the published secret string, as computed by a conformant implementation.
     * Quoted in cashu-lib#254.
     */
    private static final String PUBLISHED_Y =
            "02561ea0cc7fc3ea7c13ccdd070747daa574b27a95535e9ee036773641840fa2a9";

    private static final String LOCKING_KEY =
            "0249098aa8b9d2fbec49ff8598feb17b592b986e62319a4fa488a3dc36387157a7";

    /**
     * Ensures a secret we construct is encoded as NUT-10's two elements, the second an object,
     * rather than the flattened four-element array released up to 0.23.0.
     */
    @Test
    void shouldEncodeConstructedSecretAsKindAndConditionObject() throws Exception {
        // Arrange
        P2PKSecret secret = new P2PKSecret(Hex.decode(LOCKING_KEY));

        // Act
        JsonNode encoded = JsonUtils.JSON_MAPPER.readTree(secret.toString());

        // Assert
        assertThat(encoded.isArray()).isTrue();
        assertThat(encoded.size()).isEqualTo(2);
        assertThat(encoded.get(0).asText()).isEqualTo("P2PK");
        assertThat(encoded.get(1).isObject()).isTrue();
        assertThat(encoded.get(1).get("data").asText()).isEqualTo(LOCKING_KEY);
        assertThat(encoded.get(1).has("nonce")).isTrue();
        assertThat(encoded.get(1).get("tags").isArray()).isTrue();
    }

    /**
     * Ensures NUT-10's tag values are written as strings, since the spec allows nothing else in a
     * tag even where the value is conceptually an integer.
     */
    @Test
    void shouldEncodeIntegerTagValuesAsStrings() throws Exception {
        // Arrange
        P2PKSecret secret = new P2PKSecret(Hex.decode(LOCKING_KEY));
        secret.setLockTime(1689418329);

        // Act
        JsonNode tags = JsonUtils.JSON_MAPPER.readTree(secret.toString()).get(1).get("tags");

        // Assert
        JsonNode locktime = findTag(tags, "locktime");
        assertThat(locktime.get(1).isTextual()).isTrue();
        assertThat(locktime.get(1).asText()).isEqualTo("1689418329");
    }

    /**
     * Ensures a secret read off the wire is replayed byte for byte, which is what NUT-11 means by
     * signing the unescaped secret string.
     */
    @Test
    void shouldReplayTheExactStringAReceivedSecretArrivedAs() {
        // Arrange
        Secret secret = SecretUtil.toSecret(PUBLISHED_SECRET);

        // Act
        String replayed = secret.toString();

        // Assert
        assertThat(replayed).isEqualTo(PUBLISHED_SECRET);
    }

    /**
     * Ensures the double-spend key derived from a received secret matches the one a conformant
     * implementation derives, which is the defect reported in cashu-lib#254.
     */
    @Test
    void shouldDeriveThePublishedYFromAReceivedSecret() {
        // Arrange
        Secret secret = SecretUtil.toSecret(PUBLISHED_SECRET);

        // Act
        String y = SecretUtil.toY(secret);

        // Assert
        assertThat(y).isEqualTo(PUBLISHED_Y);
    }

    /**
     * Ensures a proof round-trips its secret unchanged, and as a JSON string rather than a nested
     * array, which is how NUT-00 defines the field.
     */
    @Test
    void shouldRoundTripProofSecretAsAnUnchangedJsonString() throws Exception {
        // Arrange
        String proofJson = "{\"amount\":1,\"id\":\"009a1f293253e41e\",\"secret\":"
                + JsonUtils.JSON_MAPPER.writeValueAsString(PUBLISHED_SECRET)
                + ",\"C\":\"02698c4e2b5f9534cd0687d87513c759790cf829aa5739184a3e3735471fbda904\"}";
        Proof<?> proof = JsonUtils.JSON_MAPPER.readValue(proofJson, Proof.class);

        // Act
        JsonNode reserialized = JsonUtils.JSON_MAPPER.readTree(
                JsonUtils.JSON_MAPPER.writeValueAsString(proof));

        // Assert
        assertThat(reserialized.get("secret").isTextual()).isTrue();
        assertThat(reserialized.get("secret").asText()).isEqualTo(PUBLISHED_SECRET);
    }

    /**
     * Ensures a secret still parses from the flattened form released up to 0.23.0, so proofs
     * already issued under it stay readable.
     */
    @Test
    void shouldStillParseTheFlattenedFormEmittedBeforeThisChange() {
        // Arrange
        String flattened = "[\"P2PK\",\"" + LOCKING_KEY + "\",\"abc\",[[\"sigflag\",\"SIG_INPUTS\"]]]";

        // Act
        Secret secret = SecretUtil.toSecret(flattened);

        // Assert
        assertThat(secret).isInstanceOf(P2PKSecret.class);
        assertThat(((P2PKSecret) secret).getNonce()).isEqualTo("abc");
        assertThat(((P2PKSecret) secret).getSigFlag()).isEqualTo("SIG_INPUTS");
    }

    /**
     * Ensures a proof issued under the flattened form keeps its original Y, so this change does not
     * silently orphan it: the remembered wire string, not the new encoding, is what gets hashed.
     */
    @Test
    void shouldKeepTheOriginalYWhenAFlattenedSecretIsRead() {
        // Arrange
        String flattened = "[\"P2PK\",\"" + LOCKING_KEY + "\",\"abc\",[[\"sigflag\",\"SIG_INPUTS\"]]]";

        // Act
        String y = SecretUtil.toY(SecretUtil.toSecret(flattened));

        // Assert
        assertThat(y).isEqualTo(SecretUtil.toYFromString(flattened));
    }

    /**
     * Ensures mutating a received secret discards the remembered string, since replaying it would
     * describe a secret the object no longer holds.
     */
    @Test
    void shouldStopReplayingTheWireStringOnceTheSecretIsMutated() {
        // Arrange
        P2PKSecret secret = SecretUtil.toSecret(PUBLISHED_SECRET);

        // Act
        secret.setNonce("a-different-nonce");

        // Assert
        assertThat(secret.toString()).isNotEqualTo(PUBLISHED_SECRET);
        assertThat(secret.toString()).contains("a-different-nonce");
    }

    private static JsonNode findTag(JsonNode tags, String key) {
        for (JsonNode tag : tags) {
            if (key.equals(tag.get(0).asText())) {
                return tag;
            }
        }
        throw new AssertionError("No tag named " + key + " in " + tags);
    }
}
