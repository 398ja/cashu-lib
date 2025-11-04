package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProofSecretDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    /**
     * Ensures proofs with string secrets deserialize to RandomStringSecret instances.
     */
    void shouldDeserializeRandomStringSecretProof() throws Exception {
        // Arrange
        RandomStringSecret secret = RandomStringSecret.create();
        String json = "{\"amount\":1,\"secret\":\"" + secret + "\",\"id\":\"keyset\"}";

        // Act
        Proof<?> proof = mapper.readValue(json, Proof.class);

        // Assert
        assertTrue(proof.getSecret() instanceof RandomStringSecret);
        assertEquals(secret.toString(), proof.getSecret().toString());
    }

    @Test
    /**
     * Ensures proofs with P2PK secret payloads deserialize to P2PKSecret instances.
     */
    void shouldDeserializeP2PKSecretProof() throws Exception {
        // Arrange
        String json = "{\"amount\":1,\"secret\":{\"kind\":\"P2PK\",\"nonce\":\"abc\",\"data\":\"deadbeef\"},\"id\":\"keyset\"}";

        // Act
        Proof<?> proof = mapper.readValue(json, Proof.class);

        // Assert
        assertTrue(proof.getSecret() instanceof P2PKSecret);
        P2PKSecret secret = (P2PKSecret) proof.getSecret();
        assertEquals("abc", secret.getNonce());
        assertArrayEquals(Hex.decode("deadbeef"), secret.getData());
    }
}
