package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.Signature;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PostRestoreEntitiesTest {

    // Ensures PostRestoreRequest serializes and deserializes outputs correctly.
    @Test
    public void requestSerializationRoundTrip() throws Exception {
        ObjectMapper mapper = JsonUtils.JSON_MAPPER;
        BlindedMessage output = BlindedMessage.builder()
                .amount(2)
                .keySetId(KeysetId.fromString("009a1f293253e41e"))
                .blindedMessage(PublicKey.fromString("02fdfd6796bfeac490cbee12f778f867f0a2c68f6508d17c649759ea0dc3547528"))
                .build();
        PostRestoreRequest request = new PostRestoreRequest(List.of(output));
        String json = mapper.writeValueAsString(request);
        PostRestoreRequest parsed = mapper.readValue(json, PostRestoreRequest.class);
        assertThat(parsed).isEqualTo(request);
    }

    // Verifies PostRestoreResponse carries matching outputs and signatures after JSON round-trip.
    @Test
    public void responseSerializationRoundTrip() throws Exception {
        ObjectMapper mapper = JsonUtils.JSON_MAPPER;
        BlindedMessage output = BlindedMessage.builder()
                .amount(2)
                .keySetId(KeysetId.fromString("009a1f293253e41e"))
                .blindedMessage(PublicKey.fromString("02fdfd6796bfeac490cbee12f778f867f0a2c68f6508d17c649759ea0dc3547528"))
                .build();
        BlindSignature signature = BlindSignature.builder()
                .amount(2)
                .keySetId(KeysetId.fromString("009a1f293253e41e"))
                .blindedSignature(Signature.fromString("02fdfd6796bfeac490cbee12f778f867f0a2c68f6508d17c649759ea0dc3547528"))
                .build();
        PostRestoreResponse response = new PostRestoreResponse(List.of(output), List.of(signature));
        String json = mapper.writeValueAsString(response);
        PostRestoreResponse parsed = mapper.readValue(json, PostRestoreResponse.class);
        assertThat(parsed).isEqualTo(response);
    }
}
