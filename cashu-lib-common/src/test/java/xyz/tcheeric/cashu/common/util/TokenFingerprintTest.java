package xyz.tcheeric.cashu.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.RSSProof;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Signature;
import xyz.tcheeric.cashu.common.TokenV3;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link TokenFingerprint} utility class.
 */
@DisplayName("TokenFingerprint")
class TokenFingerprintTest {

    private static final String SECRET_1 = "407915bc212be61a77e3e6d2aeb4c727980bda51cd06a6afc29e2861768a7837";
    private static final String SECRET_2 = "fe15109314e61d7756b0f8ee0f23a624acaa3f4e042f61433c728c7057b931be";
    private static final String MINT_URL = "https://8333.space:3338";

    @Nested
    @DisplayName("compute(String) - V3 tokens")
    class ComputeV3 {

        /**
         * Ensures V3 token fingerprint is deterministic.
         */
        @Test
        void shouldProduceDeterministicFingerprintForV3Token() {
            // Arrange
            String serializedToken = createV3Token().serialize(false);

            // Act
            String fingerprint1 = TokenFingerprint.compute(serializedToken);
            String fingerprint2 = TokenFingerprint.compute(serializedToken);

            // Assert
            assertThat(fingerprint1).isEqualTo(fingerprint2);
            assertThat(fingerprint1).hasSize(64);
        }

        /**
         * Ensures clickable URI format produces same fingerprint as bare token.
         */
        @Test
        void shouldProduceSameFingerprintForClickableAndBareV3Token() {
            // Arrange
            TokenV3<RandomStringSecret> token = createV3Token();
            String bareToken = token.serialize(false);
            String clickableToken = token.serialize(true);

            // Act
            String fingerprintBare = TokenFingerprint.compute(bareToken);
            String fingerprintClickable = TokenFingerprint.compute(clickableToken);

            // Assert
            assertThat(clickableToken).startsWith("cashu:cashuA");
            assertThat(bareToken).startsWith("cashuA");
            assertThat(fingerprintClickable).isEqualTo(fingerprintBare);
        }

        /**
         * Ensures different V3 tokens produce different fingerprints.
         */
        @Test
        void shouldProduceDifferentFingerprintsForDifferentV3Tokens() {
            // Arrange
            TokenV3<RandomStringSecret> tokenA = createV3Token();
            TokenV3<RandomStringSecret> tokenB = createV3TokenWithDifferentSecrets();

            String serializedA = tokenA.serialize(false);
            String serializedB = tokenB.serialize(false);

            // Act
            String fingerprintA = TokenFingerprint.compute(serializedA);
            String fingerprintB = TokenFingerprint.compute(serializedB);

            // Assert
            assertThat(fingerprintA).isNotEqualTo(fingerprintB);
        }

        /**
         * Ensures V3 fingerprint matches ProofFingerprint computation.
         */
        @Test
        void shouldMatchProofFingerprintForV3Token() {
            // Arrange
            TokenV3<RandomStringSecret> token = createV3Token();
            String serializedToken = token.serialize(false);

            // Get proofs directly
            Set<Proof<RandomStringSecret>> proofs = token.getMintProofs().iterator().next().getProofs();
            String mintUrl = token.getMintProofs().iterator().next().getMint();

            // Act
            String tokenFingerprint = TokenFingerprint.compute(serializedToken);
            String proofFingerprint = ProofFingerprint.compute(proofs, mintUrl);

            // Assert
            assertThat(tokenFingerprint).isEqualTo(proofFingerprint);
        }
    }

    @Nested
    @DisplayName("compute(String) - V4 tokens")
    class ComputeV4 {

        /**
         * Ensures V4 token fingerprint is deterministic.
         */
        @Test
        void shouldProduceDeterministicFingerprintForV4Token() {
            // Arrange
            String serializedToken = createV4Token().serialize(false);

            // Act
            String fingerprint1 = TokenFingerprint.compute(serializedToken);
            String fingerprint2 = TokenFingerprint.compute(serializedToken);

            // Assert
            assertThat(fingerprint1).isEqualTo(fingerprint2);
            assertThat(fingerprint1).hasSize(64);
        }

        /**
         * Ensures V4 token is recognized by its prefix.
         */
        @Test
        void shouldRecognizeV4TokenPrefix() {
            // Arrange
            String serializedToken = createV4Token().serialize(false);

            // Assert
            assertThat(serializedToken).startsWith("cashuB");

            // Act - should not throw
            String fingerprint = TokenFingerprint.compute(serializedToken);
            assertThat(fingerprint).hasSize(64);
        }

        /**
         * Ensures different V4 tokens produce different fingerprints.
         */
        @Test
        void shouldProduceDifferentFingerprintsForDifferentV4Tokens() {
            // Arrange
            TokenV4 tokenA = createV4Token();
            TokenV4 tokenB = createV4TokenWithDifferentSecrets();

            String serializedA = tokenA.serialize(false);
            String serializedB = tokenB.serialize(false);

            // Act
            String fingerprintA = TokenFingerprint.compute(serializedA);
            String fingerprintB = TokenFingerprint.compute(serializedB);

            // Assert
            assertThat(fingerprintA).isNotEqualTo(fingerprintB);
        }
    }

    @Nested
    @DisplayName("compute(String) - fallback behavior")
    class ComputeFallback {

        /**
         * Ensures invalid token format falls back to string hashing.
         */
        @Test
        void shouldFallbackToStringHashForInvalidFormat() {
            // Arrange
            String invalidToken = "not-a-valid-token-format";

            // Act - should not throw, uses fallback
            String fingerprint = TokenFingerprint.compute(invalidToken);

            // Assert
            assertThat(fingerprint).hasSize(64);
        }

