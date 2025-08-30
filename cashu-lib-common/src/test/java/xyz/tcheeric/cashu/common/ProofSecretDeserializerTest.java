package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.P2PKSecret;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.RandomStringSecret;

import static org.junit.jupiter.api.Assertions.*;

public class ProofSecretDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void deserializeRandomStringSecretProof() throws Exception {
        RandomStringSecret secret = RandomStringSecret.create();
        String json = "{\"amount\":1,\"secret\":\"" + secret + "\",\"id\":\"keyset\"}";
        Proof<?> proof = mapper.readValue(json, Proof.class);
        assertTrue(proof.getSecret() instanceof RandomStringSecret);
        assertEquals(secret.toString(), proof.getSecret().toString());
    }

    @Test
    public void deserializeP2PKSecretProof() throws Exception {
        String json = "{\"amount\":1,\"secret\":{\"kind\":\"P2PK\",\"nonce\":\"abc\",\"data\":\"deadbeef\"},\"id\":\"keyset\"}";
        Proof<?> proof = mapper.readValue(json, Proof.class);
        assertTrue(proof.getSecret() instanceof P2PKSecret);
        P2PKSecret secret = (P2PKSecret) proof.getSecret();
        assertEquals("abc", secret.getNonce());
        assertArrayEquals(Hex.decode("deadbeef"), secret.getData());
    }
}
