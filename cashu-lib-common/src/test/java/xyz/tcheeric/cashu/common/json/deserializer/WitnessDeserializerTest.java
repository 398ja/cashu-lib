package xyz.tcheeric.cashu.common.json.deserializer;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The NUT-11 witness wire forms a proof can arrive in.
 */
class WitnessDeserializerTest {

    private static final String SIGNATURE =
            "60f3c9b766770b46caac1d27e1ae6b77c8866ebaeba0b9489fe6a15a837eaa6f"
                    + "cd6eaa825499c72ac342983983fd3ba3a8a41f56677cc99ffd73da68b59e1383";

    /**
     * Ensures the JSON-encoded string witness other implementations publish is read.
     */
    @Test
    void shouldReadSignaturesWhenWitnessIsAJsonEncodedString() throws Exception {
        // Arrange
        String json = "{\"amount\":1,\"secret\":\"s\",\"id\":\"009a1f293253e41e\",\"C\":\"c\","
                + "\"witness\":\"{\\\"signatures\\\":[\\\"" + SIGNATURE + "\\\"]}\"}";

        // Act
        Proof<Secret> proof = readProof(json);

        // Assert
        assertThat(proof.getWitness().getSignatures()).containsExactly(SIGNATURE);
    }

    /**
     * Ensures a witness written as a nested object, as this library emits it, still parses.
     */
    @Test
    void shouldReadSignaturesWhenWitnessIsANestedObject() throws Exception {
        // Arrange
        String json = "{\"amount\":1,\"secret\":\"s\",\"id\":\"009a1f293253e41e\",\"C\":\"c\","
                + "\"witness\":{\"signatures\":[\"" + SIGNATURE + "\"]}}";

        // Act
        Proof<Secret> proof = readProof(json);

        // Assert
        assertThat(proof.getWitness().getSignatures()).containsExactly(SIGNATURE);
    }

    /**
     * Ensures an absent witness stays absent rather than becoming an empty one.
     */
    @Test
    void shouldLeaveWitnessNullWhenItIsAnEmptyString() throws Exception {
        // Arrange
        String json = "{\"amount\":1,\"secret\":\"s\",\"id\":\"009a1f293253e41e\",\"C\":\"c\","
                + "\"witness\":\"\"}";

        // Act
        Proof<Secret> proof = readProof(json);

        // Assert
        assertThat(proof.getWitness()).isNull();
    }

    @SuppressWarnings("unchecked")
    private static Proof<Secret> readProof(String json) throws Exception {
        return JsonUtils.JSON_MAPPER.readValue(json, Proof.class);
    }
}
