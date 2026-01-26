package xyz.tcheeric.cashu.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.RSSProof;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Signature;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ProofFingerprint} utility class.
 */
@DisplayName("ProofFingerprint")
class ProofFingerprintTest {

    private static final String MINT_URL = "https://testmint.example.com";
    private static final String SECRET_1 = "407915bc212be61a77e3e6d2aeb4c727980bda51cd06a6afc29e2861768a7837";
    private static final String SECRET_2 = "fe15109314e61d7756b0f8ee0f23a624acaa3f4e042f61433c728c7057b931be";
    private static final String SECRET_3 = "9a6dbb847bd232ba76db0df197216b29d3b8cc14553cd27827fc1cc942fedb4e";

    @Nested
    @DisplayName("compute(Collection<Proof>, String)")
    class ComputeFromProofs {

        /**
         * Ensures the fingerprint is deterministic: same proofs always produce same fingerprint.
         */
        @Test
        void shouldProduceDeterministicFingerprintForSameProofs() {
            // Arrange
            List<Proof<RandomStringSecret>> proofs = List.of(
                    createProof(SECRET_1, 2),
                    createProof(SECRET_2, 8)
            );

            // Act
            String fingerprint1 = ProofFingerprint.compute(proofs, MINT_URL);
            String fingerprint2 = ProofFingerprint.compute(proofs, MINT_URL);

            // Assert
            assertThat(fingerprint1).isEqualTo(fingerprint2);
            assertThat(fingerprint1).hasSize(64); // SHA-256 produces 64 hex chars
        }

        /**
         * Ensures proof ordering does not affect the fingerprint (secrets are sorted).
         */
        @Test
        void shouldProduceSameFingerprintRegardlessOfProofOrder() {
            // Arrange
            List<Proof<RandomStringSecret>> proofsOrderA = List.of(
                    createProof(SECRET_1, 2),
                    createProof(SECRET_2, 8)
            );
            List<Proof<RandomStringSecret>> proofsOrderB = List.of(
                    createProof(SECRET_2, 8),
                    createProof(SECRET_1, 2)
            );

            // Act
            String fingerprintA = ProofFingerprint.compute(proofsOrderA, MINT_URL);
            String fingerprintB = ProofFingerprint.compute(proofsOrderB, MINT_URL);

            // Assert
            assertThat(fingerprintA).isEqualTo(fingerprintB);
        }

        /**
         * Ensures different proofs produce different fingerprints (collision resistance).
         */
        @Test
        void shouldProduceDifferentFingerprintsForDifferentProofs() {
            // Arrange
            List<Proof<RandomStringSecret>> proofsA = List.of(createProof(SECRET_1, 2));
            List<Proof<RandomStringSecret>> proofsB = List.of(createProof(SECRET_2, 2));

            // Act
            String fingerprintA = ProofFingerprint.compute(proofsA, MINT_URL);
            String fingerprintB = ProofFingerprint.compute(proofsB, MINT_URL);

            // Assert
            assertThat(fingerprintA).isNotEqualTo(fingerprintB);
        }

        /**
         * Ensures same proofs at different mints produce different fingerprints.
         */
        @Test
        void shouldProduceDifferentFingerprintsForDifferentMints() {
            // Arrange
            List<Proof<RandomStringSecret>> proofs = List.of(createProof(SECRET_1, 2));

            // Act
            String fingerprintMintA = ProofFingerprint.compute(proofs, "https://mint-a.example.com");
            String fingerprintMintB = ProofFingerprint.compute(proofs, "https://mint-b.example.com");

            // Assert
            assertThat(fingerprintMintA).isNotEqualTo(fingerprintMintB);
        }

