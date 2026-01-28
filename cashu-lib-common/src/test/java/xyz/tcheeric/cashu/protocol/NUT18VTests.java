package xyz.tcheeric.cashu.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut10.Nut10Option;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut18.PaymentPayloadProof;
import xyz.tcheeric.cashu.common.nut18.VoucherPaymentPayload;
import xyz.tcheeric.cashu.common.nut18.VoucherPaymentRequest;
import xyz.tcheeric.cashu.common.nut18.VoucherTransport;
import xyz.tcheeric.cashu.common.nut18.VoucherTransportType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for NUT-18V Voucher Payment Requests.
 *
 * <p>NUT-18V extends NUT-18 for Model B gift card vouchers with:
 * <ul>
 *     <li>{@code vreqA} prefix (instead of {@code creqA})</li>
 *     <li>Required {@code issuerId} field</li>
 *     <li>MERCHANT transport type</li>
 *     <li>Offline verification support</li>
 * </ul>
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18 Specification</a>
 */
@DisplayName("NUT-18V Voucher Payment Requests")
class NUT18VTests {

    private static final String SAMPLE_E = "e".repeat(64);
    private static final String SAMPLE_S = "a".repeat(64);
    private static final String SAMPLE_R = "b".repeat(64);
    private static final String SAMPLE_ISSUER_ID = "issuer-abc-123";

    @Nested
    @DisplayName("Voucher Payment Request Encoding")
    class VoucherPaymentRequestEncodingTests {

        @Test
        void shouldEncodeMinimalVoucherRequest() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(100)
                    .unit("sat")
                    .build();

            String encoded = request.serialize();

            assertThat(encoded).startsWith("vreqA");
            assertThat(encoded).doesNotContain("cashu:");
            assertThat(encoded).doesNotStartWith("creqA");
        }

        @Test
        void shouldEncodeClickableUri() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(100)
                    .unit("sat")
                    .build();

            String clickable = request.serialize(true);

