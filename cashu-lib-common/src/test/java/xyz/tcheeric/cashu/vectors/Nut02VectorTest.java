package xyz.tcheeric.cashu.vectors;

import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.tcheeric.cashu.common.Keys;
import xyz.tcheeric.cashu.common.KeysetIdVersion;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.crypto.util.KeySetDerivation;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * NUT-02 vectors: deriving a keyset id from its public keys.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/tests/02-tests.md">NUT-02 test vectors</a>
 */
class Nut02VectorTest {

    private static final VectorDocument VECTORS = VectorDocument.load("02-tests.md");

    private static final Pattern KEYSET_ID_IN_PROSE = Pattern.compile("[Kk]eyset id: `([0-9a-f]+)`");

    static Stream<KeysetVector> keysetVectors() {
        List<String> ids = keysetIds();
        return IntStream.range(0, ids.size())
                .mapToObj(index -> new KeysetVector(ids.get(index), VECTORS.block("json", index).text()));
    }

    /**
     * Ensures a version 1 keyset's id is derived from its public keys as published.
     */
    @ParameterizedTest(name = "keyset id {0}")
    @MethodSource("keysetVectors")
    void shouldDerivePublishedIdWhenKeysetIsVersionOne(KeysetVector vector) {
        // Arrange
        assumeThat(KeysetIdVersion.of(vector.getKeysetId()))
                .as("Version 2 keyset ids use a derivation this library does not implement (issue #247)")
                .isEqualTo(KeysetIdVersion.V1);
        Keys keys = parseKeys(vector.getKeysJson());

        // Act
        String derivedId = KeySetDerivation.getId(keys.values());

        // Assert
        assertThat(derivedId).isEqualTo(vector.getKeysetId());
    }

    private static List<String> keysetIds() {
        Matcher matcher = KEYSET_ID_IN_PROSE.matcher(VECTORS.getMarkdown());
        return matcher.results().map(result -> result.group(1)).toList();
    }

    @SneakyThrows
    private static Keys parseKeys(String keysJson) {
        return JsonUtils.JSON_MAPPER.readValue(keysJson, Keys.class);
    }

    /**
     * One published keyset id with the keys it is derived from.
     */
    @Value
    static class KeysetVector {

        String keysetId;
        String keysJson;

        @Override
        public String toString() {
            return keysetId;
        }
    }
}