        /**
         * Ensures empty proof collection throws exception.
         */
        @Test
        void shouldThrowExceptionWhenProofsEmpty() {
            // Arrange
            List<Proof<RandomStringSecret>> emptyProofs = List.of();

            // Act & Assert
            assertThatThrownBy(() -> ProofFingerprint.compute(emptyProofs, MINT_URL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }

        /**
         * Ensures fingerprint is valid hex string.
         */
        @Test
        void shouldProduceValidHexFingerprint() {
            // Arrange
            List<Proof<RandomStringSecret>> proofs = List.of(
                    createProof(SECRET_1, 2),
                    createProof(SECRET_2, 8),
                    createProof(SECRET_3, 16)
            );

            // Act
            String fingerprint = ProofFingerprint.compute(proofs, MINT_URL);

            // Assert
            assertThat(fingerprint)
                    .hasSize(64)
                    .matches("[0-9a-f]+");
        }
    }

    @Nested
    @DisplayName("computeFromSecrets(Collection<String>, String)")
    class ComputeFromSecrets {

        /**
         * Ensures fingerprint from secrets matches fingerprint from proofs with same secrets.
         */
        @Test
        void shouldMatchFingerprintFromProofs() {
            // Arrange
            List<Proof<RandomStringSecret>> proofs = List.of(
                    createProof(SECRET_1, 2),
                    createProof(SECRET_2, 8)
            );
            List<String> secrets = List.of(SECRET_1, SECRET_2);

            // Act
            String fromProofs = ProofFingerprint.compute(proofs, MINT_URL);
            String fromSecrets = ProofFingerprint.computeFromSecrets(secrets, MINT_URL);

            // Assert
            assertThat(fromSecrets).isEqualTo(fromProofs);
        }

        /**
         * Ensures empty secrets collection throws exception.
         */
        @Test
        void shouldThrowExceptionWhenSecretsEmpty() {
            // Arrange
            List<String> emptySecrets = List.of();

            // Act & Assert
            assertThatThrownBy(() -> ProofFingerprint.computeFromSecrets(emptySecrets, MINT_URL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }

        /**
         * Ensures blank secrets are filtered out.
         */
        @Test
        void shouldFilterBlankSecrets() {
            // Arrange
            List<String> secretsWithBlanks = List.of(SECRET_1, "", "  ", SECRET_2);
            List<String> validSecrets = List.of(SECRET_1, SECRET_2);

            // Act
            String withBlanks = ProofFingerprint.computeFromSecrets(secretsWithBlanks, MINT_URL);
            String withoutBlanks = ProofFingerprint.computeFromSecrets(validSecrets, MINT_URL);

            // Assert
            assertThat(withBlanks).isEqualTo(withoutBlanks);
        }
    }

    @Nested
    @DisplayName("computeFromString(String)")
    class ComputeFromString {

        /**
         * Ensures fallback fingerprint is deterministic.
         */
        @Test
        void shouldProduceDeterministicFallbackFingerprint() {
            // Arrange
            String input = "cashuAeyJ0b2tlbiI6W3sibWludCI6Imh0dHBzOi8vODMzMy5zcGFjZTozMzM4In1dfQ";

            // Act
            String fingerprint1 = ProofFingerprint.computeFromString(input);
            String fingerprint2 = ProofFingerprint.computeFromString(input);

            // Assert
            assertThat(fingerprint1).isEqualTo(fingerprint2);
        }

        /**
         * Ensures whitespace is trimmed before hashing.
         */
        @Test
        void shouldTrimWhitespaceBeforeHashing() {
            // Arrange
            String withWhitespace = "  test-token-string  ";
            String withoutWhitespace = "test-token-string";

            // Act
            String fingerprintWith = ProofFingerprint.computeFromString(withWhitespace);
            String fingerprintWithout = ProofFingerprint.computeFromString(withoutWhitespace);

            // Assert
            assertThat(fingerprintWith).isEqualTo(fingerprintWithout);
        }

        /**
         * Ensures empty string throws exception.
         */
        @Test
        void shouldThrowExceptionWhenInputEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> ProofFingerprint.computeFromString(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ProofFingerprint.computeFromString("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Creates a test proof with the given secret and amount.
     */
    private static RSSProof createProof(String secretHex, int amount) {
        RSSProof proof = new RSSProof();
        proof.setSecret(RandomStringSecret.fromString(secretHex));
        proof.setAmount(amount);
        proof.setKeySetId("009a1f293253e41e");
        proof.setUnblindedSignature(Signature.fromString(
                "02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea"));
        return proof;
    }
}
