package xyz.tcheeric.cashu.common.voucher;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut18.PaymentPayload;
import xyz.tcheeric.cashu.common.nut18.PaymentPayloadProof;
import xyz.tcheeric.cashu.common.nut18.VoucherPaymentPayload;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the optional {@code vouchers} map on NUT-18 payment payloads (cashu-voucher#64).
 */
class PaymentPayloadVoucherDocumentsTest {

    private static final String GOLDEN_PAYMENT_PAYLOAD =
            "{\"id\":\"i\",\"memo\":\"m\",\"mint\":\"https://m\",\"unit\":\"sat\",\"proofs\":[{\"amount\":2,\"id\":\"009a1f293253e41e\",\"secret\":\"s1\",\"C\":\"02aa\"}]}";
    private static final String GOLDEN_VOUCHER_PAYMENT_PAYLOAD =
            "{\"id\":\"i\",\"r\":\"r\",\"mint\":\"https://m\",\"unit\":\"sat\",\"proofs\":[{\"amount\":2,\"id\":\"009a1f293253e41e\",\"secret\":\"s1\",\"C\":\"02aa\"}]}";
    private static final byte[] DOC = "voucher-document".getBytes(StandardCharsets.UTF_8);

    private static PaymentPayloadProof proof() {
        return PaymentPayloadProof.builder().amount(2).keysetId("009a1f293253e41e").secret("s1").signature("02aa").build();
    }

    private static PaymentPayload payload() {
        return PaymentPayload.builder().id("i").memo("m").mint("https://m").unit("sat").proofs(List.of(proof())).build();
    }

    private static VoucherPaymentPayload voucherPayload() {
        return VoucherPaymentPayload.builder().id("i").issuerId("r").mint("https://m").unit("sat").proofs(List.of(proof())).build();
    }

    /** A payload without documents serializes exactly as before this change. */
    @Test
    void shouldSerializeIdenticallyWhenPaymentPayloadHasNoDocuments() {
        assertThat(payload().toJson()).isEqualTo(GOLDEN_PAYMENT_PAYLOAD);
        assertThat(new PaymentPayload().toJson()).doesNotContain("vouchers");
    }

    /** A voucher payload without documents serializes exactly as before this change. */
    @Test
    void shouldSerializeIdenticallyWhenVoucherPaymentPayloadHasNoDocuments() {
        assertThat(voucherPayload().toJson()).isEqualTo(GOLDEN_VOUCHER_PAYMENT_PAYLOAD);
    }

    /** Documents round trip through JSON and decode to their bytes. */
    @Test
    void shouldRoundTripDocumentsWhenPayloadCarriesThem() {
        PaymentPayload payload = payload();
        String key = VoucherDocuments.key(DOC);
        payload.setVouchers(Map.of(key, HexFormat.of().formatHex(DOC)));

        PaymentPayload decoded = PaymentPayload.fromJson(payload.toJson());

        assertThat(payload.toJson()).endsWith(",\"vouchers\":{\"" + key + "\":\"" + HexFormat.of().formatHex(DOC) + "\"}}");
        assertThat(decoded.voucherDocuments().get(key)).isEqualTo(DOC);
    }

    /** Voucher payload documents round trip through JSON. */
    @Test
    void shouldRoundTripDocumentsWhenVoucherPayloadCarriesThem() {
        VoucherPaymentPayload payload = voucherPayload();
        String key = VoucherDocuments.key(DOC);
        payload.setVouchers(Map.of(key, HexFormat.of().formatHex(DOC)));

        VoucherPaymentPayload decoded = VoucherPaymentPayload.fromJson(payload.toJson());

        assertThat(decoded.voucherDocuments().get(key)).isEqualTo(DOC);
    }

    /** A key that does not hash its value is refused. */
    @Test
    void shouldRefuseDocumentsWhenKeyDoesNotMatch() {
        PaymentPayload payload = payload();
        payload.setVouchers(Map.of("00".repeat(32), HexFormat.of().formatHex(DOC)));

        assertThatThrownBy(payload::voucherDocuments)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match SHA-256");
    }

    /** A payload JSON without the map decodes with no documents. */
    @Test
    void shouldReturnNoDocumentsWhenMapIsAbsent() {
        assertThat(PaymentPayload.fromJson(GOLDEN_PAYMENT_PAYLOAD).voucherDocuments()).isEmpty();
    }
}
