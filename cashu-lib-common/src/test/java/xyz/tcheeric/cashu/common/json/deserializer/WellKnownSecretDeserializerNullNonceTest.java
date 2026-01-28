package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut18.VoucherWellKnownSecret;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that WellKnownSecretDeserializer correctly handles null nonce in NUT-10 format.
 *
 * This is a regression test for the bug where JsonNode.asText() was called on a null node,
 * returning the string "null" instead of Java null.
 */
class WellKnownSecretDeserializerNullNonceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldPreserveNullNonceInNut10Format() throws Exception {
        // Given: JSON with null nonce
        String json = "[\"VOUCHER\",\"746573742d64617461\",null,[]]";

        // When: Deserialize and re-serialize
        WellKnownSecret secret = mapper.readValue(json, WellKnownSecret.class);
        String output = secret.toString();

        // Then: Nonce should be Java null, not the string "null"
        assertThat(secret.getNonce())
                .withFailMessage("Nonce should be Java null, not the string 'null'")
                .isNull();

        // And: Re-serialized JSON should contain JSON null, not string "null"
        assertThat(output)
                .withFailMessage("Output should contain JSON null: %s", output)
                .contains(",null,");
        assertThat(output)
                .withFailMessage("Output should NOT contain string 'null': %s", output)
                .doesNotContain(",\"null\",");
    }

    @Test
    void shouldPreserveStringNonceInNut10Format() throws Exception {
        // Given: JSON with string nonce
        String json = "[\"VOUCHER\",\"746573742d64617461\",\"my-nonce-123\",[]]";

        // When: Deserialize and re-serialize
        WellKnownSecret secret = mapper.readValue(json, WellKnownSecret.class);
        String output = secret.toString();

        // Then: Nonce should be preserved
        assertThat(secret.getNonce()).isEqualTo("my-nonce-123");
        assertThat(output).contains("\"my-nonce-123\"");
    }

    @Test
    void shouldHandleNullNonceInLegacyFormat() throws Exception {
        // Given: Legacy format with null nonce
        String json = "[\"VOUCHER\",{\"data\":\"746573742d64617461\",\"nonce\":null}]";

        // When: Deserialize
        WellKnownSecret secret = mapper.readValue(json, WellKnownSecret.class);

        // Then: Nonce should be Java null
        assertThat(secret.getNonce())
                .withFailMessage("Nonce should be Java null in legacy format")
                .isNull();
    }
}
