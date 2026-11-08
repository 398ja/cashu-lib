package xyz.tcheeric.cashu.entities.rest;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.LiteralSecret;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.nut00.CashuErrorCode;
import xyz.tcheeric.cashu.common.util.CashuErrorException;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.common.voucher.VoucherDocuments;
import xyz.tcheeric.cashu.entities.rest.nut03.PostSwapRequest;
import xyz.tcheeric.cashu.entities.rest.nut05.PostMeltRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the optional {@code vouchers} map on swap and melt requests (cashu-voucher#64).
 */
class PostInputRequestVoucherDocumentsTest {

    private static final String GOLDEN_SWAP =
            "{\"inputs\":[{\"amount\":2,\"id\":\"009a1f293253e41e\",\"secret\":\"s1\",\"C\":null}],\"outputs\":[]}";
    private static final String GOLDEN_MELT =
            "{\"inputs\":[{\"amount\":2,\"id\":\"009a1f293253e41e\",\"secret\":\"s1\",\"C\":null}],\"quote\":\"q1\"}";
    private static final byte[] DOC = "voucher-document".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OTHER_DOC = "other-document".getBytes(StandardCharsets.UTF_8);

    private static List<Proof<LiteralSecret>> inputs() {
        Proof<LiteralSecret> proof = new Proof<>();
        proof.setAmount(2);
        proof.setKeySetId("009a1f293253e41e");
        proof.setSecret(LiteralSecret.of("s1"));
        return new ArrayList<>(List.of(proof));
    }

    private static PostSwapRequest<LiteralSecret> swap() {
        PostSwapRequest<LiteralSecret> request = new PostSwapRequest<>();
        request.setInputs(inputs());
        request.setBlindedMessages(new ArrayList<>());
        return request;
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    /** A swap request without documents serializes exactly as before this change. */
    @Test
    void shouldSerializeSwapIdenticallyWhenNoDocuments() throws Exception {
        assertThat(JsonUtils.JSON_MAPPER.writeValueAsString(swap())).isEqualTo(GOLDEN_SWAP);
    }

    /** A melt request without documents serializes exactly as before this change. */
    @Test
    void shouldSerializeMeltIdenticallyWhenNoDocuments() throws Exception {
        PostMeltRequest<LiteralSecret> request = new PostMeltRequest<>("q1", inputs());

        assertThat(JsonUtils.JSON_MAPPER.writeValueAsString(request)).isEqualTo(GOLDEN_MELT);
    }

    /** Documents round trip through JSON and decode to their bytes. */
    @Test
    void shouldRoundTripDocumentsWhenSwapCarriesThem() throws Exception {
        PostSwapRequest<LiteralSecret> request = swap();
        String key = VoucherDocuments.key(DOC);
        request.setVouchers(Map.of(key, hex(DOC)));

        String json = JsonUtils.JSON_MAPPER.writeValueAsString(request);
        PostSwapRequest<?> decoded = JsonUtils.JSON_MAPPER.readValue(json, PostSwapRequest.class);

        assertThat(json).contains("\"vouchers\":{\"" + key + "\":\"" + hex(DOC) + "\"}");
        assertThat(decoded.voucherDocuments().get(key)).isEqualTo(DOC);
    }

    /** A request without the map decodes with no documents. */
    @Test
    void shouldReturnNoDocumentsWhenMapIsAbsent() throws Exception {
        PostSwapRequest<?> decoded = JsonUtils.JSON_MAPPER.readValue(GOLDEN_SWAP, PostSwapRequest.class);

        assertThat(decoded.voucherDocuments()).isEmpty();
    }

    /** A key that is not lowercase hex is a typed protocol error. */
    @Test
    void shouldRaiseDocumentErrorWhenKeyIsUppercase() {
        PostSwapRequest<LiteralSecret> request = swap();
        request.setVouchers(Map.of(VoucherDocuments.key(DOC).toUpperCase(), hex(DOC)));

        assertDocumentError(request, "64 lowercase hex");
    }

    /** A key that does not hash its value is a typed protocol error. */
    @Test
    void shouldRaiseDocumentErrorWhenKeyDoesNotMatch() {
        PostSwapRequest<LiteralSecret> request = swap();
        request.setVouchers(Map.of(VoucherDocuments.key(OTHER_DOC), hex(DOC)));

        assertDocumentError(request, "does not match SHA-256");
    }

    /** An empty document is a typed protocol error. */
    @Test
    void shouldRaiseDocumentErrorWhenDocumentIsEmpty() {
        PostSwapRequest<LiteralSecret> request = swap();
        request.setVouchers(Map.of(VoucherDocuments.key(new byte[0]), ""));

        assertDocumentError(request, "must not be empty");
    }

    /** A document over 65,536 bytes is a typed protocol error. */
    @Test
    void shouldRaiseDocumentErrorWhenDocumentIsTooLarge() {
        byte[] huge = new byte[VoucherDocuments.MAX_DOCUMENT_BYTES + 1];
        PostSwapRequest<LiteralSecret> request = swap();
        request.setVouchers(Map.of(VoucherDocuments.key(huge), hex(huge)));

        assertDocumentError(request, "exceeds 65536 bytes");
    }

    /** More documents than inputs is a typed protocol error. */
    @Test
    void shouldRaiseDocumentErrorWhenDocumentsOutnumberInputs() {
        PostSwapRequest<LiteralSecret> request = swap();
        request.setVouchers(Map.of(VoucherDocuments.key(DOC), hex(DOC), VoucherDocuments.key(OTHER_DOC), hex(OTHER_DOC)));

        assertDocumentError(request, "count exceeds proof count");
    }

    /** A non-hex value is a typed protocol error. */
    @Test
    void shouldRaiseDocumentErrorWhenValueIsNotHex() {
        PostSwapRequest<LiteralSecret> request = swap();
        request.setVouchers(Map.of(VoucherDocuments.key(DOC), "not-hex"));

        assertDocumentError(request, "not valid hex");
    }

    /** The new error code is an extension code and does not collide with any other. */
    @Test
    void shouldUseUniqueExtensionCodeWhenReportingDocumentErrors() {
        CashuErrorCode code = CashuErrorCode.voucher_document_invalid;

        assertThat(code.isExtension()).isTrue();
        assertThat(CashuErrorCode.forCode(code.getCode())).contains(code);
    }

    private static void assertDocumentError(PostInputRequest<?> request, String message) {
        assertThatThrownBy(request::voucherDocuments)
                .isInstanceOfSatisfying(CashuErrorException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CashuErrorCode.voucher_document_invalid))
                .hasMessageContaining(message);
    }
}
