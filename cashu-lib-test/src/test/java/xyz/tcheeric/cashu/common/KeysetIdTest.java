package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KeysetIdTest {

    @Test
    public void validKeysetId() throws Exception {
        String value = "009a1f293253e41e";
        KeysetId id = KeysetId.fromString(value);
        assertEquals(value, id.toString());

        ObjectMapper mapper = new ObjectMapper();
        KeysetId fromJson = mapper.readValue("\"" + value + "\"", KeysetId.class);
        assertEquals(id, fromJson);
    }

    @Test
    public void invalidHexKeysetId() {
        assertThrows(IllegalArgumentException.class, () -> KeysetId.fromString("zzzzzzzzzzzzzzzz"));
    }

    @Test
    public void invalidLengthKeysetId() {
        assertThrows(IllegalArgumentException.class, () -> KeysetId.fromString("1234abcd"));
    }
}
