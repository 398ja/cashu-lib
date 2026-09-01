package xyz.tcheeric.cashu.common;

import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;
import xyz.tcheeric.cashu.crypto.SecretEncoding;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a NUT-00 secret string is allowed to be, and which bytes it stores.
 */
class RandomStringSecretTest {

    private static final String PLAIN_TEXT_SECRET = "not hex at all";
    private static final String HEX_SECRET =
            "9a1f293253e41e9a1f293253e41e9a1f293253e41e9a1f293253e41e9a1f2932";

    /**
     * Ensures a secret that is not hex is accepted, since NUT-00 only recommends hex.
     */
    @Test
    void shouldKeepSecretVerbatimWhenItIsNotHex() {
        // Act
        RandomStringSecret secret = RandomStringSecret.fromString(PLAIN_TEXT_SECRET);

        // Assert
        assertThat(secret.toString()).isEqualTo(PLAIN_TEXT_SECRET);
    }

    /**
     * Ensures a hex secret survives a round trip without being decoded and re-encoded.
     */
    @Test
    void shouldKeepSecretVerbatimWhenItIsHex() {
        // Act
        RandomStringSecret secret = RandomStringSecret.fromString(HEX_SECRET);

        // Assert
        assertThat(secret.toString()).isEqualTo(HEX_SECRET);
    }

    /**
     * Ensures an uppercase hex secret is not normalised, since the mint hashes the exact string.
     */
    @Test
    void shouldPreserveCaseWhenSecretIsUppercaseHex() {
        // Arrange
        String uppercase = HEX_SECRET.toUpperCase();

        // Act
        RandomStringSecret secret = RandomStringSecret.fromString(uppercase);

        // Assert
        assertThat(secret.toString()).isEqualTo(uppercase);
    }

    /**
     * Ensures the stored bytes are the ones hash_to_curve consumes, matching SecretEncoding.SPEC.
     */
    @Test
    void shouldStoreTheBytesSpecEncodingHashes() {
        // Arrange
        RandomStringSecret secret = RandomStringSecret.fromString(HEX_SECRET);

        // Act
        byte[] stored = secret.getData();

        // Assert
        assertThat(stored).isEqualTo(SecretEncoding.SPEC.encode(HEX_SECRET));
        assertThat(stored).isEqualTo(HEX_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Ensures Y derived from the stored bytes equals Y derived from the secret string.
     */
    @Test
    void shouldDeriveTheSameCurvePointFromStoredBytesAsFromTheSecretString() {
        // Arrange
        RandomStringSecret secret = RandomStringSecret.fromString(PLAIN_TEXT_SECRET);

        // Act
        byte[] fromBytes = BDHKEUtils.hashToCurve(secret.getData()).getEncoded(true);
        byte[] fromString = BDHKEUtils.hashToCurve(secret.toString());

        // Assert
        assertThat(fromBytes).isEqualTo(fromString);
    }

    /**
     * Ensures a generated secret is the 64-character hex string NUT-00 recommends.
     */
    @Test
    void shouldGenerateSixtyFourCharacterHexWhenCreated() {
        // Act
        RandomStringSecret secret = RandomStringSecret.create();

        // Assert
        assertThat(secret.toString()).hasSize(64).matches("[0-9a-f]{64}");
    }

    /**
     * Ensures a proof carrying a plain-text secret deserializes instead of failing on hex decoding.
     */
    @Test
    void shouldDeserializeProofWhenSecretIsNotHex() throws Exception {
        // Arrange
        String json = "{\"amount\":1,\"secret\":\"" + PLAIN_TEXT_SECRET
                + "\",\"id\":\"009a1f293253e41e\",\"C\":\"c\"}";

        // Act
        Proof<?> proof = JsonUtils.JSON_MAPPER.readValue(json, Proof.class);

        // Assert
        assertThat(proof.getSecret().toString()).isEqualTo(PLAIN_TEXT_SECRET);
    }

    /**
     * Ensures a proof's secret is re-serialized as the exact string it arrived as.
     */
    @Test
    void shouldSerializeSecretBackToTheStringItArrivedAs() throws Exception {
        // Arrange
        String json = "{\"amount\":1,\"secret\":\"" + PLAIN_TEXT_SECRET
                + "\",\"id\":\"009a1f293253e41e\",\"C\":\"c\"}";
        Proof<?> proof = JsonUtils.JSON_MAPPER.readValue(json, Proof.class);

        // Act
        String reserialized = JsonUtils.JSON_MAPPER.writeValueAsString(proof);

        // Assert
        assertThat(reserialized).contains("\"secret\":\"" + PLAIN_TEXT_SECRET + "\"");
    }

    /**
     * Ensures a proof issued under the pre-ADR-0001 hex encoding still verifies once its secret has
     * been round-tripped through this class, so legacy proofs stay spendable.
     */
    @Test
    void shouldStillVerifyLegacyProofWhenSecretRoundTripsThroughThisClass() {
        // Arrange: issue C = k*Y under the legacy encoding, as an old mint would have
        BigInteger mintKey = BigInteger.valueOf(0x1234567);
        ECPoint legacyY = BDHKEUtils.hashToCurve(SecretEncoding.LEGACY_HEX.encode(HEX_SECRET));
        ECPoint specY = BDHKEUtils.hashToCurve(SecretEncoding.SPEC.encode(HEX_SECRET));
        assertThat(legacyY)
                .as("the test is only meaningful if the two encodings commit to different points")
                .isNotEqualTo(specY);
        ECPoint commitment = legacyY.multiply(mintKey);
        String secretOffTheWire = RandomStringSecret.fromString(HEX_SECRET).toString();

        // Act
        boolean verified = BDHKEUtils.verify(secretOffTheWire, mintKey, commitment);

        // Assert
        assertThat(verified).isTrue();
    }

    /**
     * Ensures a proof issued under the NUT-00 spec encoding verifies through the same path.
     */
    @Test
    void shouldVerifySpecProofWhenSecretRoundTripsThroughThisClass() {
        // Arrange
        BigInteger mintKey = BigInteger.valueOf(0x1234567);
        ECPoint specY = BDHKEUtils.hashToCurve(SecretEncoding.SPEC.encode(PLAIN_TEXT_SECRET));
        ECPoint commitment = specY.multiply(mintKey);
        String secretOffTheWire = RandomStringSecret.fromString(PLAIN_TEXT_SECRET).toString();

        // Act
        boolean verified = BDHKEUtils.verify(secretOffTheWire, mintKey, commitment);

        // Assert
        assertThat(verified).isTrue();
    }
}
