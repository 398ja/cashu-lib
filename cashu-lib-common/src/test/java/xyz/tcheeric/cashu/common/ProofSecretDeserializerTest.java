package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProofSecretDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // Real compressed secp256k1 points (G, 2G, 3G, 4G). These fixtures previously used
    // "deadbeef" as the lock and 02||"22"*32 style filler as refund keys; NUT-11 validation
    // now checks length, prefix and curve membership, and filler x-coordinates are not points.
    private static final String KEY_LOCK =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String KEY_2 =
            "02c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";
    private static final String KEY_3 =
            "02f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9";
    private static final String KEY_4 =
            "02e493dbf1c10d80f3581e4904930b1404cc6c13900ee0758474fa94abe8c4cd13";

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
        String json = "{\"amount\":1,\"secret\":{\"kind\":\"P2PK\",\"nonce\":\"abc\",\"data\":\""
                + KEY_LOCK + "\"},\"id\":\"keyset\"}";

        // Act
        Proof<?> proof = mapper.readValue(json, Proof.class);

        // Assert
        assertTrue(proof.getSecret() instanceof P2PKSecret);
        P2PKSecret secret = (P2PKSecret) proof.getSecret();
        assertEquals("abc", secret.getNonce());
        assertArrayEquals(Hex.decode(KEY_LOCK), secret.getData());
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
        // Three refund keys, not two: NUT-11 makes a threshold exceeding its pathway's key
        // count malformed, so n_sigs_refund of 3 needs three keys to back it.
        P2PKSecret secret = new P2PKSecret(Hex.decode(KEY_LOCK));
        secret.setNSigsRefund(3);
        secret.setRefund(List.of(KEY_2, KEY_3, KEY_4));
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
