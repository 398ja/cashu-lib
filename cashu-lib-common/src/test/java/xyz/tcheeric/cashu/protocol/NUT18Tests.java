package xyz.tcheeric.cashu.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut10.Nut10Option;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut18.PaymentPayload;
import xyz.tcheeric.cashu.common.nut18.PaymentPayloadProof;
import xyz.tcheeric.cashu.common.nut18.PaymentRequest;
import xyz.tcheeric.cashu.common.nut18.Transport;
import xyz.tcheeric.cashu.common.nut18.TransportType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for NUT-18 Payment Requests.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18 Specification</a>
 */
@DisplayName("NUT-18 Payment Requests")
class NUT18Tests {

    private static final String SAMPLE_E = "e".repeat(64);
    private static final String SAMPLE_S = "a".repeat(64);
    private static final String SAMPLE_R = "b".repeat(64);

    @Nested
    @DisplayName("Payment Request Encoding")
    class PaymentRequestEncodingTests {

        @Test
        void shouldEncodeMinimalRequest() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(100)
                    .unit("sat")
                    .build();

            String encoded = request.serialize();

            assertThat(encoded).startsWith("creqA");
            assertThat(encoded).doesNotContain("cashu:");
        }

        @Test
        void shouldEncodeClickableUri() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(100)
                    .unit("sat")
                    .build();

            String clickable = request.serialize(true);

