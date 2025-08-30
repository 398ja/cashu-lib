package xyz.tcheeric.cashu.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.P2PKSecret;
import xyz.tcheeric.cashu.common.WellKnownSecret;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class P2PKSecretTest {

    @Test
    public void roundTripSerialization() throws Exception {
        byte[] data = Hex.decode("deadbeef");
        P2PKSecret secret = new P2PKSecret(data);
        secret.setNSigs(2);
        secret.setSigFlag(P2PKSecret.SignatureFlag.SIG_ALL);
        secret.addPubKey("pk1");
        secret.setLockTime(42);
        secret.addRefund("refund1");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(secret);
        WellKnownSecret deserialized = mapper.readValue(json, WellKnownSecret.class);

        assertThat(deserialized).isEqualTo(secret);
    }
}
