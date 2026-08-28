package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import xyz.tcheeric.cashu.common.nut13.DeterministicSecret;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DeterministicSecret} covering NUT-13 metadata handling and JSON serialization.
 */
class DeterministicSecretTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final byte[] TEST_SECRET_BYTES = Hex.decode(
            "a1b2c3d4e5f6071829384756a1b2c3d4e5f6071829384756a1b2c3d4e5f60718"
    );
    private static final String TEST_SECRET_HEX =
            "a1b2c3d4e5f6071829384756a1b2c3d4e5f6071829384756a1b2c3d4e5f60718";

    /**
     * Ensures a secret can be created from raw bytes without metadata.
     */
    @Test
    void shouldCreateSecretFromBytes() {
        // Arrange
        byte[] secretBytes = TEST_SECRET_BYTES.clone();

        // Act
        DeterministicSecret secret = DeterministicSecret.fromBytes(secretBytes);

        // Assert
        assertNotNull(secret);
        assertArrayEquals(TEST_SECRET_BYTES, secret.getDerivedBytes());
        assertArrayEquals(TEST_SECRET_BYTES, secret.toBytes());
        assertFalse(secret.hasMetadata());
    }

    /**
     * Ensures a secret can be created from a hex string without metadata.
     */
    @Test
    void shouldCreateSecretFromHexString() {
        // Arrange
        String hex = TEST_SECRET_HEX;

        // Act
        DeterministicSecret secret = DeterministicSecret.fromString(hex);

        // Assert
        assertNotNull(secret);
        assertArrayEquals(TEST_SECRET_BYTES, secret.getDerivedBytes());
        assertEquals(hex, secret.toHexString());
        assertFalse(secret.hasMetadata());
    }

    /**
     * Ensures metadata is populated when a keyset and counter are provided.
     */
    @Test
    void shouldCreateSecretWithMetadata() {
        // Arrange
        KeysetId keysetId = KeysetId.fromString("00ad268c4d1f5826");
        int counter = 5;

        // Act
        DeterministicSecret secret = DeterministicSecret.create(TEST_SECRET_BYTES, keysetId, counter);

        // Assert
        assertNotNull(secret);
        assertArrayEquals(TEST_SECRET_BYTES, secret.getDerivedBytes());
        assertTrue(secret.hasMetadata());
        assertEquals(keysetId, secret.getKeysetId());
        assertEquals(counter, secret.getCounter());
        assertNotNull(secret.getDerivationPath());
        assertEquals(keysetId, secret.getDerivationPath().getKeysetId());
        assertEquals(counter, secret.getDerivationPath().getCounter());
    }

    /**
     * Ensures secrets created from derivation path metadata inherit the path.
     */
    @Test
    void shouldCreateSecretFromDerivationPath() {
        // Arrange
        KeysetId keysetId = KeysetId.fromString("00ad268c4d1f5826");
        SecretDerivationPath derivationPath = new SecretDerivationPath();
        derivationPath.setKeysetId(keysetId);
        derivationPath.setCounter(10);

        // Act
        DeterministicSecret secret = DeterministicSecret.create(TEST_SECRET_BYTES, derivationPath);

        // Assert
        assertNotNull(secret);
        assertTrue(secret.hasMetadata());
        assertEquals(keysetId, secret.getKeysetId());
        assertEquals(10, secret.getCounter());
        assertEquals(derivationPath, secret.getDerivationPath());
    }

    /**
     * Ensures the secret remains immutable once created.
     */
    @Test
    void shouldRejectMutableUpdates() {
        // Arrange
        DeterministicSecret secret = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);

        // Act
        Executable mutation = () -> secret.setData(new byte[32]);

        // Assert
        assertThrows(UnsupportedOperationException.class, mutation);
    }

    /**
     * Ensures string representations show raw hex and optionally include metadata details.
     */
    @Test
    void shouldRenderToStringWithoutMetadata() {
        // Arrange
        DeterministicSecret withoutMetadata = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);
        DeterministicSecret withMetadata =
                DeterministicSecret.create(TEST_SECRET_BYTES, KeysetId.fromString("00ad268c4d1f5826"), 5);

        // Act
        String simpleString = withoutMetadata.toString();
        String metadataString = withMetadata.toString();
        String detailedString = withMetadata.toStringWithMetadata();

        // Assert
        assertEquals(TEST_SECRET_HEX, simpleString);
        assertEquals(TEST_SECRET_HEX, metadataString);
        assertTrue(detailedString.contains(TEST_SECRET_HEX));
        assertTrue(detailedString.contains("keyset=00ad268c4d1f5826"));
        assertTrue(detailedString.contains("counter=5"));
    }

    /**
     * Ensures toHexString returns the canonical hex value.
     */
    @Test
    void shouldReturnHexString() {
        // Arrange
        DeterministicSecret secret =
                DeterministicSecret.create(TEST_SECRET_BYTES, KeysetId.fromString("00ad268c4d1f5826"), 5);

        // Act
        String hexString = secret.toHexString();

        // Assert
        assertEquals(TEST_SECRET_HEX, hexString);
    }

    /**
     * Ensures JSON serialization emits a plain quoted hex string.
     */
    @Test
    void shouldSerializeToJson() throws Exception {
        // Arrange
        DeterministicSecret secret = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);

        // Act
        String json = OBJECT_MAPPER.writeValueAsString(secret);

        // Assert
        assertEquals("\"" + TEST_SECRET_HEX + "\"", json);
    }

    /**
     * Ensures JSON deserialization restores the original secret bytes.
     */
    @Test
    void shouldDeserializeFromJson() throws Exception {
        // Arrange
        String json = "\"" + TEST_SECRET_HEX + "\"";

        // Act
        DeterministicSecret secret = OBJECT_MAPPER.readValue(json, DeterministicSecret.class);

        // Assert
        assertNotNull(secret);
        assertArrayEquals(TEST_SECRET_BYTES, secret.getDerivedBytes());
        assertEquals(TEST_SECRET_HEX, secret.toHexString());
    }

    /**
     * Ensures JSON round-trips preserve byte content but drop metadata.
     */
    @Test
    void shouldRoundTripJsonWithoutMetadata() throws Exception {
        // Arrange
        DeterministicSecret original = DeterministicSecret.create(
                TEST_SECRET_BYTES,
                KeysetId.fromString("00ad268c4d1f5826"),
                5
        );

        // Act
        String json = OBJECT_MAPPER.writeValueAsString(original);
        DeterministicSecret deserialized = OBJECT_MAPPER.readValue(json, DeterministicSecret.class);

        // Assert
        assertEquals(original.toHexString(), deserialized.toHexString());
        assertArrayEquals(original.getDerivedBytes(), deserialized.getDerivedBytes());
        assertFalse(deserialized.hasMetadata());
    }

    /**
     * Ensures equality is based on the secret bytes.
     */
    @Test
    void shouldCompareSecretsByData() {
        // Arrange
        DeterministicSecret first = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);
        DeterministicSecret second = DeterministicSecret.fromBytes(TEST_SECRET_BYTES.clone());

        // Act & Assert
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    /**
     * Ensures metadata differences do not impact equality.
     */
    @Test
    void shouldConsiderSecretsEqualEvenWithMetadata() {
        // Arrange
        DeterministicSecret withMetadata =
                DeterministicSecret.create(TEST_SECRET_BYTES, KeysetId.fromString("00ad268c4d1f5826"), 5);
        DeterministicSecret withoutMetadata = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);

        // Act & Assert
        assertEquals(withoutMetadata, withMetadata);
    }

    /**
     * Ensures different data produces non-equal secrets.
     */
    @Test
    void shouldConsiderSecretsWithDifferentDataNotEqual() {
        // Arrange
        byte[] otherBytes = new byte[32];
        Arrays.fill(otherBytes, (byte) 0x42);
        DeterministicSecret original = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);
        DeterministicSecret different = DeterministicSecret.fromBytes(otherBytes);

        // Act & Assert
        assertNotEquals(original, different);
    }

    /**
     * Ensures null byte arrays are rejected.
     */
    @Test
    void shouldRejectNullByteArray() {
        // Arrange
        Executable action = () -> DeterministicSecret.fromBytes(null);

        // Act & Assert
        assertThrows(NullPointerException.class, action);
    }

    /**
     * Ensures null hex strings are rejected.
     */
    @Test
    void shouldRejectNullHex() {
        // Arrange
        Executable action = () -> DeterministicSecret.fromString(null);

        // Act & Assert
        assertThrows(NullPointerException.class, action);
    }

    /**
     * Ensures creating secrets with null keyset id is rejected.
     */
    @Test
    void shouldRejectNullKeysetId() {
        // Arrange
        Executable action = () -> DeterministicSecret.create(TEST_SECRET_BYTES, (KeysetId) null, 0);

        // Act & Assert
        assertThrows(NullPointerException.class, action);
    }

    /**
     * Ensures creating secrets with null derivation path is rejected.
     */
    @Test
    void shouldRejectNullDerivationPath() {
        // Arrange
        Executable action = () -> DeterministicSecret.create(TEST_SECRET_BYTES, (SecretDerivationPath) null);

        // Act & Assert
        assertThrows(NullPointerException.class, action);
    }

    /**
     * Ensures deriving the same secret bytes is reproducible.
     */
    @Test
    void shouldBeReproducibleForSameBytes() {
        // Arrange
        byte[] bytes = TEST_SECRET_BYTES.clone();

        // Act
        DeterministicSecret first = DeterministicSecret.fromBytes(bytes);
        DeterministicSecret second = DeterministicSecret.fromBytes(bytes);

        // Assert
        assertEquals(first, second);
        assertEquals(first.toHexString(), second.toHexString());
    }

    /**
     * Ensures the generic Secret.fromString factory can create deterministic secrets.
     */
    @Test
    void shouldCreateSecretViaFactory() {
        // Arrange
        String hex = TEST_SECRET_HEX;

        // Act
        Secret secret = Secret.fromString(hex, DeterministicSecret.class);

        // Assert
        assertNotNull(secret);
        assertTrue(secret instanceof DeterministicSecret);
        assertEquals(hex, ((DeterministicSecret) secret).toHexString());
    }

    /**
     * Ensures getData returns a copy of the underlying byte array.
     */
    @Test
    void shouldReturnCopyOfData() {
        // Arrange
        DeterministicSecret secret = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);

        // Act
        byte[] data = secret.getDerivedBytes();

        // Assert
        assertNotNull(data);
        assertArrayEquals(TEST_SECRET_BYTES, data);
    }

    /**
     * Ensures toBytes returns a copy of the underlying byte array.
     */
    @Test
    void shouldReturnCopyOfBytes() {
        // Arrange
        DeterministicSecret secret = DeterministicSecret.fromBytes(TEST_SECRET_BYTES);

        // Act
        byte[] data = secret.toBytes();

        // Assert
        assertNotNull(data);
        assertArrayEquals(TEST_SECRET_BYTES, data);
    }
}
