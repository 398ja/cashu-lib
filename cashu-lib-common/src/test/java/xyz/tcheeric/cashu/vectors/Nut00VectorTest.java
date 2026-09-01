package xyz.tcheeric.cashu.vectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.TokenV3;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.common.TokenV4CborEncoder;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NUT-00 vectors: hash-to-curve, blinded messages, blind signatures and token serialization.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/tests/00-tests.md">NUT-00 test vectors</a>
 */
class Nut00VectorTest {

    private static final VectorDocument VECTORS = VectorDocument.load("00-tests.md");

    private static final int HASH_TO_CURVE_BLOCK = 0;
    private static final int BLINDED_MESSAGE_BLOCK = 1;
    private static final int BLIND_SIGNATURE_BLOCK = 2;
    private static final int MALFORMED_TOKEN_V3_BLOCK = 3;
    private static final int WELL_FORMED_TOKEN_V3_BLOCK = 4;
    private static final int PADDED_AND_UNPADDED_TOKEN_V3_BLOCK = 5;

    /** Hex of the ASCII "cashuB" prefix that opens the raw binary TokenV4 vector. */
    private static final String RAW_BINARY_PREFIX = "6372617742";

    static Stream<Arguments> hashToCurveVectors() {
        return recordsOf(HASH_TO_CURVE_BLOCK, "Message")
                .map(vector -> Arguments.of(vector.get("Message"), vector.get("Point")));
    }

    static Stream<Arguments> blindedMessageVectors() {
        return recordsOf(BLINDED_MESSAGE_BLOCK, "x")
                .map(vector -> Arguments.of(vector.get("x"), vector.get("r"), vector.get("B_")));
    }

    static Stream<Arguments> blindSignatureVectors() {
        return recordsOf(BLIND_SIGNATURE_BLOCK, "mint private key")
                .map(vector -> Arguments.of(vector.get("mint private key"), vector.get("B_"), vector.get("C_")));
    }

    static Stream<String> malformedTokenV3Vectors() {
        return VECTORS.block("shell", MALFORMED_TOKEN_V3_BLOCK).values().stream();
    }

    static Stream<String> tokenV3Vectors() {
        return Stream.concat(
                VECTORS.block("shell", WELL_FORMED_TOKEN_V3_BLOCK).values().stream(),
                VECTORS.block("shell", PADDED_AND_UNPADDED_TOKEN_V3_BLOCK).values().stream());
    }

    /**
     * Ensures hash_to_curve maps each vector message to the published curve point.
     */
    @ParameterizedTest(name = "message {0}")
    @MethodSource("hashToCurveVectors")
    void shouldMapMessageToPublishedPointWhenHashingToCurve(String message, String expectedPoint) {
        // Arrange
        byte[] messageBytes = Utils.hexStringToBytes(message);

        // Act
        String point = BDHKEUtils.pointToHex(BDHKEUtils.hashToCurve(messageBytes));

        // Assert
        assertThat(point).isEqualTo(expectedPoint);
    }

    /**
     * Ensures blinding a secret with the vector blinding factor yields the published B_.
     */
    @ParameterizedTest(name = "secret {0}")
    @MethodSource("blindedMessageVectors")
    void shouldProducePublishedBlindedMessageWhenBlindingWithVectorFactor(String secret, String r, String expectedB) {
        // Arrange
        byte[] secretBytes = Utils.hexStringToBytes(secret);
        byte[] blindingFactor = Utils.hexStringToBytes(r);

        // Act
        byte[] blindedMessage = BDHKEUtils.blindMessage(secretBytes, blindingFactor);

        // Assert
        assertThat(compress(blindedMessage)).isEqualTo(expectedB);
    }

    /**
     * Ensures signing a blinded message with the mint key yields the published C_.
     */
    @ParameterizedTest(name = "mint key {0}")
    @MethodSource("blindSignatureVectors")
    void shouldProducePublishedBlindSignatureWhenSigningVectorMessage(String mintKey, String blindedMessage, String expectedC) {
        // Arrange
        byte[] key = Utils.hexStringToBytes(mintKey);
        byte[] messagePoint = Utils.hexStringToBytes(blindedMessage);

        // Act
        byte[] blindSignature = BDHKEUtils.signBlindedMessage(messagePoint, key);

        // Assert
        assertThat(Utils.bytesToHexString(blindSignature)).isEqualTo(expectedC);
    }

    /**
     * Ensures a v3 token round-trips to the exact serialization published in the vectors.
     */
    @Test
    void shouldReproducePublishedSerializationWhenRoundTrippingTokenV3() {
        // Arrange
        String serialized = VECTORS.block("", 0).text();

        // Act
        String reserialized = TokenV3.deserialize(serialized).serialize(false);

        // Assert
        assertThat(reserialized).isEqualTo(serialized);
    }

    /**
     * Ensures every well-formed v3 token in the vectors deserializes, padded or not.
     */
    @ParameterizedTest(name = "token {index}")
    @MethodSource("tokenV3Vectors")
    void shouldDeserializeWhenTokenV3IsWellFormed(String serialized) {
        // Act
        TokenV3<?> token = TokenV3.deserialize(serialized);

        // Assert
        assertThat(token.getMintProofs()).isNotEmpty();
    }

    /**
     * Ensures malformed v3 prefixes are rejected rather than silently accepted.
     */
    @ParameterizedTest(name = "token {index}")
    @MethodSource("malformedTokenV3Vectors")
    void shouldRejectWhenTokenV3PrefixIsMalformed(String serialized) {
        // Act / Assert
        assertThatThrownBy(() -> TokenV3.deserialize(serialized))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Ensures a single-keyset v4 token round-trips to the published serialization.
     */
    @Test
    void shouldReproducePublishedSerializationWhenRoundTrippingSingleKeysetTokenV4() {
        // Arrange
        String serialized = VECTORS.block("", 1).text();

        // Act
        String reserialized = TokenV4.deserialize(serialized).serialize(false);

        // Assert
        assertThat(reserialized).isEqualTo(stripPadding(serialized));
    }

    /**
     * Ensures a multi-keyset v4 token round-trips to the published serialization.
     */
    @Test
    void shouldReproducePublishedSerializationWhenRoundTrippingMultiKeysetTokenV4() {
        // Arrange
        String serialized = VECTORS.block("", 2).text();

        // Act
        String reserialized = TokenV4.deserialize(serialized).serialize(false);

        // Assert
        assertThat(reserialized).isEqualTo(stripPadding(serialized));
    }

    /**
     * Ensures a v4 token encodes to the CBOR body of the published raw binary serialization.
     */
    @Test
    void shouldReproducePublishedCborBodyWhenEncodingTokenV4() {
        // Arrange
        String serialized = VECTORS.block("", 1).text();
        String expectedCborHex = rawBinaryVector().substring(RAW_BINARY_PREFIX.length());

        // Act
        byte[] cbor = TokenV4CborEncoder.encode(TokenV4.deserialize(serialized));

        // Assert
        assertThat(Utils.bytesToHexString(cbor)).isEqualTo(expectedCborHex);
    }

    private static Stream<Map<String, String>> recordsOf(int shellBlockIndex, String recordKey) {
        List<Map<String, String>> records = VECTORS.block("shell", shellBlockIndex).records(recordKey);
        return records.stream();
    }

    private static String rawBinaryVector() {
        return VECTORS.inlineHexLiteral();
    }

    private static String stripPadding(String serialized) {
        return serialized.replace("=", "");
    }

    private static String compress(byte[] uncompressedXy) {
        return PublicKey.fromBytes(uncompressedXy).toString();
    }
}