        /**
         * Ensures malformed base64 falls back gracefully.
         */
        @Test
        void shouldFallbackWhenBase64Malformed() {
            // Arrange - valid prefix but invalid base64 payload
            String malformedToken = "cashuA!!!invalid-base64!!!";

            // Act - should not throw, uses fallback
            String fingerprint = TokenFingerprint.compute(malformedToken);

            // Assert
            assertThat(fingerprint).hasSize(64);
        }

        /**
         * Ensures empty token throws exception.
         */
        @Test
        void shouldThrowExceptionForEmptyToken() {
            // Act & Assert
            assertThatThrownBy(() -> TokenFingerprint.compute(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        /**
         * Ensures whitespace-only token throws exception.
         */
        @Test
        void shouldThrowExceptionForWhitespaceOnlyToken() {
            // Act & Assert
            assertThatThrownBy(() -> TokenFingerprint.compute("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("computeFromProofs(Collection<Proof>, String)")
    class ComputeFromProofs {

        /**
         * Ensures direct proof fingerprinting works.
         */
        @Test
        void shouldComputeFingerprintFromProofsDirectly() {
            // Arrange
            Set<Proof<RandomStringSecret>> proofs = new LinkedHashSet<>();
            proofs.add(createProof(SECRET_1, 2));
            proofs.add(createProof(SECRET_2, 8));

            // Act
            String fingerprint = TokenFingerprint.computeFromProofs(proofs, MINT_URL);

            // Assert
            assertThat(fingerprint).hasSize(64);
        }
    }

    /**
     * Creates a V3 token with known secrets for testing.
     */
    private static TokenV3<RandomStringSecret> createV3Token() {
        TokenV3<RandomStringSecret> token = new TokenV3<>();
        token.setMemo("Test token");
        token.setUnit("sat");

        Set<TokenV3.MintProof<RandomStringSecret>> mintProofs = new LinkedHashSet<>();
        TokenV3.MintProof<RandomStringSecret> mintProof = new TokenV3.MintProof<>();
        mintProof.setMint(MINT_URL);

        Set<Proof<RandomStringSecret>> proofs = new LinkedHashSet<>();
        proofs.add(createProof(SECRET_1, 2));
        proofs.add(createProof(SECRET_2, 8));

        mintProof.setProofs(proofs);
        mintProofs.add(mintProof);
        token.setMintProofs(mintProofs);

        return token;
    }

    /**
     * Creates a V3 token with different secrets for collision testing.
     */
    private static TokenV3<RandomStringSecret> createV3TokenWithDifferentSecrets() {
        TokenV3<RandomStringSecret> token = new TokenV3<>();
        token.setMemo("Different token");
        token.setUnit("sat");

        Set<TokenV3.MintProof<RandomStringSecret>> mintProofs = new LinkedHashSet<>();
        TokenV3.MintProof<RandomStringSecret> mintProof = new TokenV3.MintProof<>();
        mintProof.setMint(MINT_URL);

        Set<Proof<RandomStringSecret>> proofs = new LinkedHashSet<>();
        proofs.add(createProof("9a6dbb847bd232ba76db0df197216b29d3b8cc14553cd27827fc1cc942fedb4e", 4));

        mintProof.setProofs(proofs);
        mintProofs.add(mintProof);
        token.setMintProofs(mintProofs);

        return token;
    }

    /**
     * Creates a V4 token with known secrets for testing.
     */
    private static TokenV4 createV4Token() {
        TokenV4 token = new TokenV4();
        token.setMemo("Test V4 token");
        token.setUnit("sat");
        token.setMintUrl("http://localhost:3338");

        TokenV4.TokenData.TokenProof proof1 = new TokenV4.TokenData.TokenProof();
        proof1.setAmount(2);
        proof1.setSecret(SECRET_1);
        proof1.setSignature(Utils.hexStringToBytes(
                "038618543ffb6b8695df4ad4babcde92a34a96bdcd97dcee0d7ccf98d472126792"));

        TokenV4.TokenData.TokenProof proof2 = new TokenV4.TokenData.TokenProof();
        proof2.setAmount(8);
        proof2.setSecret(SECRET_2);
        proof2.setSignature(Utils.hexStringToBytes(
                "038618543ffb6b8695df4ad4babcde92a34a96bdcd97dcee0d7ccf98d472126792"));

        token.setTokenDataList(List.of(
                new TokenV4.TokenData(
                        Utils.hexStringToBytes("00ad268c4d1f5826"),
                        List.of(proof1, proof2)
                )
        ));

        return token;
    }

    /**
     * Creates a V4 token with different secrets for collision testing.
     */
    private static TokenV4 createV4TokenWithDifferentSecrets() {
        TokenV4 token = new TokenV4();
        token.setMemo("Different V4 token");
        token.setUnit("sat");
        token.setMintUrl("http://localhost:3338");

        TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
        proof.setAmount(4);
        proof.setSecret("9a6dbb847bd232ba76db0df197216b29d3b8cc14553cd27827fc1cc942fedb4e");
        proof.setSignature(Utils.hexStringToBytes(
                "038618543ffb6b8695df4ad4babcde92a34a96bdcd97dcee0d7ccf98d472126792"));

        token.setTokenDataList(List.of(
                new TokenV4.TokenData(
                        Utils.hexStringToBytes("00ad268c4d1f5826"),
                        List.of(proof)
                )
        ));

        return token;
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
