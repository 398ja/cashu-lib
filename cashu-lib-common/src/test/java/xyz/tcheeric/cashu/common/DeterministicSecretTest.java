package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DeterministicSecret}.
 * Tests NUT-13 deterministic secret generation and serialization.
 *
 * @author NUT-13 Implementation Team
 * @since 1.0.0
 */
class DeterministicSecretTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Test data: 32 bytes of deterministically derived data (simulated)
    private static final byte[] TEST_SECRET_BYTES = Hex.decode(
        "a1b2c3d4e5f6071829384756a1b2c3d4e5f6071829384756a1b2c3d4e5f60718"
    );

    private static final String TEST_SECRET_HEX = "a1b2c3d4e5f6071829384756a1b2c3d4e5f6071829384756a1b2c3d4e5f60718";

    @Test
    void testFromBytes() {
        DeterministicSecret secret = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);

        assertNotNull(secret);
        assertArrayEquals(TEST_SECRET_BYTES, secret.getData());
        assertArrayEquals(TEST_SECRET_BYTES, secret.toBytes());
        assertFalse(secret.hasMetadata());
    }

    @Test
    void testFromString() {
        DeterministicSecret secret = DeterministicSecret.fromString(TEST_SECRET_HEX);

        assertNotNull(secret);
        assertArrayEquals(TEST_SECRET_BYTES, secret.getData());
        assertEquals(TEST_SECRET_HEX, secret.toHexString());
        assertFalse(secret.hasMetadata());
    }

    @Test
    void testCreateWithMetadata() {
        KeysetId keysetId = KeysetId.fromString("00ad268c4d1f5826");
        int counter = 5;

        DeterministicSecret secret = DeterministicSecret.create(TEST_SECRET_BYTES, keysetId, counter);

        assertNotNull(secret);
        assertArrayEquals(TEST_SECRET_BYTES, secret.getData());
        assertTrue(secret.hasMetadata());
        assertEquals(keysetId, secret.getKeysetId());
        assertEquals(counter, secret.getCounter());
        assertNotNull(secret.getDerivationPath());
        assertEquals(keysetId, secret.getDerivationPath().getKeysetId());
        assertEquals(counter, secret.getDerivationPath().getCounter());
    }

    @Test
    void testCreateWithDerivationPath() {
        KeysetId keysetId = KeysetId.fromString("00ad268c4d1f5826");
        SecretDerivationPath path = new SecretDerivationPath();
        path.setKeysetId(keysetId);
        path.setCounter(10);

        DeterministicSecret secret = DeterministicSecret.create(TEST_SECRET_BYTES, path);

        assertNotNull(secret);
        assertTrue(secret.hasMetadata());
        assertEquals(keysetId, secret.getKeysetId());
        assertEquals(10, secret.getCounter());
        assertEquals(path, secret.getDerivationPath());
    }

    @Test
    void testImmutability() {
        DeterministicSecret secret = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);

        // Attempting to modify the secret should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> {
            secret.setData(new byte[32]);
        });
    }

    @Test
    void testToString() {
        // toString() always returns just the hex
        DeterministicSecret secret1 = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);
        assertEquals(TEST_SECRET_HEX, secret1.toString());

        // Even with metadata, toString() returns just hex (for JSON serialization)
        KeysetId keysetId = KeysetId.fromString("00ad268c4d1f5826");
        DeterministicSecret secret2 = DeterministicSecret.create(TEST_SECRET_BYTES, keysetId, 5);
        assertEquals(TEST_SECRET_HEX, secret2.toString());

        // toStringWithMetadata() includes the metadata
        String toStringWithMetadata = secret2.toStringWithMetadata();
        assertTrue(toStringWithMetadata.contains(TEST_SECRET_HEX));
        assertTrue(toStringWithMetadata.contains("keyset=" + keysetId));
        assertTrue(toStringWithMetadata.contains("counter=5"));
    }

    @Test
    void testToHexString() {
        KeysetId keysetId = KeysetId.fromString("00ad268c4d1f5826");
        DeterministicSecret secret = DeterministicSecret.create(TEST_SECRET_BYTES, keysetId, 5);

        // toHexString should return only the hex without metadata
        assertEquals(TEST_SECRET_HEX, secret.toHexString());
    }

    @Test
    void testJsonSerialization() throws Exception {
        DeterministicSecret secret = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(secret);

        // Should serialize as a simple quoted string (hex)
        assertEquals("\"" + TEST_SECRET_HEX + "\"", json);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "\"" + TEST_SECRET_HEX + "\"";

        // Deserialize from JSON
        DeterministicSecret secret = objectMapper.readValue(json, DeterministicSecret.class);

        assertNotNull(secret);
        assertArrayEquals(TEST_SECRET_BYTES, secret.getData());
        assertEquals(TEST_SECRET_HEX, secret.toHexString());
    }

    @Test
    void testJsonRoundTrip() throws Exception {
        DeterministicSecret original = DeterministicSecret.create(
            TEST_SECRET_BYTES,
            KeysetId.fromString("00ad268c4d1f5826"),
            5
        );

        // Serialize
        String json = objectMapper.writeValueAsString(original);

        // Deserialize
        DeterministicSecret deserialized = objectMapper.readValue(json, DeterministicSecret.class);

        // Compare (note: metadata is not serialized, so only hex should match)
        assertEquals(original.toHexString(), deserialized.toHexString());
        assertArrayEquals(original.getData(), deserialized.getData());

        // Metadata is not preserved in JSON
        assertFalse(deserialized.hasMetadata());
    }

    @Test
    void testEqualityBasedOnBytes() {
        DeterministicSecret secret1 = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);
        DeterministicSecret secret2 = DeterministicSecret.fromBytes(TEST_SECRET_BYTES.clone());

        // Should be equal if bytes are the same (BaseKey uses bytes for equals)
        assertEquals(secret1, secret2);
        assertEquals(secret1.hashCode(), secret2.hashCode());
    }

    @Test
    void testEqualityWithMetadata() {
        KeysetId keysetId = KeysetId.fromString("00ad268c4d1f5826");
        DeterministicSecret secret1 = DeterministicSecret.create(TEST_SECRET_BYTES, keysetId, 5);
        DeterministicSecret secret2 = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);

        // Should be equal even if one has metadata and the other doesn't
        // because equality is based on bytes (BaseKey behavior)
        assertEquals(secret1, secret2);
    }

    @Test
    void testDifferentSecretsNotEqual() {
        byte[] otherBytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            otherBytes[i] = (byte) i;
        }

        DeterministicSecret secret1 = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);
        DeterministicSecret secret2 = DeterministicSecret.fromBytes(otherBytes);

        assertNotEquals(secret1, secret2);
    }

    @Test
    void testNullBytesShouldThrow() {
        assertThrows(NullPointerException.class, () -> {
            DeterministicSecret.fromBytes(null);
        });
    }

    @Test
    void testNullHexStringShouldThrow() {
        assertThrows(NullPointerException.class, () -> {
            DeterministicSecret.fromString(null);
        });
    }

    @Test
    void testNullKeysetIdShouldThrow() {
        assertThrows(NullPointerException.class, () -> {
            DeterministicSecret.create(TEST_SECRET_BYTES, (KeysetId) null, 0);
        });
    }

    @Test
    void testNullDerivationPathShouldThrow() {
        assertThrows(NullPointerException.class, () -> {
            DeterministicSecret.create(TEST_SECRET_BYTES, (SecretDerivationPath) null);
        });
    }

    // Note: Derivation path consistency validation tests removed
    // as they would require accessing private constructor which is an implementation detail

    @Test
    void testReproducibility() {
        // Same bytes should always produce the same secret
        byte[] bytes = TEST_SECRET_BYTES.clone();

        DeterministicSecret secret1 = DeterministicSecret.fromBytes(bytes);
        DeterministicSecret secret2 = DeterministicSecret.fromBytes(bytes);

        assertEquals(secret1, secret2);
        assertEquals(secret1.toHexString(), secret2.toHexString());
    }

    @Test
    void testSecretFromStringIntegration() {
        // Test the Secret.fromString() factory method
        Secret secret = Secret.fromString(TEST_SECRET_HEX, DeterministicSecret.class);

        assertNotNull(secret);
        assertTrue(secret instanceof DeterministicSecret);
        assertEquals(TEST_SECRET_HEX, ((DeterministicSecret) secret).toHexString());
    }

    @Test
    void testGetData() {
        DeterministicSecret secret = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);
        byte[] data = secret.getData();

        assertNotNull(data);
        assertArrayEquals(TEST_SECRET_BYTES, data);
    }

    @Test
    void testToBytes() {
        DeterministicSecret secret = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);
        byte[] bytes = secret.toBytes();

        assertNotNull(bytes);
        assertArrayEquals(TEST_SECRET_BYTES, bytes);
    }
}
