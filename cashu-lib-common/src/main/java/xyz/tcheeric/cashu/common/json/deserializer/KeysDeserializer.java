package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.cashu.common.Keys;
import xyz.tcheeric.cashu.common.PublicKey;

import java.util.Map;

import java.io.IOException;

public class KeysDeserializer extends JsonDeserializer<Keys> {
    @Override
    public Keys deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            var map = mapper.readValue(node.toString(), new TypeReference<Map<String, String>>() {});
            Keys keys = new Keys();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                keys.put(new java.math.BigInteger(entry.getKey()), PublicKey.fromString(entry.getValue()));
            }
            return keys;
        }
        throw new RuntimeException("Invalid Keys format");
    }
}
