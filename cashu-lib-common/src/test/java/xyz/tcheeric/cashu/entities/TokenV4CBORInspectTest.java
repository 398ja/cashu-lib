package xyz.tcheeric.cashu.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TokenV4CBORInspectTest {

    @Test
    /**
     * Ensures CBOR serialization retains the unit field when processed manually and via Jackson.
     */
    void shouldPreserveUnitDuringManualAndJacksonCborSerialization() throws Exception {
        // Arrange
        TokenV4 token = new TokenV4();
        token.setMintUrl("https://mint");
        token.setUnit("sat");

        TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
        proof.setAmount(1);
        proof.setSecret("test-secret");
        proof.setSignature(Utils.hexStringToBytes("0244538319de485d55bed3b29a642bee5879375ab9e7a620e11e48ba482421f3cf"));

        TokenV4.TokenData tokenData = new TokenV4.TokenData(
                Utils.hexStringToBytes("00ffd48b8f5ecf80"),
                new ArrayList<>(List.of(proof))
        );
        token.setTokenDataList(new ArrayList<>(List.of(tokenData)));

        // Act
        String serialized = token.serialize(false);
        String tokenWithoutPrefix = serialized.substring("cashuB".length());
        byte[] manualCborBytes = Base64.getUrlDecoder().decode(tokenWithoutPrefix);

        ObjectMapper mapper = JsonUtils.CBOR_MAPPER;
        TokenV4 manualDeserialized = mapper.readValue(manualCborBytes, TokenV4.class);
        byte[] jacksonBytes = mapper.writeValueAsBytes(token);
        TokenV4 jacksonDeserialized = mapper.readValue(jacksonBytes, TokenV4.class);

        // Assert
        assertNotNull(manualDeserialized.getUnit());
        assertEquals("sat", manualDeserialized.getUnit());
        assertNotNull(jacksonDeserialized.getUnit());
        assertEquals("sat", jacksonDeserialized.getUnit());
    }
}
