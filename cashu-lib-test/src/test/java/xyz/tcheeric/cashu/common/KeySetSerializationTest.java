package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.json.serializer.KeySetSerializer;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeySetSerializationTest {

    @Test
    public void keySetSerializesToValidJson() throws Exception {
        Keys keys = new Keys();
        keys.put(BigInteger.ONE, PublicKey.fromString("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2"));

        KeySet keySet = KeySet.builder()
                .id("id")
                .unit("sat")
                .keys(keys)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(KeySet.class, new KeySetSerializer());
        mapper.registerModule(module);

        String json = mapper.writeValueAsString(keySet);
        assertTrue(mapper.readTree(json).isObject());
    }
}
