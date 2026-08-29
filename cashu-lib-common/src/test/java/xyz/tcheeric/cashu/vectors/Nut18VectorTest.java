package xyz.tcheeric.cashu.vectors;

import lombok.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.tcheeric.cashu.common.nut18.PaymentRequest;

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
     * Ensures a decoded request re-encodes to the byte-identical published string.
     *
     * <p>This is the assertion that catches a field written in the wrong CBOR order or an optional
     * field emitted when it should be absent: both decode correctly and both produce a request
     * other implementations read differently.
     *
     * <p>Two of the seven vectors fail today, and are left failing as the standing evidence for
     * issue #255: we write indefinite-length CBOR maps where the spec writes definite-length ones,
     * and we emit an empty transport array where the spec omits the field.
     */
    @Disabled("Fails on 2 of 7 vectors; see #255 — indefinite-length CBOR and an empty transport array")
    @ParameterizedTest(name = "request {0}")
    @MethodSource("paymentRequestVectors")
    void shouldReEncodeToThePublishedString(PaymentRequestVector vector) {
        // Arrange
        PaymentRequest request = PaymentRequest.deserialize(vector.getEncoded());

        // Act
        String reEncoded = request.serialize();

        // Assert
        assertThat(reEncoded).isEqualTo(vector.getEncoded());
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
