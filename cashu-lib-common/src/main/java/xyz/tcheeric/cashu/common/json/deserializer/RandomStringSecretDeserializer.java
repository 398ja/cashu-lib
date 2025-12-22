package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.io.IOException;

/**
 * Deserializes RandomStringSecret from JSON.
 * Delegates to SecretUtil for proper handling of both hex strings and NUT-10 formats.
 */
public class RandomStringSecretDeserializer extends JsonDeserializer<RandomStringSecret> {
    @Override
    public RandomStringSecret deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node.isTextual()) {
            String value = node.textValue();
            // Use SecretUtil to properly parse the string
            Secret secret = SecretUtil.toSecret(value);
            // If it's already a RandomStringSecret, return it
            if (secret instanceof RandomStringSecret rss) {
                return rss;
            }
            // For other secret types, we shouldn't reach here in normal use
            // but handle gracefully by creating a RandomStringSecret from the string bytes
            return RandomStringSecret.fromBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        throw new RuntimeException("Invalid RandomStringSecret format: expected text node");
    }
}
