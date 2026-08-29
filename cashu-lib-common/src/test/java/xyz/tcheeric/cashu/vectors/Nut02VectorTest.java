package xyz.tcheeric.cashu.vectors;

import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.tcheeric.cashu.common.Keys;
import xyz.tcheeric.cashu.common.KeysetIdVersion;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.crypto.util.KeySetDerivation;
import xyz.tcheeric.cashu.crypto.util.KeySetIdV2Derivation;

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
                .as("Version 2 ids are derived over metadata too, covered by the v2 test below")
                .isEqualTo(KeysetIdVersion.V1);
        Keys keys = parseKeys(vector.getKeysJson());

        // Act
        String derivedId = KeySetDerivation.getId(keys.values());

        // Assert
        assertThat(derivedId).isEqualTo(vector.getKeysetId());
    }

    /**
     * Ensures a version 2 keyset's id is derived from its keys and its metadata as published.
     *
     * <p>Vector 2 is the one that matters most: its {@code input_fee_ppk} is zero, which NUT-02
     * says MUST be omitted from the preimage rather than written as {@code 0}. Writing it would
     * give every fee-free keyset an id no other implementation agrees with.
     */
    @ParameterizedTest(name = "keyset id {0}")
    @MethodSource("versionTwoVectors")
    void shouldDerivePublishedIdWhenKeysetIsVersionTwo(KeysetV2Vector vector) {
        // Arrange
        Keys keys = parseKeys(vector.getKeysJson());

        // Act
        String derivedId = KeySetIdV2Derivation.getId(
                keys.values(), vector.getUnit(), vector.getInputFeePpk(), vector.getFinalExpiry());

        // Assert
        assertThat(derivedId).isEqualTo(vector.getKeysetId());
    }

    static Stream<KeysetV2Vector> versionTwoVectors() {
        List<String> ids = keysetIds();
        List<String> v2Ids = ids.stream()
                .filter(id -> KeysetIdVersion.of(id) == KeysetIdVersion.V2)
                .toList();
        // The JSON blocks appear in document order, so the v2 blocks are the trailing ones.
        int firstV2Block = ids.size() - v2Ids.size();
        return IntStream.range(0, v2Ids.size())
                .mapToObj(index -> {
                    String id = v2Ids.get(index);
                    return new KeysetV2Vector(
                            id,
                            metadata(id, "Unit").orElseThrow(),
                            metadata(id, "Input fee ppk").map(Integer::valueOf).orElse(null),
                            metadata(id, "Final expiry").map(Long::valueOf).orElse(null),
                            VECTORS.block("json", firstV2Block + index).text());
                });
    }

    /**
     * Reads a {@code - Label: `value`} line from the prose describing one vector.
     *
     * <p>Absent is a real answer rather than an error: vector 3 publishes no final expiry, and a
     * keyset without one is exactly the case NUT-02 says to omit from the preimage.
     */
    private static java.util.Optional<String> metadata(String keysetId, String label) {
        Matcher section = Pattern.compile(
                        Pattern.quote(keysetId) + "`(.*?)(?=\\n### |\\n## |$)", Pattern.DOTALL)
                .matcher(VECTORS.getMarkdown());
        if (!section.find()) {
            return java.util.Optional.empty();
        }
        Matcher value = Pattern.compile("- " + Pattern.quote(label) + ": `([^`]+)`")
                .matcher(section.group(1));
        return value.find() ? java.util.Optional.of(value.group(1)) : java.util.Optional.empty();
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

    /** One published version 2 keyset id with the keys and metadata it is derived from. */
    @Value
    static class KeysetV2Vector {

        String keysetId;
        String unit;
        Integer inputFeePpk;
        Long finalExpiry;
        String keysJson;

        @Override
        public String toString() {
            return keysetId;
        }
    }
}
