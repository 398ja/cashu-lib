package xyz.tcheeric.cashu.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.P2PKSecret;
import xyz.tcheeric.cashu.common.WellKnownSecret;

import static org.assertj.core.api.Assertions.assertThat;

class P2PKSecretTest {

    @Test
    /**
     * Ensures P2PK secrets round-trip through JSON serialization with all metadata intact.
     */
    void shouldRoundTripSerializeP2PKSecret() throws Exception {
        // Arrange
        byte[] secretData = Hex.decode("deadbeef");
        P2PKSecret secret = new P2PKSecret(secretData);
        secret.setNSigs(2);
        secret.setSigFlag(P2PKSecret.SignatureFlag.SIG_ALL);
        secret.addPubKey("pk1");
        secret.setLockTime(42);
        secret.addRefund("refund1");
        ObjectMapper mapper = new ObjectMapper();

        // Act
        String json = mapper.writeValueAsString(secret);
        WellKnownSecret deserialized = mapper.readValue(json, WellKnownSecret.class);

        // Assert
        assertThat(deserialized).isEqualTo(secret);
    }
}
