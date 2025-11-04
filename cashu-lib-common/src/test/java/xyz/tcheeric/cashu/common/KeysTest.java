package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeysTest {

    @Test
    /**
     * Ensures Keys serializes and deserializes without losing entries or mutating values.
     */
    void shouldPreserveEntriesWhenSerializedAndDeserialized() throws Exception {
        // Arrange
        PublicKey publicKey = PublicKey.fromString("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2");
        Keys keys = new Keys().put(BigInteger.ONE, publicKey);

        // Act
        String json = JsonUtils.JSON_MAPPER.writeValueAsString(keys);
        Keys parsedKeys = JsonUtils.JSON_MAPPER.readValue(json, Keys.class);
        Map<BigInteger, byte[]> parsedBytes = parsedKeys.values();

        // Assert
        assertEquals(1, parsedKeys.getValues().size());
        assertTrue(parsedKeys.getValues().containsKey(BigInteger.ONE));
        assertEquals(publicKey.toString(), parsedKeys.getValues().get(BigInteger.ONE).toString());
        assertArrayEquals(publicKey.toBytes(), parsedBytes.get(BigInteger.ONE));
    }
}
