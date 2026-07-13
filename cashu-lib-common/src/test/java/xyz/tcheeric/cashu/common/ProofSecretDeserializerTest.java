package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;

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

    /**
     * Regression test for the real mint wire path: when {@code Proof.secret} arrives
     * as a JSON STRING carrying a NUT-10 array (e.g. {@code "[\"P2PK\",...]"}), it is
     * deserialized via {@code SecretDeserializer} -> {@code SecretUtil.toSecret(String)}
     * -> {@code addTagsToSecret} -> {@code SecretUtil}'s own {@code convertP2PKTagValues}
     * — a copy of the numeric-tag coercion that is entirely separate from
     * {@code WellKnownSecretDeserializer}/{@code TagDeserializer} (exercised by
     * {@link #shouldDeserializeP2PKSecretProof()} above, which passes the secret as a
     * JSON object and therefore never touches {@code SecretUtil}). Before the fix, this
     * third copy left n_sigs_refund as a boxed Long, so a mint parsing this exact Proof
     * JSON on the refund-authorization path would throw ClassCastException the moment
     * getNSigsRefund() cast it to Integer.
     */
    @Test
    void shouldDeserializeNSigsRefundOnProofSecretUtilWirePath() throws Exception {
        // Arrange: build a P2PK secret carrying n_sigs_refund + refund keys, then embed
        // its NUT-10 array serialization as a JSON *string* value of "secret" — this is
        // what routes through SecretUtil.toSecret(String) rather than the object-format
        // WellKnownSecretDeserializer path.
        P2PKSecret secret = new P2PKSecret(Hex.decode("deadbeef"));
        secret.setNSigsRefund(3);
        secret.addRefund("02" + "22".repeat(32));
        secret.addRefund("02" + "33".repeat(32));
        String secretJson = secret.toString();

        ObjectNode proofNode = mapper.createObjectNode();
        proofNode.put("amount", 1);
        proofNode.put("secret", secretJson);
        proofNode.put("id", "keyset");
        String proofJson = mapper.writeValueAsString(proofNode);

        // Act
        Proof<?> proof = mapper.readValue(proofJson, Proof.class);

        // Assert: must not throw ClassCastException (Long -> Integer)
        assertTrue(proof.getSecret() instanceof P2PKSecret);
        assertEquals(3, ((P2PKSecret) proof.getSecret()).getNSigsRefund());
    }
}
