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
}
