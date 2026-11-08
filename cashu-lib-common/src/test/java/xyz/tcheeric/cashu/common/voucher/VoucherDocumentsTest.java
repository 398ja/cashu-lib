package xyz.tcheeric.cashu.common.voucher;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the shared voucher document container rules.
 */
class VoucherDocumentsTest {

    /** The key is lowercase hex SHA-256 of the document. */
    @Test
    void shouldHashDocumentWhenComputingKey() {
        String key = VoucherDocuments.key("abc".getBytes(StandardCharsets.UTF_8));

        assertThat(key).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    /** Only 32-byte data counts as a reference. */
    @Test
    void shouldDetectReferenceWhenDataIs32Bytes() {
        assertThat(VoucherDocuments.isReference(new byte[32])).isTrue();
        assertThat(VoucherDocuments.isReference(new byte[33])).isFalse();
    }

    /** A hex value that is not hex is refused. */
    @Test
    void shouldRefuseHexMapWhenValueIsNotHex() {
        assertThatThrownBy(() -> VoucherDocuments.fromHex(Map.of("00".repeat(32), "zz"), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid hex");
    }

    /** Hex encoding and decoding are inverse. */
    @Test
    void shouldRoundTripWhenConvertingHex() {
        byte[] doc = {1, 2, 3};
        Map<String, String> hex = VoucherDocuments.toHex(Map.of(VoucherDocuments.key(doc), doc));

        assertThat(VoucherDocuments.fromHex(hex, 1).get(VoucherDocuments.key(doc))).isEqualTo(doc);
    }

    /**
     * Sixteen full-size documents are exactly the 1 MiB total and are accepted. One byte more in
     * total is refused, even though every single document is within its own bound. Without the
     * total, 1000 inputs could carry 64 MiB.
     */
    @Test
    void shouldRefuseDocumentsWhenTheirTotalExceedsOneMebibyte() {
        Map<String, byte[]> atLimit = new java.util.TreeMap<>();
        for (int i = 0; i < 16; i++) {
            byte[] doc = new byte[VoucherDocuments.MAX_DOCUMENT_BYTES];
            java.util.Arrays.fill(doc, (byte) (i + 1));
            atLimit.put(VoucherDocuments.key(doc), doc);
        }
        VoucherDocuments.validate(atLimit, 1000);

        Map<String, byte[]> overLimit = new java.util.TreeMap<>(atLimit);
        byte[] extra = {42};
        overLimit.put(VoucherDocuments.key(extra), extra);

        assertThatThrownBy(() -> VoucherDocuments.validate(overLimit, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("in total");
    }

    /** The hex form is bounded before any value is decoded or hashed. */
    @Test
    void shouldRefuseHexMapWhenItsTotalExceedsOneMebibyteBeforeDecoding() {
        Map<String, String> hex = new java.util.TreeMap<>();
        // Not even valid hex: the size check must fire first, which proves nothing was decoded.
        String oversized = "z".repeat(2 * VoucherDocuments.MAX_TOTAL_DOCUMENT_BYTES + 2);
        hex.put("00".repeat(32), oversized);

        assertThatThrownBy(() -> VoucherDocuments.fromHex(hex, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("in total");
    }

    /** More hex entries than proofs is refused before decoding. */
    @Test
    void shouldRefuseHexMapWhenItHasMoreEntriesThanProofs() {
        Map<String, String> hex = Map.of("00".repeat(32), "zz", "11".repeat(32), "zz");

        assertThatThrownBy(() -> VoucherDocuments.fromHex(hex, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds proof count");
    }
}
