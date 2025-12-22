package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests VoucherWellKnownSecret serialization and deserialization via SecretUtil.
 */
class VoucherWellKnownSecretTest {

    /**
     * Tests that VoucherWellKnownSecret can be serialized to NUT-10 JSON and parsed back.
     */
    @Test
    void shouldSerializeAndParseVoucherSecret() {
        // Arrange: Create voucher secret with test data
        byte[] voucherData = "test-voucher-data-with-signature-and-metadata-longer-than-32-bytes"
                .getBytes(StandardCharsets.UTF_8);
        VoucherWellKnownSecret original = new VoucherWellKnownSecret(voucherData);

        // Act: Serialize to JSON and parse back
        String json = original.toString();
        System.out.println("Serialized JSON: " + json);

        Secret parsed = SecretUtil.toSecret(json);

        // Assert: Parsed secret should be VoucherWellKnownSecret with same data
        assertThat(parsed).isInstanceOf(VoucherWellKnownSecret.class);
        VoucherWellKnownSecret parsedVoucher = (VoucherWellKnownSecret) parsed;
        assertThat(parsedVoucher.getVoucherData()).isEqualTo(voucherData);
    }

    /**
     * Tests that voucher secrets with explicit nonces are deterministic.
     */
    @Test
    void shouldCreateDeterministicSecretWithExplicitNonce() {
        // Arrange
        byte[] voucherData = "test-data".getBytes(StandardCharsets.UTF_8);
        String nonce = "fixed-nonce-for-testing";

        // Act
        VoucherWellKnownSecret secret1 = new VoucherWellKnownSecret(voucherData, nonce);
        VoucherWellKnownSecret secret2 = new VoucherWellKnownSecret(voucherData, nonce);

        // Assert
        assertThat(secret1.toString()).isEqualTo(secret2.toString());
    }
}