            assertThat(clickable).startsWith("cashu:vreqA");
        }

        @Test
        void shouldDecodeFromClickableUri() {
            VoucherPaymentRequest original = VoucherPaymentRequest.builder()
                    .paymentId("voucher-123")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(500)
                    .unit("sat")
                    .description("Gift card redemption")
                    .build();

            String clickable = original.serialize(true);
            VoucherPaymentRequest decoded = VoucherPaymentRequest.deserialize(clickable);

            assertThat(decoded.getPaymentId()).isEqualTo("voucher-123");
            assertThat(decoded.getIssuerId()).isEqualTo(SAMPLE_ISSUER_ID);
            assertThat(decoded.getAmount()).isEqualTo(500);
            assertThat(decoded.getUnit()).isEqualTo("sat");
            assertThat(decoded.getDescription()).isEqualTo("Gift card redemption");
        }

        @Test
        void shouldRequireIssuerIdForSerialization() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .amount(100)
                    .unit("sat")
                    .build();

            assertThrows(IllegalStateException.class, request::serialize);
        }
    }

    @Nested
    @DisplayName("Full Voucher Payment Flow")
    class FullVoucherPaymentFlowTests {

        @Test
        void shouldCompleteVoucherPaymentRequestToPayloadFlow() {
            // 1. Issuer/Receiver creates voucher payment request
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .paymentId("voucher-order-12345")
                    .issuerId("gift-card-issuer-xyz")
                    .amount(5000)
                    .unit("sat")
                    .singleUse(true)
                    .offlineVerification(true)
                    .mints(List.of("https://mint.example.com"))
                    .description("$50 Gift Card Redemption")
                    .transports(List.of(
                            VoucherTransport.merchant("https://merchant.example.com/redeem", "merchant-456")
                    ))
                    .build();

            // 2. Serialize and transmit to sender (e.g., QR code at POS)
            String encoded = request.serialize();
            assertThat(encoded).startsWith("vreqA");

            // 3. Sender decodes the request
            VoucherPaymentRequest decoded = VoucherPaymentRequest.deserialize(encoded);
            assertThat(decoded.getPaymentId()).isEqualTo("voucher-order-12345");
            assertThat(decoded.getIssuerId()).isEqualTo("gift-card-issuer-xyz");
            assertThat(decoded.getAmount()).isEqualTo(5000);
            assertThat(decoded.requiresOfflineVerification()).isTrue();

            // 4. Sender validates mint is permitted
            assertThat(decoded.isMintPermitted("https://mint.example.com")).isTrue();
            assertThat(decoded.isMintPermitted("https://other.mint.com")).isFalse();

            // 5. Sender creates voucher payment payload with matching proofs
            List<PaymentPayloadProof> proofs = List.of(
                    PaymentPayloadProof.builder()
                            .amount(4096)
                            .keysetId("009a1f293253e41e")
                            .secret("voucher-secret1")
                            .signature("02abc123")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                            .build(),
                    PaymentPayloadProof.builder()
                            .amount(512)
                            .keysetId("009a1f293253e41e")
                            .secret("voucher-secret2")
                            .signature("02def456")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                            .build(),
                    PaymentPayloadProof.builder()
                            .amount(256)
                            .keysetId("009a1f293253e41e")
                            .secret("voucher-secret3")
                            .signature("02ghi789")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                            .build(),
                    PaymentPayloadProof.builder()
                            .amount(136)
                            .keysetId("009a1f293253e41e")
                            .secret("voucher-secret4")
                            .signature("02jkl012")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                            .build()
            );

            VoucherPaymentPayload payload = VoucherPaymentPayload.fromRequest(
                    decoded,
                    proofs,
                    "https://mint.example.com",
                    "sat",
                    "Redeeming gift card at merchant"
            );

            // 6. Verify payload matches request
            assertThat(payload.getId()).isEqualTo("voucher-order-12345");
            assertThat(payload.getIssuerId()).isEqualTo("gift-card-issuer-xyz");
            assertThat(payload.getTotalAmount()).isEqualTo(5000);
            assertThat(payload.allProofsHaveDLEQ()).isTrue();

            // 7. Validate for offline verification
            payload.validateForOfflineVerification(); // Should not throw

            // 8. Serialize payload for transmission
            String payloadJson = payload.toJson();
            assertThat(payloadJson).contains("voucher-order-12345");
            assertThat(payloadJson).contains("gift-card-issuer-xyz");

            // 9. Receiver/Merchant deserializes and validates
            VoucherPaymentPayload receivedPayload = VoucherPaymentPayload.fromJson(payloadJson);
            assertThat(receivedPayload.getId()).isEqualTo(request.getPaymentId());
            assertThat(receivedPayload.getIssuerId()).isEqualTo(request.getIssuerId());
            assertThat(receivedPayload.getTotalAmount()).isEqualTo(request.getAmount());
        }

        @Test
        void shouldHandleOpenAmountVoucherRequest() {
            // Request without specific amount (variable value gift card)
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .paymentId("variable-voucher-001")
                    .issuerId("variable-issuer")
                    .description("Variable value gift card - any amount")
                    .transports(List.of(VoucherTransport.merchant("https://merchant.com/redeem")))
                    .build();

            String encoded = request.serialize();
            VoucherPaymentRequest decoded = VoucherPaymentRequest.deserialize(encoded);

            assertThat(decoded.hasAmount()).isFalse();
            assertThat(decoded.getIssuerId()).isEqualTo("variable-issuer");
            assertThat(decoded.getDescription()).isEqualTo("Variable value gift card - any amount");
        }
    }

    @Nested
    @DisplayName("Merchant Transport")
    class MerchantTransportTests {

        @Test
        void shouldCreateMerchantTransportRequest() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .paymentId("merchant-pay-001")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(1000)
                    .unit("sat")
                    .transports(List.of(
                            VoucherTransport.merchant("https://merchant.example.com/redeem", "m-123")
                    ))
                    .build();

            String encoded = request.serialize();
            VoucherPaymentRequest decoded = VoucherPaymentRequest.deserialize(encoded);

            assertThat(decoded.getTransports()).hasSize(1);
            VoucherTransport transport = decoded.getPreferredTransport();
            assertThat(transport.getType()).isEqualTo(VoucherTransportType.MERCHANT);
            assertThat(transport.getTarget()).isEqualTo("https://merchant.example.com/redeem");
            assertThat(transport.getTagValue("merchant_id")).isEqualTo("m-123");
            assertThat(transport.isMerchant()).isTrue();
        }

        @Test
        void shouldGetMerchantTransportFromMixedTransports() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .paymentId("multi-transport-001")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(2000)
                    .unit("sat")
                    .transports(List.of(
                            VoucherTransport.httpPost("https://callback.example.com"),
                            VoucherTransport.merchant("https://merchant.example.com/redeem"),
                            VoucherTransport.nostrNip17("nprofile1abc")
                    ))
                    .build();

            String encoded = request.serialize();
            VoucherPaymentRequest decoded = VoucherPaymentRequest.deserialize(encoded);

            VoucherTransport merchantTransport = decoded.getMerchantTransport();
            assertThat(merchantTransport).isNotNull();
            assertThat(merchantTransport.isMerchant()).isTrue();
            assertThat(merchantTransport.getTarget()).isEqualTo("https://merchant.example.com/redeem");
        }

        @Test
        void shouldHandleMultipleMerchantTransports() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .paymentId("multi-merchant-001")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .transports(List.of(
                            VoucherTransport.merchant("https://merchant1.com/redeem", "m1"),
                            VoucherTransport.merchant("https://merchant2.com/redeem", "m2")
                    ))
                    .build();

            String encoded = request.serialize();
            VoucherPaymentRequest decoded = VoucherPaymentRequest.deserialize(encoded);

            assertThat(decoded.getTransports()).hasSize(2);
            assertThat(decoded.getMerchantTransport().getTarget()).isEqualTo("https://merchant1.com/redeem");
        }
    }

    @Nested
    @DisplayName("Offline Verification")
    class OfflineVerificationTests {

        @Test
        void shouldRequestOfflineVerification() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .paymentId("offline-pay-001")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(500)
                    .unit("sat")
                    .offlineVerification(true)
                    .build();

            String encoded = request.serialize();
            VoucherPaymentRequest decoded = VoucherPaymentRequest.deserialize(encoded);

            assertThat(decoded.requiresOfflineVerification()).isTrue();
        }

        @Test
        void shouldValidatePayloadForOfflineVerification() {
            List<PaymentPayloadProof> proofsWithDLEQ = List.of(
                    PaymentPayloadProof.builder()
                            .amount(100)
                            .keysetId("keyset1")
                            .secret("secret1")
                            .signature("sig1")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                            .build()
            );

            VoucherPaymentPayload payload = VoucherPaymentPayload.builder()
                    .id("offline-test")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .proofs(proofsWithDLEQ)
                    .build();

            // Should not throw
            payload.validateForOfflineVerification();
        }

        @Test
        void shouldFailValidationWithoutDLEQ() {
            List<PaymentPayloadProof> proofsWithoutDLEQ = List.of(
                    PaymentPayloadProof.builder()
                            .amount(100)
                            .keysetId("keyset1")
                            .secret("secret1")
                            .signature("sig1")
                            .build()
            );

            VoucherPaymentPayload payload = VoucherPaymentPayload.builder()
                    .id("offline-test")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .proofs(proofsWithoutDLEQ)
                    .build();

            assertThrows(IllegalStateException.class, payload::validateForOfflineVerification);
        }

        @Test
        void shouldFailValidationWithDLEQButNoBlindingFactor() {
            // DLEQ with only e and s, but missing r (blinding factor)
            // This is valid for basic DLEQ but NOT sufficient for offline verification
            List<PaymentPayloadProof> proofsWithIncompleteeDLEQ = List.of(
                    PaymentPayloadProof.builder()
                            .amount(100)
                            .keysetId("keyset1")
                            .secret("secret1")
                            .signature("sig1")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).build()) // No r!
                            .build()
            );

            VoucherPaymentPayload payload = VoucherPaymentPayload.builder()
                    .id("offline-test-no-r")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .proofs(proofsWithIncompleteeDLEQ)
                    .build();

            // hasDLEQ returns true (DLEQ object exists)
            assertThat(payload.allProofsHaveDLEQ()).isTrue();
            // But hasDLEQWithBlindingFactor returns false (r is missing)
            assertThat(payload.allProofsHaveDLEQWithBlindingFactor()).isFalse();
            // Validation for offline verification should fail
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    payload::validateForOfflineVerification);
            assertThat(ex.getMessage()).contains("blinding factor");
        }

        @Test
        void shouldCheckAllProofsHaveDLEQWithBlindingFactor() {
            List<PaymentPayloadProof> proofsWithFullDLEQ = List.of(
                    PaymentPayloadProof.builder()
                            .amount(100)
                            .keysetId("keyset1")
                            .secret("secret1")
                            .signature("sig1")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                            .build()
            );

            VoucherPaymentPayload payload = VoucherPaymentPayload.builder()
                    .id("offline-test-full")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .proofs(proofsWithFullDLEQ)
                    .build();

            assertThat(payload.allProofsHaveDLEQ()).isTrue();
            assertThat(payload.allProofsHaveDLEQWithBlindingFactor()).isTrue();
            // Should not throw
            payload.validateForOfflineVerification();
        }

        @Test
        void shouldFailValidationWithoutIssuerId() {
            List<PaymentPayloadProof> proofsWithDLEQ = List.of(
                    PaymentPayloadProof.builder()
                            .amount(100)
                            .keysetId("keyset1")
                            .secret("secret1")
                            .signature("sig1")
                            .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                    .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                            .build()
            );

            VoucherPaymentPayload payload = VoucherPaymentPayload.builder()
                    .id("offline-test")
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .proofs(proofsWithDLEQ)
                    .build();

            assertThrows(IllegalStateException.class, payload::validateForOfflineVerification);
        }
    }

    @Nested
    @DisplayName("NUT-10 Locking Conditions")
    class Nut10LockingTests {

        @Test
        void shouldCreateVoucherLockedRequest() {
            String voucherData = "ef".repeat(32);

            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .paymentId("voucher-locked-001")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(2500)
                    .unit("sat")
                    .nut10Option(Nut10Option.forVoucher(voucherData))
                    .build();

            String encoded = request.serialize();
            VoucherPaymentRequest decoded = VoucherPaymentRequest.deserialize(encoded);

            assertThat(decoded.hasNut10Locking()).isTrue();
            assertThat(decoded.getNut10Option().getKind()).isEqualTo(WellKnownSecret.Kind.VOUCHER);
            assertThat(decoded.getNut10Option().isVoucher()).isTrue();
        }

        @Test
        void shouldCreateP2PKLockedVoucherRequest() {
            String pubKeyHex = "02" + "ab".repeat(32);

            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .paymentId("p2pk-voucher")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(5000)
                    .unit("sat")
                    .nut10Option(Nut10Option.forP2PK(pubKeyHex))
                    .build();

            String encoded = request.serialize();
            VoucherPaymentRequest decoded = VoucherPaymentRequest.deserialize(encoded);

            assertThat(decoded.getNut10Option().getKind()).isEqualTo(WellKnownSecret.Kind.P2PK);
            assertThat(decoded.getNut10Option().getData()).isEqualTo(pubKeyHex);
        }
    }

    @Nested
    @DisplayName("Validation Rules")
    class ValidationRulesTests {

        @Test
        void shouldRejectMissingIssuerId() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .amount(100)
                    .unit("sat")
                    .build();

            assertThrows(IllegalStateException.class, () -> request.serialize());
        }

        @Test
        void shouldRejectBlankIssuerId() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("   ")
                    .amount(100)
                    .unit("sat")
                    .build();

            assertThrows(IllegalStateException.class, () -> request.serialize());
        }

        @Test
        void shouldRejectAmountWithoutUnit() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(100)
                    .build();

            assertThrows(IllegalStateException.class, () -> request.serialize());
        }

        @Test
        void shouldAllowRequestWithoutAmount() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId(SAMPLE_ISSUER_ID)
                    .description("Open voucher request")
                    .build();

            // Should not throw
            String encoded = request.serialize();
            assertThat(encoded).startsWith("vreqA");
        }

        @Test
        void shouldValidateSingleUseFlag() {
            VoucherPaymentRequest singleUse = VoucherPaymentRequest.builder()
                    .paymentId("single-use-voucher")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(100)
                    .unit("sat")
                    .singleUse(true)
                    .build();

            String encoded = singleUse.serialize();
            VoucherPaymentRequest decoded = VoucherPaymentRequest.deserialize(encoded);

            assertThat(decoded.isSingleUseRequest()).isTrue();
        }
    }

    @Nested
    @DisplayName("Prefix and Format")
    class PrefixAndFormatTests {

        @Test
        void shouldUseVreqPrefix() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId(SAMPLE_ISSUER_ID)
                    .build();

            String encoded = request.serialize();

            assertThat(encoded).startsWith("vreqA");
            assertThat(encoded).doesNotStartWith("creqA");
        }

        @Test
        void shouldRejectCreqPrefix() {
            // Create a valid creq payment request string (NUT-18)
            // Attempting to deserialize as VoucherPaymentRequest should fail
            assertThrows(IllegalArgumentException.class, () ->
                    VoucherPaymentRequest.deserialize("creqAsomedata"));
        }

        @Test
        void shouldRejectInvalidVersionCode() {
            assertThrows(IllegalArgumentException.class, () ->
                    VoucherPaymentRequest.deserialize("vreqBsomedata"));
        }

        @Test
        void shouldDifferFromStandardPaymentRequest() {
            // Both should work independently
            VoucherPaymentRequest voucherRequest = VoucherPaymentRequest.builder()
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(100)
                    .unit("sat")
                    .build();

            String voucherEncoded = voucherRequest.serialize();

            assertThat(voucherEncoded).startsWith("vreqA");
            assertThat(VoucherPaymentRequest.REQUEST_PREFIX).isEqualTo("vreq");
        }
    }

    @Nested
    @DisplayName("Voucher Payment Payload")
    class VoucherPaymentPayloadTests {

        @Test
        void shouldCreatePayloadFromRequest() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .paymentId("payload-test-001")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .amount(1000)
                    .unit("sat")
                    .build();

            List<PaymentPayloadProof> proofs = List.of(
                    PaymentPayloadProof.builder()
                            .amount(1000)
                            .keysetId("keyset1")
                            .secret("secret1")
                            .signature("sig1")
                            .build()
            );

            VoucherPaymentPayload payload = VoucherPaymentPayload.fromRequest(
                    request, proofs, "https://mint.example.com", "sat");

            assertThat(payload.getId()).isEqualTo("payload-test-001");
            assertThat(payload.getIssuerId()).isEqualTo(SAMPLE_ISSUER_ID);
            assertThat(payload.getMint()).isEqualTo("https://mint.example.com");
            assertThat(payload.getUnit()).isEqualTo("sat");
            assertThat(payload.getTotalAmount()).isEqualTo(1000);
        }

        @Test
        void shouldSerializeAndDeserializePayload() {
            VoucherPaymentPayload original = VoucherPaymentPayload.builder()
                    .id("json-test-001")
                    .issuerId(SAMPLE_ISSUER_ID)
                    .memo("Test memo")
                    .mint("https://mint.example.com")
                    .unit("sat")
                    .proofs(List.of(
                            PaymentPayloadProof.builder()
                                    .amount(500)
                                    .keysetId("keyset1")
                                    .secret("secret1")
                                    .signature("sig1")
                                    .dleq(PaymentPayloadProof.PaymentPayloadDLEQ.builder()
                                            .e(SAMPLE_E).s(SAMPLE_S).r(SAMPLE_R).build())
                                    .build()
                    ))
                    .build();

            String json = original.toJson();
            VoucherPaymentPayload restored = VoucherPaymentPayload.fromJson(json);

            assertThat(restored.getId()).isEqualTo("json-test-001");
            assertThat(restored.getIssuerId()).isEqualTo(SAMPLE_ISSUER_ID);
            assertThat(restored.getMemo()).isEqualTo("Test memo");
            assertThat(restored.getMint()).isEqualTo("https://mint.example.com");
            assertThat(restored.getUnit()).isEqualTo("sat");
            assertThat(restored.getTotalAmount()).isEqualTo(500);
            assertThat(restored.allProofsHaveDLEQ()).isTrue();
        }

        @Test
        void shouldStripTrailingSlashFromMint() {
            VoucherPaymentPayload payload = new VoucherPaymentPayload();
            payload.setMint("https://mint.example.com///");

            assertThat(payload.getMint()).isEqualTo("https://mint.example.com");
        }

        @Test
        void shouldCountProofs() {
            VoucherPaymentPayload payload = VoucherPaymentPayload.builder()
                    .proofs(List.of(
                            PaymentPayloadProof.builder().amount(100).keysetId("k1").secret("s1").signature("sig1").build(),
                            PaymentPayloadProof.builder().amount(200).keysetId("k1").secret("s2").signature("sig2").build(),
                            PaymentPayloadProof.builder().amount(300).keysetId("k1").secret("s3").signature("sig3").build()
                    ))
                    .build();

            assertThat(payload.getProofCount()).isEqualTo(3);
            assertThat(payload.getTotalAmount()).isEqualTo(600);
        }

        @Test
        void shouldAddProof() {
            VoucherPaymentPayload payload = new VoucherPaymentPayload();
            payload.addProof(PaymentPayloadProof.builder()
                    .amount(100)
                    .keysetId("k1")
                    .secret("s1")
                    .signature("sig1")
                    .build());

            assertThat(payload.getProofCount()).isEqualTo(1);
            assertThat(payload.getTotalAmount()).isEqualTo(100);
        }
    }
}
