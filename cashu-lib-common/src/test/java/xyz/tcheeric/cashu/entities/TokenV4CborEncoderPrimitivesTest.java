package xyz.tcheeric.cashu.entities;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4CborEncoder;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.assertThat;

class TokenV4CborEncoderPrimitivesTest {
    private static byte[] cap(java.util.function.Consumer<ByteArrayOutputStream> w) {
        var o = new ByteArrayOutputStream(); w.accept(o); return o.toByteArray();
    }
    @Test void uintMinimalHeaders() {
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 0))).containsExactly(0x00);
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 23))).containsExactly(0x17);
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 24))).containsExactly(0x18, 0x18);
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 255))).containsExactly(0x18, 0xFF);
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 256))).containsExactly(0x19, 0x01, 0x00);
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 1000000))).containsExactly(0x1A, 0x00, 0x0F, 0x42, 0x40);
    }
    @Test void byteStringHeaderAndBody() {
        // h'01020304' -> 0x44 01 02 03 04
        assertThat(cap(o -> TokenV4CborEncoder.writeByteString(o, new byte[]{1,2,3,4})))
            .containsExactly(0x44, 0x01, 0x02, 0x03, 0x04);
    }
    @Test void textStringHeaderAndBody() {
        // "sat" -> 0x63 73 61 74
        assertThat(cap(o -> TokenV4CborEncoder.writeTextString(o, "sat")))
            .containsExactly(0x63, 0x73, 0x61, 0x74);
    }
    @Test void mapAndArrayHeadersAreDefiniteLength() {
        assertThat(cap(o -> TokenV4CborEncoder.writeMapHeader(o, 4))).containsExactly(0xA4);   // NOT 0xBF
        assertThat(cap(o -> TokenV4CborEncoder.writeArrayHeader(o, 2))).containsExactly(0x82); // NOT 0x9F
    }
}
