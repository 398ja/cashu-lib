package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut10.Nut10Option;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut18.PaymentRequest;
import xyz.tcheeric.cashu.common.nut18.Transport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PaymentRequest")
class PaymentRequestTest {

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        void shouldPassValidationWithAmountAndUnit() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(100)
                    .unit("sat")
                    .build();

            request.validate(); // Should not throw
        }

        @Test
        void shouldPassValidationWithoutAmount() {
            PaymentRequest request = PaymentRequest.builder()
                    .description("Open donation")
                    .build();

            request.validate(); // Should not throw
        }

        @Test
        void shouldFailValidationWithAmountButNoUnit() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(100)
                    .build();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    request::validate);

            assertThat(ex.getMessage()).contains("Unit is required");
        }

        @Test
        void shouldFailValidationWithAmountAndBlankUnit() {
            PaymentRequest request = PaymentRequest.builder()
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
            PaymentRequest request = PaymentRequest.builder()
                    .amount(100)
                    .unit("sat")
                    .build();

            String encoded = request.serialize();

            assertThat(encoded).startsWith("creqA");
            assertThat(encoded).doesNotStartWith("cashu:");
        }

        @Test
        void shouldSerializeClickableRequest() {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(100)
                    .unit("sat")
                    .build();

            String encoded = request.serialize(true);

            assertThat(encoded).startsWith("cashu:creqA");
        }

        @Test
        void shouldSerializeFullRequest() {
            PaymentRequest request = PaymentRequest.builder()
                    .paymentId("pay-123")
                    .amount(1000)
                    .unit("sat")
                    .singleUse(true)
                    .mints(List.of("https://mint.example.com"))
                    .description("Payment for order #123")
                    .transports(List.of(Transport.httpPost("https://api.example.com/callback")))
                    .nut10Option(Nut10Option.forP2PK("02" + "ab".repeat(32)))
                    .build();

            String encoded = request.serialize();

            assertThat(encoded).startsWith("creqA");

            // Verify round-trip
            PaymentRequest restored = PaymentRequest.deserialize(encoded);
            assertThat(restored.getPaymentId()).isEqualTo("pay-123");
            assertThat(restored.getAmount()).isEqualTo(1000);
            assertThat(restored.getUnit()).isEqualTo("sat");
            assertThat(restored.getSingleUse()).isTrue();
            assertThat(restored.getDescription()).isEqualTo("Payment for order #123");
        }

        @Test
        void shouldDeserializeWithUriScheme() {
            PaymentRequest original = PaymentRequest.builder()
                    .amount(500)
                    .unit("sat")
                    .description("Test")
                    .build();

            String clickable = original.serialize(true);
            PaymentRequest restored = PaymentRequest.deserialize(clickable);

            assertThat(restored.getAmount()).isEqualTo(500);
            assertThat(restored.getUnit()).isEqualTo("sat");
            assertThat(restored.getDescription()).isEqualTo("Test");
        }

        @Test
        void shouldRejectInvalidPrefix() {
            assertThrows(IllegalArgumentException.class, () ->
                    PaymentRequest.deserialize("invalidPrefix123"));
        }

        @Test
        void shouldRejectWrongVersionCode() {
            assertThrows(IllegalArgumentException.class, () ->
                    PaymentRequest.deserialize("creqB" + "somedata"));
        }
    }

    @Nested
    @DisplayName("Mint Management")
    class MintManagementTests {

        @Test
        void shouldAddMintWithTrailingSlashStripped() {
            PaymentRequest request = new PaymentRequest();
            request.addMint("https://mint.example.com/");

            assertThat(request.getMints()).containsExactly("https://mint.example.com");
        }

        @Test
        void shouldAddMultipleMints() {
            PaymentRequest request = new PaymentRequest();
            request.addMint("https://mint1.example.com");
            request.addMint("https://mint2.example.com");

            assertThat(request.getMints()).hasSize(2);
        }

        @Test
        void shouldPermitAnyMintWhenNoMintsSpecified() {
            PaymentRequest request = new PaymentRequest();

            assertThat(request.isMintPermitted("https://any.mint.com")).isTrue();
        }

        @Test
        void shouldPermitListedMint() {
            PaymentRequest request = PaymentRequest.builder()
                    .mints(List.of("https://mint.example.com"))
                    .build();

            assertThat(request.isMintPermitted("https://mint.example.com")).isTrue();
            assertThat(request.isMintPermitted("https://mint.example.com/")).isTrue();
        }

        @Test
        void shouldRejectUnlistedMint() {
            PaymentRequest request = PaymentRequest.builder()
                    .mints(List.of("https://mint.example.com"))
                    .build();

            assertThat(request.isMintPermitted("https://other.mint.com")).isFalse();
        }

        @Test
        void shouldPermitMintCaseInsensitive() {
            PaymentRequest request = PaymentRequest.builder()
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
            PaymentRequest request = new PaymentRequest();
            request.addTransport(Transport.httpPost("https://example.com/callback"));

            assertThat(request.getTransports()).hasSize(1);
        }

        @Test
        void shouldReturnPreferredTransport() {
            Transport preferred = Transport.httpPost("https://first.com");
            Transport secondary = Transport.nostrNip17("nprofile1abc");

            PaymentRequest request = PaymentRequest.builder()
                    .transports(List.of(preferred, secondary))
                    .build();

            assertThat(request.getPreferredTransport()).isEqualTo(preferred);
        }

        @Test
        void shouldReturnNullPreferredTransportWhenEmpty() {
            PaymentRequest request = new PaymentRequest();

            assertThat(request.getPreferredTransport()).isNull();
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethodTests {

        @Test
        void shouldDetectAmountPresence() {
            PaymentRequest withAmount = PaymentRequest.builder().amount(100).unit("sat").build();
            PaymentRequest withoutAmount = PaymentRequest.builder().description("Open").build();

            assertThat(withAmount.hasAmount()).isTrue();
            assertThat(withoutAmount.hasAmount()).isFalse();
        }

        @Test
        void shouldDetectSingleUse() {
            PaymentRequest singleUse = PaymentRequest.builder().singleUse(true).build();
            PaymentRequest multiUse = PaymentRequest.builder().singleUse(false).build();
            PaymentRequest unset = PaymentRequest.builder().build();

            assertThat(singleUse.isSingleUseRequest()).isTrue();
            assertThat(multiUse.isSingleUseRequest()).isFalse();
            assertThat(unset.isSingleUseRequest()).isFalse();
        }

        @Test
        void shouldDetectNut10Locking() {
            PaymentRequest withLocking = PaymentRequest.builder()
                    .nut10Option(Nut10Option.forP2PK("02" + "ab".repeat(32)))
                    .build();
            PaymentRequest withoutLocking = PaymentRequest.builder().build();

            assertThat(withLocking.hasNut10Locking()).isTrue();
            assertThat(withoutLocking.hasNut10Locking()).isFalse();
        }
    }

    @Nested
    @DisplayName("Round-Trip Serialization")
    class RoundTripTests {

        @Test
        void shouldRoundTripWithAllFields() {
            PaymentRequest original = PaymentRequest.builder()
                    .paymentId("test-payment-001")
                    .amount(2500)
                    .unit("sat")
                    .singleUse(true)
                    .mints(List.of("https://mint1.com", "https://mint2.com"))
                    .description("Complete payment test")
                    .transports(List.of(
                            Transport.httpPost("https://callback.example.com"),
                            Transport.nostrNip17("nprofile1xyz")
                    ))
                    .nut10Option(Nut10Option.forHTLC("ab".repeat(32)))
                    .build();

            String encoded = original.serialize();
            PaymentRequest restored = PaymentRequest.deserialize(encoded);

            assertThat(restored.getPaymentId()).isEqualTo(original.getPaymentId());
            assertThat(restored.getAmount()).isEqualTo(original.getAmount());
            assertThat(restored.getUnit()).isEqualTo(original.getUnit());
            assertThat(restored.getSingleUse()).isEqualTo(original.getSingleUse());
            assertThat(restored.getMints()).isEqualTo(original.getMints());
            assertThat(restored.getDescription()).isEqualTo(original.getDescription());
            assertThat(restored.getTransports()).hasSize(2);
            assertThat(restored.getNut10Option()).isNotNull();
            assertThat(restored.getNut10Option().getKind()).isEqualTo(WellKnownSecret.Kind.HTLC);
        }

        @Test
        void shouldRoundTripMinimalRequest() {
            PaymentRequest original = PaymentRequest.builder().build();

            String encoded = original.serialize();
            PaymentRequest restored = PaymentRequest.deserialize(encoded);

            assertThat(restored.getPaymentId()).isNull();
            assertThat(restored.getAmount()).isNull();
            assertThat(restored.getUnit()).isNull();
        }
    }
}
