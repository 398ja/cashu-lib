package xyz.tcheeric.cashu.crypto;

import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the NUT-00 secret encoding migration: new proofs are issued under the spec (UTF-8)
 * encoding while proofs issued under the pre-migration hex-decode encoding keep verifying.
 */
class SecretEncodingTest {


    private static final String HEX_SECRET = "d341ee4871f1f889041e63cf0d3823c713eea6aff01e80f1719f08f9e5be98f6";

    private static final String WELL_KNOWN_SECRET =
            "[\"P2PK\",{\"nonce\":\"abc\",\"data\":\"0249098aa8b9d2fbe466870b5a"
                    + "20a9b0d7e17a0e044b2ceff58c6cbcb1ea9a2f\"}]";

    private static final BigInteger MINT_KEY = new BigInteger(
            "1af2eefbd63d7c9d5d1c1d1d0c68ecbeaa1b1a6e0e1d0c0b0a09080706050403", 16);

    /** The encoding used to issue a proof is always the NUT-00 spec encoding. */
    @Test
    void shouldUseSpecEncodingWhenIssuingAProof() {
        // Arrange & Act
        SecretEncoding issuance = SecretEncoding.forIssuance();

        // Assert
        assertThat(issuance).isEqualTo(SecretEncoding.SPEC);
    }

    /** The spec encoding hashes the raw UTF-8 bytes of the secret string, all 64 of them. */
    @Test
    void shouldEncodeSecretAsUtf8BytesWhenEncodingIsSpec() {
        // Arrange & Act
        byte[] encoded = SecretEncoding.SPEC.encode(HEX_SECRET);

        // Assert
        assertThat(encoded).isEqualTo(HEX_SECRET.getBytes(StandardCharsets.UTF_8));
        assertThat(encoded).hasSize(64);
    }

    /** The legacy encoding hex-decodes a plain secret into the 32 bytes it used to hash. */
    @Test
    void shouldHexDecodeSecretWhenEncodingIsLegacy() {
        // Arrange & Act
        byte[] encoded = SecretEncoding.LEGACY_HEX.encode(HEX_SECRET);

        // Assert
        assertThat(encoded).hasSize(32);
        assertThat(encoded).isNotEqualTo(SecretEncoding.SPEC.encode(HEX_SECRET));
    }

    /** A NUT-10 well-known secret was already UTF-8 encoded, so the migration does not move it. */
    @Test
    void shouldEncodeIdenticallyUnderBothEncodingsWhenSecretIsWellKnown() {
        // Arrange & Act
        byte[] spec = SecretEncoding.SPEC.encode(WELL_KNOWN_SECRET);
        byte[] legacy = SecretEncoding.LEGACY_HEX.encode(WELL_KNOWN_SECRET);

        // Assert
        assertThat(legacy).isEqualTo(spec);
    }

    /** A non-hex secret cannot have been issued under the legacy encoding, so it is not supported. */
    @Test
    void shouldNotSupportLegacyEncodingWhenSecretIsNotHex() {
        // Arrange & Act
        boolean supported = SecretEncoding.LEGACY_HEX.supports("not-a-hex-secret");

        // Assert
        assertThat(supported).isFalse();
    }

    /** Verification tries the spec encoding before falling back to the legacy one. */
    @Test
    void shouldTrySpecEncodingFirstWhenVerifying() {
        // Arrange & Act
        var order = SecretEncoding.verificationOrder();

        // Assert
        assertThat(order).containsExactly(SecretEncoding.SPEC, SecretEncoding.LEGACY_HEX);
    }

    /** A proof issued today, under the spec encoding, verifies. */
    @Test
    void shouldVerifyWhenProofWasIssuedUnderSpecEncoding() {
        // Arrange
        ECPoint C = commitment(SecretEncoding.SPEC);

        // Act
        boolean valid = BDHKEUtils.verify(HEX_SECRET, MINT_KEY, C);

        // Assert
        assertThat(valid).isTrue();
    }

    /** A proof issued before the migration, under hex-decoding, still verifies. */
    @Test
    void shouldVerifyWhenProofWasIssuedUnderLegacyEncoding() {
        // Arrange
        ECPoint C = commitment(SecretEncoding.LEGACY_HEX);

        // Act
        boolean valid = BDHKEUtils.verify(HEX_SECRET, MINT_KEY, C);

        // Assert
        assertThat(valid).isTrue();
    }

    /** A commitment to neither encoding is still rejected; the fallback is not a blanket accept. */
    @Test
    void shouldRejectWhenCommitmentMatchesNeitherEncoding() {
        // Arrange
        ECPoint wrongCommitment = commitment(SecretEncoding.SPEC).multiply(BigInteger.valueOf(7)).normalize();

        // Act
        boolean valid = BDHKEUtils.verify(HEX_SECRET, MINT_KEY, wrongCommitment);

        // Assert
        assertThat(valid).isFalse();
    }

    /** hash_to_curve on a secret string now commits to the UTF-8 bytes, not the decoded bytes. */
    @Test
    void shouldHashUtf8BytesWhenHashingSecretString() {
        // Arrange & Act
        byte[] fromString = BDHKEUtils.hashToCurve(HEX_SECRET);
        byte[] fromUtf8Bytes = BDHKEUtils.hashToCurve(HEX_SECRET.getBytes(StandardCharsets.UTF_8)).getEncoded(true);

        // Assert
        assertThat(fromString).isEqualTo(fromUtf8Bytes);
    }

    /** An empty secret has no encoding and is rejected before any hashing happens. */
    @Test
    void shouldRejectWhenSecretIsEmpty() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> BDHKEUtils.hashToCurve("", SecretEncoding.SPEC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ECPoint commitment(SecretEncoding encoding) {
        ECPoint Y = BDHKEUtils.hashToCurve(encoding.encode(HEX_SECRET));
        return Y.multiply(MINT_KEY).normalize();
    }
}
