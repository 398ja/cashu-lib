package xyz.tcheeric.cashu.vectors;

import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.tcheeric.cashu.common.Keys;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NUT-01 vectors: which keysets a wallet must accept and which it must reject.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/tests/01-tests.md">NUT-01 test vectors</a>
 */
class Nut01VectorTest {

    private static final VectorDocument VECTORS = VectorDocument.load("01-tests.md");

    private static final int FIRST_INVALID_KEYSET_BLOCK = 0;
    private static final int INVALID_KEYSET_COUNT = 2;
    private static final int VALID_KEYSET_COUNT = 2;

    static Stream<String> invalidKeysets() {
        return blockRange(FIRST_INVALID_KEYSET_BLOCK, INVALID_KEYSET_COUNT);
    }

    static Stream<String> validKeysets() {
        return blockRange(FIRST_INVALID_KEYSET_BLOCK + INVALID_KEYSET_COUNT, VALID_KEYSET_COUNT);
    }

    /**
     * Ensures a keyset carrying a malformed public key is rejected rather than parsed.
     */
    @ParameterizedTest(name = "keyset {index}")
    @MethodSource("invalidKeysets")
    void shouldRejectWhenKeysetContainsMalformedPublicKey(String keysetJson) {
        // Act / Assert
        assertThatThrownBy(() -> parseKeys(keysetJson))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Ensures a well-formed keyset parses, including amounts above {@code Long.MAX_VALUE}.
     */
    @ParameterizedTest(name = "keyset {index}")
    @MethodSource("validKeysets")
    @SneakyThrows
    void shouldParseWhenKeysetIsWellFormed(String keysetJson) {
        // Act
        Keys keys = parseKeys(keysetJson);

        // Assert
        assertThat(keys.getValues()).hasSize(JsonUtils.JSON_MAPPER.readTree(keysetJson).size());
    }

    @SneakyThrows
    private static Keys parseKeys(String keysetJson) {
        return JsonUtils.JSON_MAPPER.readValue(keysetJson, Keys.class);
    }

    private static Stream<String> blockRange(int firstIndex, int count) {
        return Stream.iterate(firstIndex, index -> index + 1)
                .limit(count)
                .map(index -> VECTORS.block("json", index).text());
    }
}