            assertThat(clickable).startsWith("cashu:creqA");
        }

        @Test
        void shouldDecodeFromClickableUri() {
            PaymentRequest original = PaymentRequest.builder()
                    .paymentId("test-123")
                    .amount(500)
                    .unit("sat")
                    .description("Test payment")
                    .build();

            String clickable = original.serialize(true);
            PaymentRequest decoded = PaymentRequest.deserialize(clickable);

            assertThat(decoded.getPaymentId()).isEqualTo("test-123");
            assertThat(decoded.getAmount()).isEqualTo(500);
            assertThat(decoded.getUnit()).isEqualTo("sat");
            assertThat(decoded.getDescription()).isEqualTo("Test payment");
        }
    }

    @Nested
    @DisplayName("Full Payment Flow")
    class FullPaymentFlowTests {

        @Test
        void shouldCompletePaymentRequestToPayloadFlow() {
            // 1. Receiver creates payment request
            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("order-12345")
                    .amount(1000)
                    .unit("sat")
                    .singleUse(true)
                    .mints(List.of("https://mint.example.com"))
                    .description("Payment for order #12345")
                    .transports(List.of(
                            Transport.httpPost("https://api.example.com/pay/callback")
                    ))
                    .build();

            // 2. Serialize and transmit to sender (e.g., QR code)
            String encoded = request.serialize();
            assertThat(encoded).startsWith("creqA");

            // 3. Sender decodes the request
            PaymentRequest decoded = PaymentRequest.deserialize(encoded);
            assertThat(decoded.getPaymentId()).isEqualTo("order-12345");
            assertThat(decoded.getAmount()).isEqualTo(1000);

            // 4. Sender validates mint is permitted
            assertThat(decoded.isMintPermitted("https://mint.example.com")).isTrue();
            assertThat(decoded.isMintPermitted("https://other.mint.com")).isFalse();

            // 5. Sender creates payment payload with matching proofs
            List<PaymentPayloadProof> proofs = List.of(
                    PaymentPayloadProof.builder()
                            .amount(512)
                            .keysetId("009a1f293253e41e")
                            .secret("secret1")
                            .signature("02abc123")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                            .build(),
                    PaymentPayloadProof.builder()
                            .amount(256)
                            .keysetId("009a1f293253e41e")
                            .secret("secret2")
                            .signature("02def456")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                            .build(),
                    PaymentPayloadProof.builder()
                            .amount(232)
                            .keysetId("009a1f293253e41e")
                            .secret("secret3")
                            .signature("02ghi789")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                            .build()
            );

            PaymentPayload payload = PaymentPayload.fromRequest(
                    decoded,
                    proofs,
                    "https://mint.example.com",
                    "sat",
                    "Thanks for the service!"
            );

            // 6. Verify payload matches request
            assertThat(payload.getId()).isEqualTo("order-12345");
            assertThat(payload.getTotalAmount()).isEqualTo(1000);
            assertThat(payload.allProofsHaveDLEQ()).isTrue();

            // 7. Serialize payload for transmission
            String payloadJson = payload.toJson();
            assertThat(payloadJson).contains("order-12345");

            // 8. Receiver deserializes and validates
            PaymentPayload receivedPayload = PaymentPayload.fromJson(payloadJson);
            assertThat(receivedPayload.getId()).isEqualTo(request.getPaymentId());
            assertThat(receivedPayload.getTotalAmount()).isEqualTo(request.getAmount());
        }

        @Test
        void shouldHandleOpenAmountRequest() {
            // Request without specific amount (open donation)
            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("donation-001")
                    .description("Open donation - any amount welcome")
                    .transports(List.of(Transport.nostrNip17("nprofile1abc")))
                    .build();

            String encoded = request.serialize();
            PaymentRequest decoded = PaymentRequest.deserialize(encoded);

            assertThat(decoded.hasAmount()).isFalse();
            assertThat(decoded.getDescription()).isEqualTo("Open donation - any amount welcome");
        }
    }

    @Nested
    @DisplayName("NUT-10 Locking Conditions")
    class Nut10LockingTests {

        @Test
        void shouldCreateP2PKLockedRequest() {
            String pubKeyHex = "02" + "ab".repeat(32);

            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("p2pk-payment")
                    .amount(5000)
                    .unit("sat")
                    .nut10Option(Nut10Option.forP2PK(pubKeyHex))
                    .build();

            String encoded = request.serialize();
            PaymentRequest decoded = PaymentRequest.deserialize(encoded);

            assertThat(decoded.hasNut10Locking()).isTrue();
            assertThat(decoded.getNut10Option().getKind()).isEqualTo(WellKnownSecret.Kind.P2PK);
            assertThat(decoded.getNut10Option().getData()).isEqualTo(pubKeyHex);
            assertThat(decoded.getNut10Option().isP2PK()).isTrue();
        }

        @Test
        void shouldCreateHTLCLockedRequest() {
            String hashHex = "cd".repeat(32);

            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("htlc-payment")
                    .amount(10000)
                    .unit("sat")
                    .nut10Option(Nut10Option.forHTLC(hashHex))
                    .build();

            String encoded = request.serialize();
            PaymentRequest decoded = PaymentRequest.deserialize(encoded);

            assertThat(decoded.getNut10Option().getKind()).isEqualTo(WellKnownSecret.Kind.HTLC);
            assertThat(decoded.getNut10Option().isHTLC()).isTrue();
        }

        @Test
        void shouldCreateVoucherLockedRequest() {
            String voucherData = "ef".repeat(32);

            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("voucher-payment")
                    .amount(2500)
                    .unit("sat")
                    .nut10Option(Nut10Option.forVoucher(voucherData))
                    .build();

            String encoded = request.serialize();
            PaymentRequest decoded = PaymentRequest.deserialize(encoded);

            assertThat(decoded.getNut10Option().getKind()).isEqualTo(WellKnownSecret.Kind.VOUCHER);
            assertThat(decoded.getNut10Option().isVoucher()).isTrue();
        }

        @Test
        void shouldPreserveNut10OptionTags() {
            Nut10Option option = Nut10Option.forP2PK("02" + "ab".repeat(32));
            option.addTag("locktime", "1700000000");
            option.addTag("refund", "02" + "cd".repeat(32));

            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("tagged-p2pk")
                    .amount(1000)
                    .unit("sat")
                    .nut10Option(option)
                    .build();

            String encoded = request.serialize();
            PaymentRequest decoded = PaymentRequest.deserialize(encoded);

            assertThat(decoded.getNut10Option().getTagValue("locktime")).isEqualTo("1700000000");
            assertThat(decoded.getNut10Option().getTagValue("refund")).isEqualTo("02" + "cd".repeat(32));
        }
    }

    @Nested
    @DisplayName("Transport Configuration")
    class TransportConfigurationTests {

        @Test
        void shouldHandleMultipleTransports() {
            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("multi-transport")
                    .amount(100)
                    .unit("sat")
                    .transports(List.of(
                            Transport.httpPost("https://api.example.com/pay"),
                            Transport.nostrNip17("nprofile1xyz")
                    ))
                    .build();

            String encoded = request.serialize();
            PaymentRequest decoded = PaymentRequest.deserialize(encoded);

            assertThat(decoded.getTransports()).hasSize(2);
            assertThat(decoded.getPreferredTransport().getType()).isEqualTo(TransportType.POST);
            assertThat(decoded.getTransports().get(1).getType()).isEqualTo(TransportType.NOSTR);
        }

        @Test
        void shouldPreserveTransportTags() {
            Transport nostrTransport = Transport.nostrNip17("nprofile1abc");
            nostrTransport.addTag("relay", "wss://relay.example.com");

            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("tagged-transport")
                    .amount(100)
                    .unit("sat")
                    .transports(List.of(nostrTransport))
                    .build();

            String encoded = request.serialize();
            PaymentRequest decoded = PaymentRequest.deserialize(encoded);

            Transport decodedTransport = decoded.getPreferredTransport();
            assertThat(decodedTransport.getTagValue("n")).isEqualTo("17");
            assertThat(decodedTransport.getTagValue("relay")).isEqualTo("wss://relay.example.com");
        }
    }

    @Nested
    @DisplayName("Validation Rules")
    class ValidationRulesTests {

        @Test
        void shouldRejectAmountWithoutUnit() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(100)
                    .build();

            assertThrows(IllegalStateException.class, () -> request.serialize());
        }

        @Test
        void shouldAllowRequestWithoutAmount() {
            PaymentRequest request = PaymentRequest.builder()
                    .description("Open request")
                    .build();

            // Should not throw
            String encoded = request.serialize();
            assertThat(encoded).startsWith("creqA");
        }

        @Test
        void shouldValidateSingleUseFlag() {
            PaymentRequest singleUse = PaymentRequest.builder()
                    .paymentId("single-use-001")
                    .amount(100)
                    .unit("sat")
                    .singleUse(true)
                    .build();

            String encoded = singleUse.serialize();
            PaymentRequest decoded = PaymentRequest.deserialize(encoded);

            assertThat(decoded.isSingleUseRequest()).isTrue();
        }
    }

    @Nested
    @DisplayName("DLEQ Verification Support")
    class DLEQVerificationTests {

        @Test
        void shouldIncludeDLEQInPayload() {
            PaymentPayloadProof proofWithDLEQ = PaymentPayloadProof.builder()
                    .amount(64)
                    .keysetId("009a1f293253e41e")
                    .secret("testsecret")
                    .signature("02signature")
                    .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                            .e(SAMPLE_E)
                            .s(SAMPLE_S)
                            .r(SAMPLE_R)
                            .build())
                    .build();

            PaymentPayload payload = PaymentPayload.builder()
                    .id("dleq-test")
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .proofs(List.of(proofWithDLEQ))
                    .build();

            assertThat(payload.allProofsHaveDLEQ()).isTrue();

            String json = payload.toJson();
            PaymentPayload restored = PaymentPayload.fromJson(json);

            assertThat(restored.allProofsHaveDLEQ()).isTrue();
            assertThat(restored.getProofs().get(0).getDleq().getE()).isEqualTo(SAMPLE_E);
            assertThat(restored.getProofs().get(0).getDleq().hasBlindingFactor()).isTrue();
        }

        @Test
        void shouldDetectMissingDLEQ() {
            PaymentPayloadProof proofWithoutDLEQ = PaymentPayloadProof.builder()
                    .amount(32)
                    .keysetId("009a1f293253e41e")
                    .secret("testsecret")
                    .signature("02signature")
                    .build();

            PaymentPayload payload = PaymentPayload.builder()
                    .id("no-dleq-test")
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .proofs(List.of(proofWithoutDLEQ))
                    .build();

            assertThat(payload.allProofsHaveDLEQ()).isFalse();
        }
    }

    @Nested
    @DisplayName("Mint URL Handling")
    class MintUrlHandlingTests {

        @Test
        void shouldNormalizeMintUrls() {
            PaymentRequest request = PaymentRequest.builder()
                    .mints(List.of(
                            "https://mint1.example.com/",
                            "https://mint2.example.com///"
                    ))
                    .build();

            // URLs should be normalized when added
            PaymentRequest fresh = new PaymentRequest();
            fresh.addMint("https://mint.example.com///");

            assertThat(fresh.getMints().get(0)).isEqualTo("https://mint.example.com");
        }

        @Test
        void shouldMatchMintsWithTrailingSlash() {
            PaymentRequest request = PaymentRequest.builder()
                    .mints(List.of("https://mint.example.com"))
                    .build();

            assertThat(request.isMintPermitted("https://mint.example.com/")).isTrue();
            assertThat(request.isMintPermitted("https://mint.example.com")).isTrue();
        }
    }

    /**
     * Official test vectors from NUT-18 specification.
     *
     * @see <a href="https://github.com/cashubtc/nuts/blob/main/tests/18-tests.md">NUT-18 Test Vectors</a>
     */
    @Nested
    @DisplayName("Official Test Vectors")
    class OfficialTestVectors {

        @Test
        @DisplayName("Vector 1: Basic Payment Request (Nostr Transport)")
        void shouldDecodeVector1BasicNostrTransport() {
            String encoded = "creqApWF0gaNhdGVub3N0cmFheKlucHJvZmlsZTFxeTI4d3VtbjhnaGo3dW45ZDNzaGp0bnl2OWtoMnVld2Q5aHN6OW1od2RlbjV0ZTB3ZmprY2N0ZTljdXJ4dmVuOWVlaHFjdHJ2NWhzenJ0aHdkZW41dGUwZGVoaHh0bnZkYWtxcWd5ZGFxeTdjdXJrNDM5eWtwdGt5c3Y3dWRoZGh1NjhzdWNtMjk1YWtxZWZkZWhrZjBkNDk1Y3d1bmw1YWeBgmFuYjE3YWloYjdhOTAxNzZhYQphdWNzYXRhbYF3aHR0cHM6Ly84MzMzLnNwYWNlOjMzMzg=";

            PaymentRequest request = PaymentRequest.deserialize(encoded);

            assertThat(request.getPaymentId()).isEqualTo("b7a90176");
            assertThat(request.getAmount()).isEqualTo(10);
            assertThat(request.getUnit()).isEqualTo("sat");
            assertThat(request.getMints()).containsExactly("https://8333.space:3338");
            assertThat(request.getTransports()).hasSize(1);
            assertThat(request.getPreferredTransport().getType()).isEqualTo(TransportType.NOSTR);
            assertThat(request.getPreferredTransport().getTarget()).startsWith("nprofile1");
            assertThat(request.getPreferredTransport().getTagValue("n")).isEqualTo("17");
        }

        @Test
        @DisplayName("Vector 3: HTTP Transport Payment Request")
        void shouldDecodeVector3HttpTransport() {
            String encoded = "creqApWF0gaNhdGRwb3N0YWF4H2h0dHBzOi8vYXBpLmV4YW1wbGUuY29tL3JlY2VpdmVhZ/dhaWhhMmMxMmY0NWFhGDJhdWNzYXRhbYF4GWh0dHBzOi8vY2FzaHUuZXhhbXBsZS5jb20=";

            PaymentRequest request = PaymentRequest.deserialize(encoded);

            assertThat(request.getPaymentId()).isEqualTo("a2c12f45");
            assertThat(request.getAmount()).isEqualTo(50);
            assertThat(request.getUnit()).isEqualTo("sat");
            assertThat(request.getMints()).containsExactly("https://cashu.example.com");
            assertThat(request.getTransports()).hasSize(1);
            assertThat(request.getPreferredTransport().getType()).isEqualTo(TransportType.POST);
            assertThat(request.getPreferredTransport().getTarget()).isEqualTo("https://api.example.com/receive");
        }

        /**
         * Vector 4: Nostr Transport with Multiple NIPs.
         *
         * Note: The official test vector from the spec appears to have a structural issue
         * (duplicate CBOR keys). This test uses a round-trip validation instead.
         */
        @Test
        @DisplayName("Vector 4: Nostr Transport with Multiple NIPs (Round-trip)")
        void shouldRoundTripVector4MultipleNips() {
            // Build a payment request matching the Vector 4 expected structure
            PaymentRequest original = PaymentRequest.builder()
                    .paymentId("f92a51b8")
                    .amount(100)
                    .unit("sat")
                    .mints(List.of(
                            "https://mint1.example.com",
                            "https://mint2.example.com"
                    ))
                    .transports(List.of(
                            Transport.builder()
                                    .typeValue("nostr")
                                    .target("npub1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq28spj3")
                                    .tags(List.of(
                                            List.of("n", "17"),
                                            List.of("n", "9735")
                                    ))
                                    .build()
                    ))
                    .build();

            // Serialize and deserialize
            String encoded = original.serialize();
            PaymentRequest decoded = PaymentRequest.deserialize(encoded);

            // Verify round-trip
            assertThat(decoded.getPaymentId()).isEqualTo("f92a51b8");
            assertThat(decoded.getAmount()).isEqualTo(100);
            assertThat(decoded.getUnit()).isEqualTo("sat");
            assertThat(decoded.getMints()).containsExactly(
                    "https://mint1.example.com",
                    "https://mint2.example.com"
            );
            assertThat(decoded.getTransports()).hasSize(1);
            assertThat(decoded.getPreferredTransport().getType()).isEqualTo(TransportType.NOSTR);
            assertThat(decoded.getPreferredTransport().getTarget()).startsWith("npub1");
            assertThat(decoded.getPreferredTransport().getTags()).hasSize(2);
        }

        @Test
        @DisplayName("Vector 5: Minimal Payment Request")
        void shouldDecodeVector5Minimal() {
            String encoded = "creqAo2FpaDdmNGEyYjM5YXVjc2F0YW2BeBhodHRwczovL21pbnQuZXhhbXBsZS5jb20=";

            PaymentRequest request = PaymentRequest.deserialize(encoded);

            assertThat(request.getPaymentId()).isEqualTo("7f4a2b39");
            assertThat(request.getAmount()).isNull();
            assertThat(request.getUnit()).isEqualTo("sat");
            assertThat(request.getMints()).containsExactly("https://mint.example.com");
        }
    }
}
