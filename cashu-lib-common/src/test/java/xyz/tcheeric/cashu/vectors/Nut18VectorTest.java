package xyz.tcheeric.cashu.vectors;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.tcheeric.cashu.common.nut18.PaymentRequest;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NUT-18 vectors: payment requests and their published encodings.
 *
 * <p>Decoding is asserted against the published strings rather than round-tripped through our own
 * encoder, because a round trip passes for any self-consistent format. A payment request that this
 * library alone can read is a payment request no one can pay.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/tests/18-tests.md">NUT-18 test vectors</a>
 */
class Nut18VectorTest {

    private static final VectorDocument VECTORS = VectorDocument.load("18-tests.md");

    private static final Pattern ENCODED = Pattern.compile("```\\n(creqA[A-Za-z0-9_-]+)\\n```");

    static Stream<PaymentRequestVector> paymentRequestVectors() {
        List<String> encodings = encodings();
        return IntStream.range(0, encodings.size())
                .mapToObj(index -> new PaymentRequestVector(
                        VECTORS.block("json", index).text(), encodings.get(index)));
    }

    /**
     * Ensures every published encoding decodes, which is what a wallet does when it is handed a
     * payment request by someone else.
     */
    @ParameterizedTest(name = "request {0}")
    @MethodSource("paymentRequestVectors")
    void shouldDecodePublishedPaymentRequest(PaymentRequestVector vector) {
        // Act
        PaymentRequest request = PaymentRequest.deserialize(vector.getEncoded());

        // Assert
        assertThat(request).isNotNull();
    }

    /**
     * Ensures a re-encoded request is the same CBOR the vector published: definite-length, with
     * exactly the fields the spec defines.
     *
     * <p>Compared as a decoded structure rather than as a string, because NUT-18 fixes no key
     * order and the published vectors do not agree on one: the first lists {@code t} first while
     * the rest follow the field order of the specification's own JSON. Comparing bytes would
     * assert a convention the spec does not state.
     *
     * <p>What it does still catch is everything that made this fail before: an indefinite-length
     * map, a field emitted that should be absent, or a value encoded at the wrong width, since all
     * of those survive decoding into a structure that differs.
     */
    @ParameterizedTest(name = "request {0}")
    @MethodSource("paymentRequestVectors")
    @SneakyThrows
    void shouldReEncodeToTheSameCborStructure(PaymentRequestVector vector) {
        // Arrange
        PaymentRequest request = PaymentRequest.deserialize(vector.getEncoded());

        // Act
        String reEncoded = request.serialize();

        // Assert
        assertThat(cborTree(reEncoded)).isEqualTo(cborTree(vector.getEncoded()));
    }

    /**
     * Ensures the encoding uses definite-length CBOR maps, which is what issue #255 was about and
     * what the structural comparison above cannot see.
     */
    @ParameterizedTest(name = "request {0}")
    @MethodSource("paymentRequestVectors")
    void shouldEncodeWithDefiniteLengthMaps(PaymentRequestVector vector) {
        // Arrange
        PaymentRequest request = PaymentRequest.deserialize(vector.getEncoded());

        // Act
        byte[] cbor = decodeBase64(request.serialize());

        // Assert: major type 5 with a count, never 0xbf (indefinite) and never a 0xff break.
        assertThat(cbor[0] & 0xff).isNotEqualTo(0xbf);
        assertThat(cbor[0] & 0xe0).isEqualTo(0xa0);
        assertThat(cbor[cbor.length - 1] & 0xff).isNotEqualTo(0xff);
    }

    @SneakyThrows
    private static JsonNode cborTree(String encoded) {
        return JsonUtils.CBOR_MAPPER.readTree(decodeBase64(encoded));
    }

    private static byte[] decodeBase64(String encoded) {
        String payload = encoded.substring(PaymentRequest.REQUEST_PREFIX.length()
                + PaymentRequest.VERSION_CODE.length());
        return Base64.getUrlDecoder().decode(payload);
    }

    private static List<String> encodings() {
        Matcher matcher = ENCODED.matcher(VECTORS.getMarkdown());
        return matcher.results().map(result -> result.group(1)).toList();
    }

    /** One published payment request with the encoding it serializes to. */
    @Value
    static class PaymentRequestVector {

        String json;
        String encoded;

        @Override
        public String toString() {
            return encoded.length() > 24 ? encoded.substring(0, 24) + "..." : encoded;
        }
    }
}
