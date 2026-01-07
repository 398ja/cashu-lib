package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests VoucherSecret serialization and deserialization with raw byte data.
 * This tests backward compatibility with legacy data formats.
 */
class VoucherWellKnownSecretTest {

    /**
     * Tests that VoucherSecret can be serialized to NUT-10 JSON and parsed back.
     */
    @Test
    void shouldSerializeAndParseVoucherSecret() {
        // Arrange: Create voucher secret with test data
        byte[] voucherData = "test-voucher-data-with-signature-and-metadata-longer-than-32-bytes"
                .getBytes(StandardCharsets.UTF_8);
        VoucherSecret original = new VoucherSecret();
        original.setData(voucherData);

        // Act: Serialize to JSON and parse back
        String json = original.toString();
        System.out.println("Serialized JSON: " + json);

        Secret parsed = SecretUtil.toSecret(json);

        // Assert: Parsed secret should be VoucherSecret with same data
        assertThat(parsed).isInstanceOf(VoucherSecret.class);
        VoucherSecret parsedVoucher = (VoucherSecret) parsed;
        assertThat(parsedVoucher.getData()).isEqualTo(voucherData);
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
        VoucherSecret secret1 = new VoucherSecret();
        secret1.setData(voucherData);
        secret1.setNonce(nonce);

        VoucherSecret secret2 = new VoucherSecret();
        secret2.setData(voucherData);
        secret2.setNonce(nonce);

        // Assert
        assertThat(secret1.toString()).isEqualTo(secret2.toString());
    }
}
