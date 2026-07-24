package xyz.tcheeric.cashu.entities;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.common.TokenV4CborEncoder;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenV4CborEncoderTest {

    private static TokenV4 sample(boolean withMemo) {
        TokenV4 token = new TokenV4();
        token.setMintUrl("https://mint.x");
        token.setUnit("sat");
        if (withMemo) {
            token.setMemo("hi");
        }

        TokenV4.TokenData.TokenProof.DLEQProof dleq = new TokenV4.TokenData.TokenProof.DLEQProof();
        dleq.setE(Utils.hexStringToBytes("01"));
        dleq.setS(Utils.hexStringToBytes("02"));
        dleq.setR(Utils.hexStringToBytes("03"));

        TokenV4.TokenData.TokenProof proof0 = new TokenV4.TokenData.TokenProof();
        proof0.setAmount(1);
        proof0.setSecret("s0");
        proof0.setSignature(Utils.hexStringToBytes("aa"));
        proof0.setDleqProof(dleq);

        TokenV4.TokenData.TokenProof proof1 = new TokenV4.TokenData.TokenProof();
        proof1.setAmount(256);
        proof1.setSecret("s1");
        proof1.setSignature(Utils.hexStringToBytes("bb"));

        TokenV4.TokenData tokenData = new TokenV4.TokenData(
                Utils.hexStringToBytes("00e3372e61d05605"),
                new ArrayList<>(List.of(proof0, proof1))
        );

        token.setTokenDataList(new ArrayList<>(List.of(tokenData)));
        return token;
    }

    @Test
    /**
     * Ensures the top-level map is definite-length and carries the "d" (memo) key when memo is present.
     */
    void shouldEncodeDefiniteLengthTopMapWithMemo() {
        // Act
        byte[] b = TokenV4CborEncoder.encode(sample(true));

        // Assert
        assertThat(b[0] & 0xFF).isNotEqualTo(0xBF);
        assertThat(b[0] & 0xE0).isEqualTo(0xA0);
        assertThat(b[0] & 0xFF).isEqualTo(0xA4);
    }

    @Test
    /**
     * Ensures no indefinite-length markers (0xBF map, 0x9F array, 0xFF break) appear anywhere in the output.
     */
    void shouldContainNoIndefiniteLengthMarkers() {
        // Act
        byte[] b = TokenV4CborEncoder.encode(sample(true));

        // Assert
        for (byte value : b) {
            int u = value & 0xFF;
            assertThat(u).isNotEqualTo(0xBF);
            assertThat(u).isNotEqualTo(0x9F);
            assertThat(u).isNotEqualTo(0xFF);
        }
    }

    @Test
    /**
     * Ensures the top-level map omits the "d" (memo) key and shrinks to 3 keys when memo is absent.
     */
    void shouldEncodeThreeKeyTopMapWithoutMemo() {
        // Act
        byte[] b = TokenV4CborEncoder.encode(sample(false));

        // Assert
        assertThat(b[0] & 0xFF).isEqualTo(0xA3);
    }
}
