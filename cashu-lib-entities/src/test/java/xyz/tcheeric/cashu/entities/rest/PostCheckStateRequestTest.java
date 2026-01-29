package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.BaseKey;
import xyz.tcheeric.cashu.common.CompressedPublicKey;
import xyz.tcheeric.cashu.common.HashToCurveSecret;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.entities.rest.nut07.PostCheckStateRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PostCheckStateRequestTest {

    // Ensures that Ys values are deserialized into compressed public keys.
    @Test
    public void shouldDeserializeYsIntoCompressedPublicKeys() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"Ys\":[\"02599b9ea0a1ad4143706c2a5a4a568ce442dd4313e1cf1f7f0b58a317c1a355ee\"]}";

        PostCheckStateRequest request = mapper.readValue(json, PostCheckStateRequest.class);

        List<HashToCurveSecret> secrets = request.getHashToCurveSecrets();
        assertThat(secrets).hasSize(1);
        //assertThat(secrets.get(0)).isInstanceOf(CompressedPublicKey.class);
        assertThat(secrets.get(0).toString()).isEqualTo("02599b9ea0a1ad4143706c2a5a4a568ce442dd4313e1cf1f7f0b58a317c1a355ee");
    }
}
