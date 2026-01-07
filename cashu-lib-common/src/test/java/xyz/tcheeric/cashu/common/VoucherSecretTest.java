package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
        System.out.println("Serialized JSON: " + json);

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
        System.out.println("Serialized with tags: " + json);
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
}
