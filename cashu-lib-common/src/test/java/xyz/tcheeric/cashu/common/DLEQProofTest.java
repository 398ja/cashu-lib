package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DLEQProofTest {

    private static final ObjectMapper MAPPER = JsonUtils.JSON_MAPPER;
    private static final String SAMPLE_E = "e".repeat(64);
    private static final String SAMPLE_S = "a".repeat(64);
    private static final String SAMPLE_R = "b".repeat(64);
    private static final String SAMPLE_SIGNATURE = "02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea";

    @Nested
    @DisplayName("DLEQProof")
    class ProofOnly {

        @Test
        void shouldSerializeBlindSignatureDLEQWithoutR() throws Exception {
            DLEQProof proof = DLEQProof.forBlindSignature(SAMPLE_E, SAMPLE_S);

            String json = MAPPER.writeValueAsString(proof);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("e").asText()).isEqualTo(SAMPLE_E);
            assertThat(node.get("s").asText()).isEqualTo(SAMPLE_S);
            assertThat(node.get("r")).isNull();
            assertThat(proof.hasBlindingFactor()).isFalse();
        }

        @Test
        void shouldSerializeProofDLEQWithR() throws Exception {
            DLEQProof proof = DLEQProof.forProof(SAMPLE_E, SAMPLE_S, SAMPLE_R);

            String json = MAPPER.writeValueAsString(proof);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("e").asText()).isEqualTo(SAMPLE_E);
            assertThat(node.get("s").asText()).isEqualTo(SAMPLE_S);
            assertThat(node.get("r").asText()).isEqualTo(SAMPLE_R);
            assertThat(proof.hasBlindingFactor()).isTrue();
        }

        @Test
        void shouldDeserializeDLEQWithoutR() throws Exception {
            String json = "{\"e\":\"" + SAMPLE_E + "\",\"s\":\"" + SAMPLE_S + "\"}";

            DLEQProof proof = MAPPER.readValue(json, DLEQProof.class);

            assertThat(proof.getE()).isEqualTo(SAMPLE_E);
            assertThat(proof.getS()).isEqualTo(SAMPLE_S);
            assertThat(proof.getR()).isNull();
            assertThat(proof.hasBlindingFactor()).isFalse();
        }

        @Test
        void shouldDeserializeDLEQWithR() throws Exception {
            String json = "{\"e\":\"" + SAMPLE_E + "\",\"s\":\"" + SAMPLE_S + "\",\"r\":\"" + SAMPLE_R + "\"}";

            DLEQProof proof = MAPPER.readValue(json, DLEQProof.class);

            assertThat(proof.getE()).isEqualTo(SAMPLE_E);
            assertThat(proof.getS()).isEqualTo(SAMPLE_S);
            assertThat(proof.getR()).isEqualTo(SAMPLE_R);
            assertThat(proof.hasBlindingFactor()).isTrue();
        }

        @Test
        void shouldRejectMissingScalars() {
            String json = "{\"e\":\"" + SAMPLE_E + "\"}";

            assertThrows(IllegalArgumentException.class, () -> MAPPER.readValue(json, DLEQProof.class));
        }
    }

    @Nested
    @DisplayName("BlindSignature/Proof with DLEQ")
    class ContainerSerialisation {

        @Test
        void shouldRoundTripBlindSignatureWithDLEQ() throws Exception {
            BlindSignature blindSignature = BlindSignature.builder()
                    .amount(2)
                    .keySetId(KeysetId.fromString("0123456789abcdef"))
                    .blindedSignature(Signature.fromString(SAMPLE_SIGNATURE))
                    .dleq(DLEQProof.forBlindSignature(SAMPLE_E, SAMPLE_S))
                    .build();

            String json = MAPPER.writeValueAsString(blindSignature);
            BlindSignature restored = MAPPER.readValue(json, BlindSignature.class);

            assertThat(restored.hasDLEQProof()).isTrue();
            assertThat(restored.getDleq().getR()).isNull();
            assertThat(restored.getDleq().getE()).isEqualTo(SAMPLE_E);
            assertThat(restored.getDleq().getS()).isEqualTo(SAMPLE_S);
        }

        @Test
        void shouldRoundTripProofWithDLEQ() throws Exception {
            RandomStringSecret secret = RandomStringSecret.fromString("c".repeat(64));
            Proof<RandomStringSecret> proof = Proof.<RandomStringSecret>builder()
                    .amount(4)
                    .keySetId("0123456789abcdef")
                    .secret(secret)
                    .unblindedSignature(Signature.fromString(SAMPLE_SIGNATURE))
                    .dleq(DLEQProof.forProof(SAMPLE_E, SAMPLE_S, SAMPLE_R))
                    .build();

            String json = MAPPER.writeValueAsString(proof);
            Proof<?> restored = MAPPER.readValue(json, Proof.class);

            assertThat(restored.hasDLEQProof()).isTrue();
            assertThat(restored.hasDLEQWithBlindingFactor()).isTrue();
            assertThat(restored.getDleq().getR()).isEqualTo(SAMPLE_R);
            assertThat(restored.getDleq().getE()).isEqualTo(SAMPLE_E);
            assertThat(restored.getDleq().getS()).isEqualTo(SAMPLE_S);
        }
    }
}
