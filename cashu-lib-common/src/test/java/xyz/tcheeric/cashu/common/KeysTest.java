package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class KeysTest {

    // Ensures Keys serializes to JSON and deserializes back with identical content
    @Test
    public void serializeDeserializeRoundTrip() throws Exception {
        PublicKey pk = PublicKey.fromString("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2");
        Keys keys = new Keys().put(BigInteger.ONE, pk);

        String json = JsonUtils.JSON_MAPPER.writeValueAsString(keys);
        Keys parsed = JsonUtils.JSON_MAPPER.readValue(json, Keys.class);

        assertEquals(1, parsed.getValues().size());
        assertTrue(parsed.getValues().containsKey(BigInteger.ONE));
        assertEquals(pk.toString(), parsed.getValues().get(BigInteger.ONE).toString());

        Map<BigInteger, byte[]> bytesMap = parsed.values();
        assertArrayEquals(pk.toBytes(), bytesMap.get(BigInteger.ONE));
    }
}

