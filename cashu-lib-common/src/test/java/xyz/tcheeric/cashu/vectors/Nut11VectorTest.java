package xyz.tcheeric.cashu.vectors;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.crypto.Schnorr;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NUT-11 vectors: P2PK secret parsing and the BIP-340 signatures that satisfy them.
 *
 * <p>Whether a given proof is <em>spendable</em> is a mint-side decision (locktime evaluation,
 * threshold counting across pathways, {@code SIG_ALL} message aggregation) and lives in
 * {@code cashu-mint}. What this library owns, and what these tests pin down, is that a published
 * secret parses into the tags the spec says it carries and that a published signature verifies
 * against the message the spec says was signed. The rest is recorded as uncovered in
 * {@code docs/reference/nut-test-vector-coverage.md}.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/tests/11-test.md">NUT-11 test vectors</a>
 */
class Nut11VectorTest {

    private static final VectorDocument VECTORS = VectorDocument.load("11-test.md");

    /** Every {@code json} block up to the first swap request holds a single standalone proof. */
    private static final int STANDALONE_PROOF_BLOCK_COUNT = 9;

    private static final int VALID_SIG_INPUTS_PROOF_BLOCK = 3;
    private static final int INVALID_SIG_INPUTS_PROOF_BLOCK = 4;

    private static final int SWAP_MESSAGE_BLOCK = 0;
    private static final int MELT_MESSAGE_BLOCK = 1;

    static Stream<ProofVector> standaloneProofVectors() {
        return Stream.iterate(0, index -> index + 1)
                .limit(STANDALONE_PROOF_BLOCK_COUNT)
                .map(index -> new ProofVector(index, VECTORS.block("json", index).text()));
    }

    static Stream<String> aggregatedMessageVectors() {
        return Stream.of(
                VECTORS.block("", SWAP_MESSAGE_BLOCK).text(),
                VECTORS.block("", MELT_MESSAGE_BLOCK).text());
    }

    /**
     * Ensures every published P2PK proof parses into a valid, well-formed NUT-11 secret.
     */
    @ParameterizedTest(name = "proof {0}")
    @MethodSource("standaloneProofVectors")
    void shouldParseAndValidateWhenProofCarriesPublishedSpendingCondition(ProofVector vector) {
        // Arrange
        Proof<Secret> proof = readProof(vector.getProofJson());

        // Act
        Secret secret = proof.getSecret();

        // Assert
        assertThat(secret).isInstanceOf(P2PKSecret.class);
        ((P2PKSecret) secret).validate();
    }

    /**
     * Ensures the signature published as valid verifies against its own secret.
     */
    @Test
    void shouldVerifyWhenSignatureIsOverItsOwnSecret() {
        // Arrange
        RawProof proof = readRawProof(VALID_SIG_INPUTS_PROOF_BLOCK);

        // Act
        boolean valid = verifySignature(proof.getSecret(), proof.lockingKey(), proof.onlySignature());

        // Assert
        assertThat(valid).isTrue();
    }

    /**
     * Ensures the signature published as being over a different secret fails verification.
     */
    @Test
    void shouldRejectWhenSignatureIsOverADifferentSecret() {
        // Arrange
        RawProof proof = readRawProof(INVALID_SIG_INPUTS_PROOF_BLOCK);

        // Act
        boolean valid = verifySignature(proof.getSecret(), proof.lockingKey(), proof.onlySignature());

        // Assert
        assertThat(valid).isFalse();
    }

    /**
     * Ensures hashing the published {@code SIG_ALL} messages reproduces the published digests.
     *
     * <p>The digests are quoted in prose rather than fenced, so they are read from the document
     * text alongside the messages.
     */
    @Test
    void shouldReproducePublishedDigestsWhenHashingAggregatedMessages() {
        // Arrange
        List<String> messages = aggregatedMessageVectors().toList();
        List<String> expectedDigests = publishedAggregatedDigests();

        // Act
        List<String> digests = messages.stream().map(Nut11VectorTest::sha256Hex).toList();

        // Assert
        assertThat(digests).isEqualTo(expectedDigests);
    }

    private static List<String> publishedAggregatedDigests() {
        return VECTORS.getMarkdown().lines()
                .filter(line -> line.contains("sha256sum(msg_to_sign) should look like this"))
                .map(line -> line.substring(line.lastIndexOf('`', line.length() - 2)).replace("`", "").trim())
                .toList();
    }

    private static boolean verifySignature(String message, PublicKey publicKey, byte[] signature) {
        return Schnorr.verify(sha256(message), publicKey.getSchnorr(), signature);
    }

    @SneakyThrows
    private static byte[] sha256(String message) {
        return MessageDigest.getInstance("SHA-256").digest(message.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String message) {
        return Utils.bytesToHexString(sha256(message));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private static Proof<Secret> readProof(String proofJson) {
        return JsonUtils.JSON_MAPPER.readValue(proofJson, Proof.class);
    }

    /**
     * Reads a published proof as raw JSON.
     *
     * <p>The signature tests read the wire fields directly so that they exercise the signature
     * scheme itself rather than the library's proof model, which the parsing test above covers
     * separately.
     */
    @SneakyThrows
    private static RawProof readRawProof(int jsonBlockIndex) {
        JsonNode node = JsonUtils.JSON_MAPPER.readTree(VECTORS.block("json", jsonBlockIndex).text());
        return new RawProof(node.get("secret").asText(), node.get("witness").asText());
    }

    /**
     * A published proof's wire fields, before the library's proof model is applied.
     */
    @Value
    static class RawProof {

        String secret;
        String witness;

        /**
         * The key the secret is locked to, that is the NUT-10 {@code data} field.
         */
        @SneakyThrows
        PublicKey lockingKey() {
            JsonNode wellKnownSecret = JsonUtils.JSON_MAPPER.readTree(secret);
            return PublicKey.fromString(wellKnownSecret.get(1).get("data").asText());
        }

        /**
         * The proof's single witness signature.
         */
        @SneakyThrows
        byte[] onlySignature() {
            JsonNode signatures = JsonUtils.JSON_MAPPER.readTree(witness).get("signatures");
            if (signatures.size() != 1) {
                throw new IllegalStateException("Expected exactly one signature, got " + signatures.size());
            }
            return Utils.hexStringToBytes(signatures.get(0).asText());
        }
    }

    /**
     * One published proof, identified by its position in the vector document.
     */
    @Value
    static class ProofVector {

        int blockIndex;
        String proofJson;

        @Override
        public String toString() {
            return "block " + blockIndex;
        }
    }
}
