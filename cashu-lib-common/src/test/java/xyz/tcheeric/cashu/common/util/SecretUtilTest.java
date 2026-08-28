package xyz.tcheeric.cashu.common.util;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut18.VoucherSecret;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SecretUtilTest {

    private static final String HEX_SECRET =
            "0000000000000000000000000000000000000000000000000000000000000001";

    /** Y for the 32 raw bytes the secret string hex-decodes to; the pre-migration commitment. */
    private static final String LEGACY_Y_FOR_HEX_SECRET =
            "022e7158e11c9506f1aa4248bf531298daa7febd6194f003edcd9b93ade6253acf";

    /**
     * Ensures a plain secret is UTF-8 encoded before hash_to_curve, as NUT-00 requires, and no
     * longer hex-decoded into the different point the library used to commit to.
     */
    @Test
    void shouldComputeYFromHexSecretUsingUtf8Encoding() {
        RandomStringSecret secret = RandomStringSecret.fromString(HEX_SECRET);

        String actualY = SecretUtil.toY(secret);
        String expectedY = PublicKey.fromBytes(
                BDHKEUtils.hashToCurve(HEX_SECRET.getBytes(StandardCharsets.UTF_8)).getEncoded(true)).toString();

        assertThat(actualY).isEqualTo(expectedY);
        assertThat(actualY).isNotEqualTo(LEGACY_Y_FOR_HEX_SECRET);
    }

    /**
     * Ensures NUT-10 secrets use UTF-8 string hashing rather than hex decoding.
     */
    @Test
    void shouldComputeYFromNut10SecretUsingUtf8Encoding() {
        VoucherSecret secret = new VoucherSecret();
        secret.setData(Hex.decode("deadbeef"));
        secret.setNonce("nonce-123");

        String secretString = secret.toString();
        String expectedY = PublicKey.fromBytes(BDHKEUtils.hashToCurve(secretString)).toString();

        assertThat(SecretUtil.toY(secret)).isEqualTo(expectedY);
    }

    /**
     * Ensures toYFromString and toY agree for the same secret representation.
     */
    @Test
    void shouldMatchToYFromStringWithToY() {
        RandomStringSecret secret = RandomStringSecret.fromString(HEX_SECRET);

        String fromSecret = SecretUtil.toY(secret);
        String fromString = SecretUtil.toYFromString(secret.toString());

        assertThat(fromString).isEqualTo(fromSecret);
    }

    /**
     * Regression test: ensures SecretUtil.toSecret() preserves null nonce from NUT-10 JSON.
     * This is critical for BDHKE verification to work correctly.
     */
    @Test
    void shouldPreserveNullNonceFromNut10JsonArray() {
        // Given: NUT-10 JSON with null nonce
        String json = "[\"VOUCHER\",\"746573742d64617461\",null,[]]";

        // When: Parse to Secret
        Secret secret = SecretUtil.toSecret(json);

        // Then: Nonce should be Java null, not the string "null"
        assertThat(secret).isInstanceOf(WellKnownSecret.class);
        WellKnownSecret wks = (WellKnownSecret) secret;
        assertThat(wks.getNonce())
                .withFailMessage("Nonce should be Java null, not string 'null'")
                .isNull();

        // And: Re-serialized JSON should contain JSON null, not string "null"
        String output = secret.toString();
        assertThat(output)
                .withFailMessage("Output should contain JSON null: %s", output)
                .contains(",null,");
        assertThat(output)
                .withFailMessage("Output should NOT contain string 'null': %s", output)
                .doesNotContain(",\"null\",");
    }

    /**
     * Ensures that a string nonce is preserved correctly.
     */
    @Test
    void shouldPreserveStringNonceFromNut10JsonArray() {
        // Given: NUT-10 JSON with string nonce
        String json = "[\"VOUCHER\",\"746573742d64617461\",\"my-nonce-123\",[]]";

        // When: Parse to Secret
        Secret secret = SecretUtil.toSecret(json);

        // Then: Nonce should be preserved
        assertThat(secret).isInstanceOf(WellKnownSecret.class);
        WellKnownSecret wks = (WellKnownSecret) secret;
        assertThat(wks.getNonce()).isEqualTo("my-nonce-123");
    }
}
