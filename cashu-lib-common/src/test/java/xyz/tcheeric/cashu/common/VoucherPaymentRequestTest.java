package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("VoucherPaymentRequest")
class VoucherPaymentRequestTest {

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        void shouldPassValidationWithRequiredFields() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .amount(100)
                    .unit("sat")
                    .build();

            request.validate(); // Should not throw
        }

        @Test
        void shouldPassValidationWithoutAmount() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .description("Open voucher redemption")
                    .build();

            request.validate(); // Should not throw
        }

        @Test
        void shouldFailValidationWithoutIssuerId() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .amount(100)
                    .unit("sat")
                    .build();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    request::validate);

            assertThat(ex.getMessage()).contains("Issuer ID");
        }

        @Test
        void shouldFailValidationWithBlankIssuerId() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("   ")
                    .amount(100)
                    .unit("sat")
                    .build();

            assertThrows(IllegalStateException.class, request::validate);
        }

        @Test
        void shouldFailValidationWithAmountButNoUnit() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .amount(100)
                    .build();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    request::validate);

            assertThat(ex.getMessage()).contains("Unit is required");
        }

        @Test
        void shouldFailValidationWithAmountAndBlankUnit() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .amount(100)
                    .unit("   ")
                    .build();

            assertThrows(IllegalStateException.class, request::validate);
        }
    }

    @Nested
    @DisplayName("Serialization")
    class SerializationTests {

        @Test
        void shouldSerializeMinimalRequest() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .amount(100)
                    .unit("sat")
                    .build();

            String encoded = request.serialize();

            assertThat(encoded).startsWith("vreqA");
            assertThat(encoded).doesNotStartWith("cashu:");
        }

        @Test
        void shouldSerializeClickableRequest() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .amount(100)
                    .unit("sat")
                    .build();

            String encoded = request.serialize(true);

            assertThat(encoded).startsWith("cashu:vreqA");
        }

        @Test
        void shouldSerializeFullRequest() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .paymentId("voucher-pay-123")
                    .issuerId("issuer-456")
                    .amount(1000)
                    .unit("sat")
                    .singleUse(true)
                    .offlineVerification(true)
                    .mints(List.of("https://mint.example.com"))
                    .description("Voucher payment for order #123")
                    .transports(List.of(VoucherTransport.merchant("https://merchant.example.com/redeem")))
                    .nut10Option(Nut10Option.forVoucher("voucher-data"))
                    .build();

            String encoded = request.serialize();

            assertThat(encoded).startsWith("vreqA");

            // Verify round-trip
            VoucherPaymentRequest restored = VoucherPaymentRequest.deserialize(encoded);
            assertThat(restored.getPaymentId()).isEqualTo("voucher-pay-123");
            assertThat(restored.getIssuerId()).isEqualTo("issuer-456");
            assertThat(restored.getAmount()).isEqualTo(1000);
            assertThat(restored.getUnit()).isEqualTo("sat");
            assertThat(restored.getSingleUse()).isTrue();
            assertThat(restored.getOfflineVerification()).isTrue();
            assertThat(restored.getDescription()).isEqualTo("Voucher payment for order #123");
        }

        @Test
        void shouldDeserializeWithUriScheme() {
            VoucherPaymentRequest original = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .amount(500)
                    .unit("sat")
                    .description("Test")
                    .build();

            String clickable = original.serialize(true);
            VoucherPaymentRequest restored = VoucherPaymentRequest.deserialize(clickable);

            assertThat(restored.getIssuerId()).isEqualTo("issuer-123");
            assertThat(restored.getAmount()).isEqualTo(500);
            assertThat(restored.getUnit()).isEqualTo("sat");
            assertThat(restored.getDescription()).isEqualTo("Test");
        }

        @Test
        void shouldRejectInvalidPrefix() {
            assertThrows(IllegalArgumentException.class, () ->
                    VoucherPaymentRequest.deserialize("invalidPrefix123"));
        }

        @Test
        void shouldRejectWrongPrefix() {
            assertThrows(IllegalArgumentException.class, () ->
                    VoucherPaymentRequest.deserialize("creqA" + "somedata"));
        }

        @Test
        void shouldRejectWrongVersionCode() {
            assertThrows(IllegalArgumentException.class, () ->
                    VoucherPaymentRequest.deserialize("vreqB" + "somedata"));
        }
    }

    @Nested
    @DisplayName("Mint Management")
    class MintManagementTests {

        @Test
        void shouldAddMintWithTrailingSlashStripped() {
            VoucherPaymentRequest request = new VoucherPaymentRequest();
            request.addMint("https://mint.example.com/");

            assertThat(request.getMints()).containsExactly("https://mint.example.com");
        }

        @Test
        void shouldAddMultipleMints() {
            VoucherPaymentRequest request = new VoucherPaymentRequest();
            request.addMint("https://mint1.example.com");
            request.addMint("https://mint2.example.com");

            assertThat(request.getMints()).hasSize(2);
        }

        @Test
        void shouldPermitAnyMintWhenNoMintsSpecified() {
            VoucherPaymentRequest request = new VoucherPaymentRequest();

            assertThat(request.isMintPermitted("https://any.mint.com")).isTrue();
        }

        @Test
        void shouldPermitListedMint() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .mints(List.of("https://mint.example.com"))
                    .build();

            assertThat(request.isMintPermitted("https://mint.example.com")).isTrue();
            assertThat(request.isMintPermitted("https://mint.example.com/")).isTrue();
        }

        @Test
        void shouldRejectUnlistedMint() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .mints(List.of("https://mint.example.com"))
                    .build();

            assertThat(request.isMintPermitted("https://other.mint.com")).isFalse();
        }

        @Test
        void shouldPermitMintCaseInsensitive() {
            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .mints(List.of("https://Mint.Example.COM"))
                    .build();

            assertThat(request.isMintPermitted("https://mint.example.com")).isTrue();
        }
    }

    @Nested
    @DisplayName("Transport Management")
    class TransportManagementTests {

        @Test
        void shouldAddTransport() {
            VoucherPaymentRequest request = new VoucherPaymentRequest();
            request.addTransport(VoucherTransport.merchant("https://merchant.example.com/redeem"));

            assertThat(request.getTransports()).hasSize(1);
        }

        @Test
        void shouldReturnPreferredTransport() {
            VoucherTransport preferred = VoucherTransport.merchant("https://first.com");
            VoucherTransport secondary = VoucherTransport.nostrNip17("nprofile1abc");

            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .transports(List.of(preferred, secondary))
                    .build();

            assertThat(request.getPreferredTransport()).isEqualTo(preferred);
        }

        @Test
        void shouldReturnNullPreferredTransportWhenEmpty() {
            VoucherPaymentRequest request = new VoucherPaymentRequest();

            assertThat(request.getPreferredTransport()).isNull();
        }

        @Test
        void shouldReturnMerchantTransport() {
            VoucherTransport nostr = VoucherTransport.nostrNip17("nprofile1abc");
            VoucherTransport merchant = VoucherTransport.merchant("https://merchant.com");
            VoucherTransport post = VoucherTransport.httpPost("https://callback.com");

            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .transports(List.of(nostr, merchant, post))
                    .build();

            assertThat(request.getMerchantTransport()).isEqualTo(merchant);
        }

        @Test
        void shouldReturnNullWhenNoMerchantTransport() {
            VoucherTransport nostr = VoucherTransport.nostrNip17("nprofile1abc");
            VoucherTransport post = VoucherTransport.httpPost("https://callback.com");

            VoucherPaymentRequest request = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .transports(List.of(nostr, post))
                    .build();

            assertThat(request.getMerchantTransport()).isNull();
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethodTests {

        @Test
        void shouldDetectAmountPresence() {
            VoucherPaymentRequest withAmount = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .amount(100)
                    .unit("sat")
                    .build();
            VoucherPaymentRequest withoutAmount = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .description("Open")
                    .build();

            assertThat(withAmount.hasAmount()).isTrue();
            assertThat(withoutAmount.hasAmount()).isFalse();
        }

        @Test
        void shouldDetectSingleUse() {
            VoucherPaymentRequest singleUse = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .singleUse(true)
                    .build();
            VoucherPaymentRequest multiUse = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .singleUse(false)
                    .build();
            VoucherPaymentRequest unset = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .build();

            assertThat(singleUse.isSingleUseRequest()).isTrue();
            assertThat(multiUse.isSingleUseRequest()).isFalse();
            assertThat(unset.isSingleUseRequest()).isFalse();
        }

        @Test
        void shouldDetectOfflineVerification() {
            VoucherPaymentRequest withOffline = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .offlineVerification(true)
                    .build();
            VoucherPaymentRequest withoutOffline = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .offlineVerification(false)
                    .build();
            VoucherPaymentRequest unset = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .build();

            assertThat(withOffline.requiresOfflineVerification()).isTrue();
            assertThat(withoutOffline.requiresOfflineVerification()).isFalse();
            assertThat(unset.requiresOfflineVerification()).isFalse();
        }

        @Test
        void shouldDetectNut10Locking() {
            VoucherPaymentRequest withLocking = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .nut10Option(Nut10Option.forVoucher("voucher-data"))
                    .build();
            VoucherPaymentRequest withoutLocking = VoucherPaymentRequest.builder()
                    .issuerId("issuer-123")
                    .build();

            assertThat(withLocking.hasNut10Locking()).isTrue();
            assertThat(withoutLocking.hasNut10Locking()).isFalse();
        }
    }

    @Nested
    @DisplayName("Round-Trip Serialization")
    class RoundTripTests {

        @Test
        void shouldRoundTripWithAllFields() {
            VoucherPaymentRequest original = VoucherPaymentRequest.builder()
                    .paymentId("test-voucher-001")
                    .issuerId("issuer-xyz-789")
                    .amount(2500)
                    .unit("sat")
                    .singleUse(true)
                    .offlineVerification(true)
                    .mints(List.of("https://mint1.com", "https://mint2.com"))
                    .description("Complete voucher payment test")
                    .transports(List.of(
                            VoucherTransport.merchant("https://merchant.example.com", "m123"),
                            VoucherTransport.httpPost("https://callback.example.com"),
                            VoucherTransport.nostrNip17("nprofile1xyz")
                    ))
                    .nut10Option(Nut10Option.forVoucher("voucher-secret-data"))
                    .build();

            String encoded = original.serialize();
            VoucherPaymentRequest restored = VoucherPaymentRequest.deserialize(encoded);

            assertThat(restored.getPaymentId()).isEqualTo(original.getPaymentId());
            assertThat(restored.getIssuerId()).isEqualTo(original.getIssuerId());
            assertThat(restored.getAmount()).isEqualTo(original.getAmount());
            assertThat(restored.getUnit()).isEqualTo(original.getUnit());
            assertThat(restored.getSingleUse()).isEqualTo(original.getSingleUse());
            assertThat(restored.getOfflineVerification()).isEqualTo(original.getOfflineVerification());
            assertThat(restored.getMints()).isEqualTo(original.getMints());
            assertThat(restored.getDescription()).isEqualTo(original.getDescription());
            assertThat(restored.getTransports()).hasSize(3);
            assertThat(restored.getNut10Option()).isNotNull();
            assertThat(restored.getNut10Option().getKind()).isEqualTo(WellKnownSecret.Kind.VOUCHER);
        }

        @Test
        void shouldRoundTripMinimalRequest() {
            VoucherPaymentRequest original = VoucherPaymentRequest.builder()
                    .issuerId("min-issuer")
                    .build();

            String encoded = original.serialize();
            VoucherPaymentRequest restored = VoucherPaymentRequest.deserialize(encoded);

            assertThat(restored.getIssuerId()).isEqualTo("min-issuer");
            assertThat(restored.getPaymentId()).isNull();
            assertThat(restored.getAmount()).isNull();
            assertThat(restored.getUnit()).isNull();
        }

        @Test
        void shouldRoundTripWithMerchantTransport() {
            VoucherPaymentRequest original = VoucherPaymentRequest.builder()
                    .issuerId("merchant-issuer")
                    .amount(5000)
                    .unit("usd")
                    .transports(List.of(VoucherTransport.merchant("https://merchant.com/redeem", "m456")))
                    .build();

            String encoded = original.serialize();
            VoucherPaymentRequest restored = VoucherPaymentRequest.deserialize(encoded);

            assertThat(restored.getTransports()).hasSize(1);
            VoucherTransport transport = restored.getTransports().get(0);
            assertThat(transport.isMerchant()).isTrue();
            assertThat(transport.getTarget()).isEqualTo("https://merchant.com/redeem");
            assertThat(transport.getTagValue("merchant_id")).isEqualTo("m456");
        }
    }

    @Nested
    @DisplayName("Prefix Verification")
    class PrefixTests {

        @Test
        void shouldUseVreqPrefix() {
            assertThat(VoucherPaymentRequest.REQUEST_PREFIX).isEqualTo("vreq");
        }

        @Test
        void shouldUseVersionCodeA() {
            assertThat(VoucherPaymentRequest.VERSION_CODE).isEqualTo("A");
        }

        @Test
        void shouldUseCashuUriScheme() {
            assertThat(VoucherPaymentRequest.URI_SCHEME).isEqualTo("cashu:");
        }

        @Test
        void shouldDifferFromPaymentRequestPrefix() {
            assertThat(VoucherPaymentRequest.REQUEST_PREFIX)
                    .isNotEqualTo(PaymentRequest.REQUEST_PREFIX);
        }
    }
}
