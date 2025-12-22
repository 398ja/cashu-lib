package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeysetIdTest {

    @Test
    /**
     * Ensures valid hex strings round-trip through the KeysetId type and JSON serialization.
     */
    void shouldParseValidKeysetId() throws Exception {
        // Arrange
        String keysetIdHex = "009a1f293253e41e";
        ObjectMapper mapper = new ObjectMapper();

        // Act
        KeysetId keysetId = KeysetId.fromString(keysetIdHex);
        KeysetId fromJson = mapper.readValue("\"" + keysetIdHex + "\"", KeysetId.class);

        // Assert
        assertEquals(keysetIdHex, keysetId.toString());
        assertEquals(keysetId, fromJson);
    }

    @Test
    /**
     * Ensures keyset IDs reject non-hex characters.
     */
    void shouldRejectNonHexCharacters() {
        // Arrange
        String invalidHex = "zzzzzzzzzzzzzzzz";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> KeysetId.fromString(invalidHex));
    }

    @Test
    /**
     * Ensures keyset IDs reject strings with invalid length.
     */
    void shouldRejectInvalidLength() {
        // Arrange
        String invalidLength = "1234abcd";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> KeysetId.fromString(invalidLength));
    }
}
