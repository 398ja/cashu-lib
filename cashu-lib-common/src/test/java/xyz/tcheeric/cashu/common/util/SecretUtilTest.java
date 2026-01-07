package xyz.tcheeric.cashu.common.util;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.VoucherSecret;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SecretUtilTest {

    private static final String HEX_SECRET =
            "0000000000000000000000000000000000000000000000000000000000000001";
    private static final String EXPECTED_Y_FOR_HEX_SECRET =
            "022e7158e11c9506f1aa4248bf531298daa7febd6194f003edcd9b93ade6253acf";

    /**
     * Ensures NUT-00 hex secrets are hex-decoded before hash_to_curve, matching protocol vectors.
     */
    @Test
    void shouldComputeYFromHexSecretUsingHexDecoding() {
        RandomStringSecret secret = RandomStringSecret.fromString(HEX_SECRET);

        String actualY = SecretUtil.toY(secret);
        String expectedY = PublicKey.fromBytes(BDHKEUtils.hashToCurve(HEX_SECRET)).toString();

        assertThat(actualY).isEqualTo(expectedY);
        assertThat(actualY).isEqualTo(EXPECTED_Y_FOR_HEX_SECRET);
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
}
