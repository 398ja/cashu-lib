package xyz.tcheeric.cashu.entities;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenV4RoundTripTest {

    private static TokenV4 sample() {
        TokenV4 token = new TokenV4();
        token.setMintUrl("https://mint.x");
        token.setUnit("sat");
        token.setMemo("hi");

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
    void serializeProducesDefiniteLengthAndRoundTrips() {
        TokenV4 t = sample();

        String s = t.serialize(false);
        String b64 = s.substring("cashuB".length());
        byte[] cbor = Base64.getUrlDecoder().decode(b64);

        // definite-length map, not indefinite (0xbf)
        assertThat(cbor[0] & 0xFF).isNotEqualTo(0xBF);
        assertThat(cbor[0] & 0xE0).isEqualTo(0xA0);

        TokenV4 back = TokenV4.deserialize(s);

        assertThat(back.getMintUrl()).isEqualTo(t.getMintUrl());
        assertThat(back.getUnit()).isEqualTo(t.getUnit());
        assertThat(back.getTokenDataList().size()).isEqualTo(t.getTokenDataList().size());

        // spot-check a proof amount+secret survive the round trip
        TokenV4.TokenData.TokenProof restoredProof0 = back.getTokenDataList().get(0).getProofs().get(0);
        assertThat(restoredProof0.getAmount()).isEqualTo(1);
        assertThat(restoredProof0.getSecret()).isEqualTo("s0");

        TokenV4.TokenData.TokenProof restoredProof1 = back.getTokenDataList().get(0).getProofs().get(1);
        assertThat(restoredProof1.getAmount()).isEqualTo(256);
        assertThat(restoredProof1.getSecret()).isEqualTo("s1");
    }
}
