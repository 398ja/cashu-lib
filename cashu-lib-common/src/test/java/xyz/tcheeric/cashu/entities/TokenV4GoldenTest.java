package xyz.tcheeric.cashu.entities;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Interop anchor: pins a fixed, definite-length-CBOR-encoded TokenV4 as a committed golden
 * vector ({@code src/test/resources/golden/v4-definite-token.txt}) and proves (via a companion
 * node script, {@code src/test/resources/golden/verify-cashu-ts.mjs}, documented in the sibling
 * README.md) that cashu-ts's {@code getDecodedToken()} can decode the Java-encoded token.
 *
 * <p>If the encoder's output ever changes, this test fails first -- re-run the node verification
 * script against the new vector before re-pinning it.
 */
class TokenV4GoldenTest {

    private static final String GOLDEN_RESOURCE = "/golden/v4-definite-token.txt";

    private static TokenV4 sample() {
        TokenV4 token = new TokenV4();
        token.setMintUrl("https://mint.staging.398ja.xyz");
        token.setUnit("sat");
        // memo intentionally left null

        TokenV4.TokenData.TokenProof proof0 = new TokenV4.TokenData.TokenProof();
        proof0.setAmount(1);
        proof0.setSecret("[\"VOUCHER\",\"bf69fa9c0227e8be1c0e05f2b2f52a2e0dfb3a24c9d4f2f5e60a710d8c5f4b3e\",\"nonce\",[[\"unit\",\"sat\"]]]");
        proof0.setSignature(Utils.hexStringToBytes("02" + "aabbccdd".repeat(8)));
        proof0.setDleqProof(null);

        TokenV4.TokenData.TokenProof proof1 = new TokenV4.TokenData.TokenProof();
        proof1.setAmount(256);
        proof1.setSecret("s1");
        proof1.setSignature(Utils.hexStringToBytes("03" + "1122334455667788".repeat(4)));
        proof1.setDleqProof(null);

        TokenV4.TokenData tokenData = new TokenV4.TokenData(
                Utils.hexStringToBytes("00e3372e61d05605"),
                new ArrayList<>(List.of(proof0, proof1))
        );

        token.setTokenDataList(new ArrayList<>(List.of(tokenData)));
        return token;
    }

    @Test
    void serializeMatchesPinnedCashuTsInteropVector() throws IOException {
        TokenV4 t = sample();

        String s = t.serialize(false);

        String pinned = readGoldenResource();
        assertThat(s).isEqualTo(pinned);
    }

    private static String readGoldenResource() throws IOException {
        try (InputStream in = TokenV4GoldenTest.class.getResourceAsStream(GOLDEN_RESOURCE)) {
            if (in == null) {
                throw new IOException("Golden resource not found on classpath: " + GOLDEN_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }
}
