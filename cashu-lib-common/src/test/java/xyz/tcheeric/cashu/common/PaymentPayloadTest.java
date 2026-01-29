package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut18.PaymentPayload;
import xyz.tcheeric.cashu.common.nut18.PaymentPayloadProof;
import xyz.tcheeric.cashu.common.nut18.PaymentRequest;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentPayload")
class PaymentPayloadTest {

    private static final ObjectMapper MAPPER = JsonUtils.JSON_MAPPER;
    private static final String SAMPLE_E = "e".repeat(64);
    private static final String SAMPLE_S = "a".repeat(64);
    private static final String SAMPLE_R = "b".repeat(64);

    @Nested
    @DisplayName("PaymentPayloadProof")
    class PaymentPayloadProofTests {

        @Test
        void shouldCreateProofWithAllFields() {
            PaymentPayloadProof proof = PaymentPayloadProof.builder()
                    .amount(64)
                    .keysetId("009a1f293253e41e")
                    .secret("secretvalue123")
                    .signature("02def456abc789")
                    .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                            .e(SAMPLE_E)
                            .s(SAMPLE_S)
                            .r(SAMPLE_R)
                            .build())
                    .witness("witnessdata")
                    .build();

            assertThat(proof.getAmount()).isEqualTo(64);
            assertThat(proof.getKeysetId()).isEqualTo("009a1f293253e41e");
            assertThat(proof.hasDLEQ()).isTrue();
            assertThat(proof.hasDLEQWithBlindingFactor()).isTrue();
        }

        @Test
        void shouldDetectMissingDLEQ() {
            PaymentPayloadProof proof = PaymentPayloadProof.builder()
                    .amount(32)
                    .keysetId("009a1f293253e41e")
                    .secret("secret")
                    .signature("signature")
                    .build();

            assertThat(proof.hasDLEQ()).isFalse();
            assertThat(proof.hasDLEQWithBlindingFactor()).isFalse();
        }

        @Test
        void shouldDetectDLEQWithoutBlindingFactor() {
            PaymentPayloadProof proof = PaymentPayloadProof.builder()
                    .amount(16)
                    .keysetId("009a1f293253e41e")
                    .secret("secret")
                    .signature("signature")
                    .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                            .e(SAMPLE_E)
                            .s(SAMPLE_S)
                            .build())
                    .build();

