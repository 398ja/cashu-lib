package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import xyz.tcheeric.cashu.common.KeySet;

public class KeySetDeserializer extends JsonDeserializer<KeySet> {

  @Override
  public KeySet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.readValueAsTree();
    if (node.isObject()) {
      ObjectMapper mapper = (ObjectMapper) p.getCodec();
      return mapper.readValue(node.toString(), KeySet.class);
    }
    throw new RuntimeException("Invalid Keys format");
  }
}
