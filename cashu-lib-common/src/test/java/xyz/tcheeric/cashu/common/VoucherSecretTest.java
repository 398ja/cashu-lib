package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut18.VoucherSecret;
import xyz.tcheeric.cashu.common.nut18.VoucherTags;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests VoucherSecret serialization, deserialization, and tag-based storage.
 */
class VoucherSecretTest {

    /**
     * Tests that VoucherSecret can be serialized to NUT-10 JSON and parsed back.
     */
    @Test
    void shouldSerializeAndParseVoucherSecret() {
        // Arrange: Create voucher secret with voucherId
        UUID voucherId = UUID.randomUUID();
        VoucherSecret original = new VoucherSecret(voucherId);

        // Act: Serialize to JSON and parse back
        String json = original.toString();
        Secret parsed = SecretUtil.toSecret(json);

        // Assert: Parsed secret should be VoucherSecret with same voucherId
        assertThat(parsed).isInstanceOf(VoucherSecret.class);
        VoucherSecret parsedVoucher = (VoucherSecret) parsed;
        assertThat(parsedVoucher.getVoucherId()).isEqualTo(voucherId);
    }

    /**
     * Tests that voucher secrets with explicit nonces are deterministic.
     */
    @Test
    void shouldCreateDeterministicSecretWithExplicitNonce() {
        // Arrange
        UUID voucherId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String nonce = "fixed-nonce-for-testing";

        // Act
        VoucherSecret secret1 = new VoucherSecret(voucherId, nonce);
        VoucherSecret secret2 = new VoucherSecret(voucherId, nonce);

        // Assert
        assertThat(secret1.toString()).isEqualTo(secret2.toString());
    }

    /**
     * Tests tag-based getters and setters.
     */
    @Test
    void shouldStoreAndRetrieveTags() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        VoucherSecret secret = new VoucherSecret(voucherId);

        // Act: Set tags
        secret.setIssuerId("merchant123");
        secret.setUnit("sat");
        secret.setFaceValue(5000L);
        secret.setExpiresAt(1736380800L);
        secret.setMemo("Gift card");
        secret.setFaceDecimals(2);
        secret.setIssuerSignature("5f3a8b2c");
        secret.setIssuerPublicKey("02abc123");