            assertThat(proof.hasDLEQ()).isTrue();
            assertThat(proof.hasDLEQWithBlindingFactor()).isFalse();
        }

        @Test
        void shouldSerializeWithCorrectJsonKeys() throws Exception {
            PaymentPayloadProof proof = PaymentPayloadProof.builder()
                    .amount(64)
                    .keysetId("009a1f293253e41e")
                    .secret("mysecret")
                    .signature("02abc123")
                    .build();

            String json = MAPPER.writeValueAsString(proof);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("amount").asInt()).isEqualTo(64);
            assertThat(node.get("id").asText()).isEqualTo("009a1f293253e41e");
            assertThat(node.get("secret").asText()).isEqualTo("mysecret");
            assertThat(node.get("C").asText()).isEqualTo("02abc123");
        }

        @Test
        void shouldRoundTripProof() throws Exception {
            PaymentPayloadProof original = PaymentPayloadProof.builder()
                    .amount(128)
                    .keysetId("abcdef1234567890")
                    .secret("testsecret")
                    .signature("02signature")
                    .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                            .e(SAMPLE_E)
                            .s(SAMPLE_S)
                            .r(SAMPLE_R)
                            .build())
                    .witness("mywitness")
                    .build();

            String json = MAPPER.writeValueAsString(original);
            PaymentPayloadProof restored = MAPPER.readValue(json, PaymentPayloadProof.class);

            assertThat(restored.getAmount()).isEqualTo(original.getAmount());
            assertThat(restored.getKeysetId()).isEqualTo(original.getKeysetId());
            assertThat(restored.getSecret()).isEqualTo(original.getSecret());
            assertThat(restored.getSignature()).isEqualTo(original.getSignature());
            assertThat(restored.getWitness()).isEqualTo(original.getWitness());
            assertThat(restored.getDleq().getE()).isEqualTo(SAMPLE_E);
            assertThat(restored.getDleq().getS()).isEqualTo(SAMPLE_S);
            assertThat(restored.getDleq().getR()).isEqualTo(SAMPLE_R);
        }
    }

    @Nested
    @DisplayName("PaymentPayload Creation")
    class PayloadCreationTests {

        @Test
        void shouldCreatePayloadFromRequest() {
            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("pay-123")
                    .amount(100)
                    .unit("sat")
                    .build();

            List<PaymentPayloadProof> proofs = List.of(
                    PaymentPayloadProof.builder()
                            .amount(64)
                            .keysetId("keyset1")
                            .secret("secret1")
                            .signature("sig1")
                            .build(),
                    PaymentPayloadProof.builder()
                            .amount(36)
                            .keysetId("keyset1")
                            .secret("secret2")
                            .signature("sig2")
                            .build()
            );

            PaymentPayload payload = PaymentPayload.fromRequest(request, proofs, "https://mint.example.com", "sat");

            assertThat(payload.getId()).isEqualTo("pay-123");
            assertThat(payload.getMint()).isEqualTo("https://mint.example.com");
            assertThat(payload.getUnit()).isEqualTo("sat");
            assertThat(payload.getProofs()).hasSize(2);
            assertThat(payload.getTotalAmount()).isEqualTo(100);
        }

        @Test
        void shouldCreatePayloadWithMemo() {
            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("pay-456")
                    .build();

            PaymentPayload payload = PaymentPayload.fromRequest(
                    request,
                    List.of(),
                    "https://mint.example.com",
                    "sat",
                    "Thanks for the coffee!"
            );

            assertThat(payload.getMemo()).isEqualTo("Thanks for the coffee!");
        }

        @Test
        void shouldStripTrailingSlashFromMint() {
            PaymentPayload payload = new PaymentPayload();
            payload.setMint("https://mint.example.com///");

            assertThat(payload.getMint()).isEqualTo("https://mint.example.com");
        }

        @Test
        void shouldHandleNullMint() {
            PaymentPayload payload = new PaymentPayload();
            payload.setMint(null);

            assertThat(payload.getMint()).isNull();
        }
    }

    @Nested
    @DisplayName("Proof Management")
    class ProofManagementTests {

        @Test
        void shouldAddProof() {
            PaymentPayload payload = new PaymentPayload();

            payload.addProof(PaymentPayloadProof.builder()
                    .amount(32)
                    .keysetId("keyset1")
                    .secret("secret")
                    .signature("sig")
                    .build());

            assertThat(payload.getProofCount()).isEqualTo(1);
            assertThat(payload.getTotalAmount()).isEqualTo(32);
        }

        @Test
        void shouldCalculateTotalAmount() {
            PaymentPayload payload = PaymentPayload.builder()
                    .proofs(List.of(
                            PaymentPayloadProof.builder().amount(1).keysetId("k").secret("s").signature("c").build(),
                            PaymentPayloadProof.builder().amount(2).keysetId("k").secret("s").signature("c").build(),
                            PaymentPayloadProof.builder().amount(4).keysetId("k").secret("s").signature("c").build(),
                            PaymentPayloadProof.builder().amount(8).keysetId("k").secret("s").signature("c").build()
                    ))
                    .build();

            assertThat(payload.getTotalAmount()).isEqualTo(15);
        }

        @Test
        void shouldReturnZeroForNullProofs() {
            PaymentPayload payload = PaymentPayload.builder()
                    .proofs(null)
                    .build();

            assertThat(payload.getTotalAmount()).isEqualTo(0);
            assertThat(payload.getProofCount()).isEqualTo(0);
        }

        @Test
        void shouldDetectAllProofsHaveDLEQ() {
            PaymentPayload withDLEQ = PaymentPayload.builder()
                    .proofs(List.of(
                            PaymentPayloadProof.builder()
                                    .amount(64).keysetId("k").secret("s").signature("c")
                                    .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                            .e(SAMPLE_E).s(SAMPLE_S).build())
                                    .build(),
                            PaymentPayloadProof.builder()
                                    .amount(32).keysetId("k").secret("s").signature("c")
                                    .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                            .e(SAMPLE_E).s(SAMPLE_S).build())
                                    .build()
                    ))
                    .build();

            PaymentPayload mixedDLEQ = PaymentPayload.builder()
                    .proofs(List.of(
                            PaymentPayloadProof.builder()
                                    .amount(64).keysetId("k").secret("s").signature("c")
                                    .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                            .e(SAMPLE_E).s(SAMPLE_S).build())
                                    .build(),
                            PaymentPayloadProof.builder()
                                    .amount(32).keysetId("k").secret("s").signature("c")
                                    .build()
                    ))
                    .build();

            assertThat(withDLEQ.allProofsHaveDLEQ()).isTrue();
            assertThat(mixedDLEQ.allProofsHaveDLEQ()).isFalse();
        }

        @Test
        void shouldReturnFalseForEmptyProofs() {
            PaymentPayload empty = PaymentPayload.builder().proofs(List.of()).build();
            PaymentPayload nullProofs = PaymentPayload.builder().proofs(null).build();

            assertThat(empty.allProofsHaveDLEQ()).isFalse();
            assertThat(nullProofs.allProofsHaveDLEQ()).isFalse();
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerializationTests {

        @Test
        void shouldSerializeToJson() throws Exception {
            PaymentPayload payload = PaymentPayload.builder()
                    .id("inv-12345")
                    .memo("Payment memo")
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .proofs(List.of(
                            PaymentPayloadProof.builder()
                                    .amount(64)
                                    .keysetId("009a1f293253e41e")
                                    .secret("mysecret")
                                    .signature("02abc")
                                    .build()
                    ))
                    .build();

            String json = payload.toJson();
            JsonNode node = MAPPER.valueToTree(MAPPER.readValue(json, PaymentPayload.class));

            assertThat(node.get("id").asText()).isEqualTo("inv-12345");
            assertThat(node.get("memo").asText()).isEqualTo("Payment memo");
            assertThat(node.get("mint").asText()).isEqualTo("https://mint.example.com");
            assertThat(node.get("unit").asText()).isEqualTo("sat");
            assertThat(node.get("proofs").isArray()).isTrue();
        }

        @Test
        void shouldDeserializeFromJson() {
            String json = """
                {
                  "id": "pay-789",
                  "memo": "Test payment",
                  "mint": "https://mint.test.com",
                  "unit": "sat",
                  "proofs": [
                    {
                      "amount": 32,
                      "id": "keyset123",
                      "secret": "secret123",
                      "C": "02signature"
                    }
                  ]
                }
                """;

            PaymentPayload payload = PaymentPayload.fromJson(json);

            assertThat(payload.getId()).isEqualTo("pay-789");
            assertThat(payload.getMemo()).isEqualTo("Test payment");
            assertThat(payload.getMint()).isEqualTo("https://mint.test.com");
            assertThat(payload.getUnit()).isEqualTo("sat");
            assertThat(payload.getProofs()).hasSize(1);
            assertThat(payload.getProofs().get(0).getAmount()).isEqualTo(32);
        }

        @Test
        void shouldRoundTripThroughJson() {
            PaymentPayload original = PaymentPayload.builder()
                    .id("roundtrip-001")
                    .memo("Round trip test")
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .proofs(List.of(
                            PaymentPayloadProof.builder()
                                    .amount(64)
                                    .keysetId("keyset1")
                                    .secret("secret1")
                                    .signature("sig1")
                                    .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                            .e(SAMPLE_E)
                                            .s(SAMPLE_S)
                                            .r(SAMPLE_R)
                                            .build())
                                    .build()
                    ))
                    .build();

            String json = original.toJson();
            PaymentPayload restored = PaymentPayload.fromJson(json);

            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getMemo()).isEqualTo(original.getMemo());
            assertThat(restored.getMint()).isEqualTo(original.getMint());
            assertThat(restored.getUnit()).isEqualTo(original.getUnit());
            assertThat(restored.getTotalAmount()).isEqualTo(original.getTotalAmount());
            assertThat(restored.getProofs().get(0).getDleq().getE()).isEqualTo(SAMPLE_E);
        }

        @Test
        void shouldOmitNullFields() throws Exception {
            PaymentPayload payload = PaymentPayload.builder()
                    .id("minimal")
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .build();

            String json = payload.toJson();
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.has("memo")).isFalse();
        }
    }
}