        // Assert: Verify tags are stored correctly
        assertThat(secret.getIssuerId()).isEqualTo("merchant123");
        assertThat(secret.getUnit()).isEqualTo("sat");
        assertThat(secret.getFaceValue()).isEqualTo(5000L);
        assertThat(secret.getExpiresAt()).isEqualTo(1736380800L);
        assertThat(secret.getMemo()).isEqualTo("Gift card");
        assertThat(secret.getFaceDecimals()).isEqualTo(2);
        assertThat(secret.getIssuerSignature()).isEqualTo("5f3a8b2c");
        assertThat(secret.getIssuerPublicKey()).isEqualTo("02abc123");
    }

    /**
     * Tests that tags survive serialization/deserialization round-trip.
     */
    @Test
    void shouldPreserveTagsAfterSerialization() {
        // Arrange
        UUID voucherId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        VoucherSecret original = VoucherSecret.builder()
                .voucherId(voucherId)
                .nonce("test-nonce")
                .issuerId("coffee_shop")
                .unit("usd")
                .faceValue(1000L)
                .expiresAt(1800000000L)
                .memo("Birthday gift")
                .faceDecimals(2)
                .issuerSignature("abcd1234")
                .issuerPublicKey("02def567")
                .build();

        // Act: Serialize and parse
        String json = original.toString();
        Secret parsed = SecretUtil.toSecret(json);

        // Assert
        assertThat(parsed).isInstanceOf(VoucherSecret.class);
        VoucherSecret parsedVoucher = (VoucherSecret) parsed;
        assertThat(parsedVoucher.getVoucherId()).isEqualTo(voucherId);
        assertThat(parsedVoucher.getIssuerId()).isEqualTo("coffee_shop");
        assertThat(parsedVoucher.getUnit()).isEqualTo("usd");
        assertThat(parsedVoucher.getFaceValue()).isEqualTo(1000L);
        assertThat(parsedVoucher.getExpiresAt()).isEqualTo(1800000000L);
        assertThat(parsedVoucher.getMemo()).isEqualTo("Birthday gift");
        assertThat(parsedVoucher.getFaceDecimals()).isEqualTo(2);
        assertThat(parsedVoucher.getIssuerSignature()).isEqualTo("abcd1234");
        assertThat(parsedVoucher.getIssuerPublicKey()).isEqualTo("02def567");
    }

    /**
     * Tests the builder pattern.
     */
    @Test
    void shouldBuildVoucherSecretWithBuilder() {
        // Act
        VoucherSecret secret = VoucherSecret.builder()
                .issuerId("store123")
                .unit("sat")
                .faceValue(10000L)
                .build();

        // Assert
        assertThat(secret.getVoucherId()).isNotNull();
        assertThat(secret.getIssuerId()).isEqualTo("store123");
        assertThat(secret.getUnit()).isEqualTo("sat");
        assertThat(secret.getFaceValue()).isEqualTo(10000L);
    }

    /**
     * Tests isExpired() helper method.
     */
    @Test
    void shouldDetectExpiredVoucher() {
        // Arrange: Create voucher with past expiry
        VoucherSecret expiredVoucher = VoucherSecret.builder()
                .expiresAt(1000000L) // Far in the past
                .build();

        // Arrange: Create voucher with future expiry
        VoucherSecret validVoucher = VoucherSecret.builder()
                .expiresAt(System.currentTimeMillis() / 1000 + 3600) // 1 hour from now
                .build();

        // Arrange: Create voucher with no expiry
        VoucherSecret noExpiryVoucher = VoucherSecret.builder()
                .build();

        // Assert
        assertThat(expiredVoucher.isExpired()).isTrue();
        assertThat(validVoucher.isExpired()).isFalse();
        assertThat(noExpiryVoucher.isExpired()).isFalse();
    }

    /**
     * Tests isSigned() helper method.
     */
    @Test
    void shouldDetectSignedVoucher() {
        // Arrange: Create unsigned voucher
        VoucherSecret unsigned = VoucherSecret.builder()
                .issuerId("merchant")
                .build();

        // Arrange: Create partially signed voucher
        VoucherSecret partialSig = VoucherSecret.builder()
                .issuerSignature("abc123")
                .build();

        // Arrange: Create fully signed voucher
        VoucherSecret signed = VoucherSecret.builder()
                .issuerSignature("abc123")
                .issuerPublicKey("02xyz789")
                .build();

        // Assert
        assertThat(unsigned.isSigned()).isFalse();
        assertThat(partialSig.isSigned()).isFalse();
        assertThat(signed.isSigned()).isTrue();
    }

    /**
     * Tests that optional tags can be cleared.
     */
    @Test
    void shouldClearOptionalTags() {
        // Arrange
        VoucherSecret secret = VoucherSecret.builder()
                .memo("Original memo")
                .expiresAt(1700000000L)
                .faceDecimals(2)
                .build();

        // Act: Clear optional tags
        secret.setMemo(null);
        secret.setExpiresAt(null);
        secret.setFaceDecimals(0);

        // Assert: Tags should be cleared
        assertThat(secret.getMemo()).isNull();
        assertThat(secret.getExpiresAt()).isNull();
        assertThat(secret.getFaceDecimals()).isEqualTo(0);
    }

    /**
     * Tests default value for face decimals.
     */
    @Test
    void shouldDefaultFaceDecimalsToZero() {
        // Arrange
        VoucherSecret secret = new VoucherSecret(UUID.randomUUID());

        // Assert: Default should be 0
        assertThat(secret.getFaceDecimals()).isEqualTo(0);
    }

    /**
     * Tests that fractional issuance_ratio values are preserved through serialization.
     */
    @Test
    void shouldPreserveFractionalIssuanceRatio() {
        // Arrange: Create voucher with fractional issuance ratio
        UUID voucherId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        VoucherSecret original = VoucherSecret.builder()
                .voucherId(voucherId)
                .nonce("ratio-test-nonce")
                .issuerId("merchant")
                .unit("sat")
                .faceValue(1000L)
                .issuanceRatio(0.5)  // Fractional ratio
                .build();

        // Act: Serialize and parse
        String json = original.toString();
        Secret parsed = SecretUtil.toSecret(json);

        // Assert: Fractional ratio should be preserved
        assertThat(parsed).isInstanceOf(VoucherSecret.class);
        VoucherSecret parsedVoucher = (VoucherSecret) parsed;
        assertThat(parsedVoucher.getIssuanceRatio()).isEqualTo(0.5);
    }

    /**
     * Tests that various fractional issuance ratios are preserved.
     */
    @Test
    void shouldPreserveVariousFractionalRatios() {
        double[] testRatios = {0.1, 0.25, 0.333, 0.5, 0.75, 1.5, 2.5, 0.001};

        for (double ratio : testRatios) {
            // Arrange
            VoucherSecret original = VoucherSecret.builder()
                    .issuerId("test")
                    .issuanceRatio(ratio)
                    .build();

            // Act
            String json = original.toString();
            Secret parsed = SecretUtil.toSecret(json);

            // Assert
            assertThat(parsed).isInstanceOf(VoucherSecret.class);
            VoucherSecret parsedVoucher = (VoucherSecret) parsed;
            assertThat(parsedVoucher.getIssuanceRatio())
                    .as("Ratio %s should be preserved", ratio)
                    .isEqualTo(ratio);
        }
    }

    /**
     * Tests that integer issuance ratios are also preserved correctly.
     */
    @Test
    void shouldPreserveIntegerIssuanceRatio() {
        // Arrange: Create voucher with integer ratio (as double)
        VoucherSecret original = VoucherSecret.builder()
                .issuerId("merchant")
                .issuanceRatio(2.0)  // Integer value as double
                .build();

        // Act
        String json = original.toString();
        Secret parsed = SecretUtil.toSecret(json);

        // Assert: Should still be 2.0
        assertThat(parsed).isInstanceOf(VoucherSecret.class);
        VoucherSecret parsedVoucher = (VoucherSecret) parsed;
        assertThat(parsedVoucher.getIssuanceRatio()).isEqualTo(2.0);
    }

    /**
     * A whole-number ratio must be stored as "2", not "2.0".
     *
     * <p>cashu-voucher 0.15.0 (#48) refuses a numeric tag whose wire spelling is not already
     * canonical, and its canonical spelling of 2.0 is "2". Storing String.valueOf(2.0) meant every
     * voucher built through this setter with a whole-number ratio other than 1 failed
     * verification, even though the issuer's signature was good. Fractions keep
     * Double.toString's spelling, which is what the canonicaliser writes for them.
     */
    @Test
    void shouldStoreIssuanceRatioInCanonicalSpelling() {
        double[] ratios = {2.0, 3.0, 100.0, 1.0E7, 0.5, 1.5, 0.07516442217350454, 1.0E-4};
        String[] expected = {"2", "3", "100", "10000000", "0.5", "1.5", "0.07516442217350454", "1.0E-4"};

        for (int i = 0; i < ratios.length; i++) {
            // Arrange + Act
            VoucherSecret secret = VoucherSecret.builder().issuerId("merchant").issuanceRatio(ratios[i]).build();

            // Assert
            assertThat(secret.getTag(VoucherTags.ISSUANCE_RATIO).getValues())
                    .as("ratio %s", ratios[i])
                    .containsExactly(expected[i]);
            assertThat(secret.getIssuanceRatio()).isEqualTo(ratios[i]);
        }
    }

    /**
     * A ratio with no number to spell is refused instead of being stored as a different number.
     *
     * <p>The canonical rule writes a non-fractional value through {@code (long) value}, so NaN
     * would become "0" and +Infinity "9223372036854775807": a ratio nobody chose, silently. The
     * old setter wrote "NaN" / "Infinity", which cashu-voucher refuses outright. Refusing at the
     * setter keeps that outcome and puts it at the call site.
     */
    @Test
    void shouldRefuseNonFiniteIssuanceRatio() {
        for (double ratio : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            // Arrange
            VoucherSecret secret = new VoucherSecret();

            // Act + Assert
            assertThatThrownBy(() -> secret.setIssuanceRatio(ratio))
                    .as("ratio %s", ratio)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("issuance_ratio");
        }
    }

    /**
     * Only the issuance_ratio tag changes spelling; the integral tags and the rendered secret
     * are otherwise byte-identical to what this library always produced.
     *
     * <p>Every live voucher's proof secret and every issuer signature depends on these bytes.
     * face_value, expires_at and face_decimals are Long/Integer and String.valueOf renders them
     * with no decimal point, so they were already canonical and must stay exactly so.
     */
    @Test
    void shouldLeaveEveryOtherTagSpellingUnchanged() {
        // Arrange
        VoucherSecret secret = VoucherSecret.builder()
                .voucherId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .nonce("aa")
                .issuerId("merchant")
                .unit("EUR")
                .faceValue(500L)
                .expiresAt(1893456000L)
                .faceDecimals(2)
                .backingStrategy("MINIMAL")
                .issuanceRatio(50.0)
                .build();

        // Act
        String wire = secret.toString();

        // Assert
        assertThat(wire).contains("[\"face_value\",\"500\"]", "[\"expires_at\",\"1893456000\"]",
                "[\"face_decimals\",\"2\"]", "[\"issuance_ratio\",\"50\"]");
        assertThat(wire).doesNotContain("50.0");
    }

    // ===== Negative test cases for error handling =====

    /**
     * Tests that getFaceValue returns null for invalid numeric data.
     */
    @Test
    void shouldReturnNullForInvalidFaceValue() {
        // Arrange: Create voucher and set invalid face_value tag
        VoucherSecret secret = new VoucherSecret(UUID.randomUUID());
        secret.setTag(VoucherTags.FACE_VALUE, java.util.List.of("not-a-number"));

        // Act & Assert: Should return null instead of throwing exception
        assertThat(secret.getFaceValue()).isNull();
    }

    /**
     * Tests that getExpiresAt returns null for invalid numeric data.
     */
    @Test
    void shouldReturnNullForInvalidExpiresAt() {
        // Arrange: Create voucher and set invalid expires_at tag
        VoucherSecret secret = new VoucherSecret(UUID.randomUUID());
        secret.setTag(VoucherTags.EXPIRES_AT, java.util.List.of("invalid-timestamp"));

        // Act & Assert: Should return null instead of throwing exception
        assertThat(secret.getExpiresAt()).isNull();
    }

    /**
     * Tests that getFaceDecimals returns 0 for invalid numeric data.
     */
    @Test
    void shouldReturnZeroForInvalidFaceDecimals() {
        // Arrange: Create voucher and set invalid face_decimals tag
        VoucherSecret secret = new VoucherSecret(UUID.randomUUID());
        secret.setTag(VoucherTags.FACE_DECIMALS, java.util.List.of("abc"));

        // Act & Assert: Should return 0 instead of throwing exception
        assertThat(secret.getFaceDecimals()).isEqualTo(0);
    }

    /**
     * Tests that getIssuanceRatio returns 1.0 for invalid numeric data.
     */
    @Test
    void shouldReturnDefaultForInvalidIssuanceRatio() {
        // Arrange: Create voucher and set invalid issuance_ratio tag
        VoucherSecret secret = new VoucherSecret(UUID.randomUUID());
        secret.setTag(VoucherTags.ISSUANCE_RATIO, java.util.List.of("invalid"));

        // Act & Assert: Should return 1.0 instead of throwing exception
        assertThat(secret.getIssuanceRatio()).isEqualTo(1.0);
    }

    /**
     * Tests that all numeric getters handle empty string values gracefully.
     */
    @Test
    void shouldHandleEmptyStringValuesGracefully() {
        // Arrange: Create voucher and set empty string values
        VoucherSecret secret = new VoucherSecret(UUID.randomUUID());
        secret.setTag(VoucherTags.FACE_VALUE, java.util.List.of(""));
        secret.setTag(VoucherTags.EXPIRES_AT, java.util.List.of(""));
        secret.setTag(VoucherTags.FACE_DECIMALS, java.util.List.of(""));
        secret.setTag(VoucherTags.ISSUANCE_RATIO, java.util.List.of(""));

        // Act & Assert: Should return default values instead of throwing exception
        assertThat(secret.getFaceValue()).isNull();
        assertThat(secret.getExpiresAt()).isNull();
        assertThat(secret.getFaceDecimals()).isEqualTo(0);
        assertThat(secret.getIssuanceRatio()).isEqualTo(1.0);
    }

    /**
     * Tests that getVoucherId returns null for invalid UUID data.
     */
    @Test
    void shouldReturnNullForInvalidVoucherId() {
        // Arrange: Create voucher and set invalid data that's not a valid UUID
        VoucherSecret secret = new VoucherSecret();
        secret.setData("not-a-valid-uuid".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Act & Assert: Should return null instead of throwing exception
        assertThat(secret.getVoucherId()).isNull();
    }

    /**
     * Tests that getVoucherId returns null for empty data.
     */
    @Test
    void shouldReturnNullForEmptyVoucherIdData() {
        // Arrange: Create voucher with empty data
        VoucherSecret secret = new VoucherSecret();
        secret.setData("".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Act & Assert: Should return null instead of throwing exception
        assertThat(secret.getVoucherId()).isNull();
    }
}
