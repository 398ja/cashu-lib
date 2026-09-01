package xyz.tcheeric.cashu.vectors;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.nut20.MintQuoteSignature;
import xyz.tcheeric.cashu.common.nut20.MintQuoteSignatureMessage;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NUT-20 vectors: the signature that authorises minting a locked quote.
 *
 * <p>The published {@code msg_to_sign} is the valuable part. Our own tests can only show the
 * encoding is self-consistent; this shows it is the same encoding every other implementation
 * produces, byte for byte, which is what decides whether a wallet's signature verifies here.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/tests/20-test.md">NUT-20 test vectors</a>
 */
class Nut20VectorTest {

    private static final VectorDocument VECTORS = VectorDocument.load("20-test.md");

    private static final int MINT_REQUEST_BLOCK = 2;

    private static final String QUOTE_PUBKEY =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    /**
     * Ensures the message we build for a quote and its outputs is byte-identical to the published
     * one. An encoding that differs by a single byte produces a signature no other implementation
     * accepts, and the failure would otherwise only surface against a real third-party wallet.
     */
    @Test
    void shouldBuildPublishedMessageToSign() {
        // Arrange
        JsonNode request = mintRequest();

        // Act
        byte[] message = MintQuoteSignatureMessage.forQuote(
                request.get("quote").asText(), outputs(request));

        // Assert
        assertThat(Utils.bytesToHexString(message)).isEqualTo(publishedValue("msg_to_sign"));
    }

    /**
     * Ensures the hash actually signed matches the published one, which pins the SHA-256 step as
     * well as the message it runs over.
     */
    @Test
    @SneakyThrows
    void shouldHashTheMessageToThePublishedDigest() {
        // Arrange
        JsonNode request = mintRequest();
        byte[] message = MintQuoteSignatureMessage.forQuote(
                request.get("quote").asText(), outputs(request));

        // Act
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(message);

        // Assert
        assertThat(Utils.bytesToHexString(hash)).isEqualTo(publishedValue("sha256(msg_to_sign)"));
    }

    /**
     * Ensures the published signature verifies against the quote's public key, which is the whole
     * decision the mint makes before issuing against a locked quote.
     */
    @Test
    void shouldAcceptThePublishedSignature() {
        // Arrange
        JsonNode request = mintRequest();

        // Act
        boolean valid = MintQuoteSignature.isValid(
                request.get("quote").asText(),
                outputs(request),
                QUOTE_PUBKEY,
                request.get("signature").asText());

        // Assert
        assertThat(valid).isTrue();
    }

    @SneakyThrows
    private static JsonNode mintRequest() {
        return JsonUtils.JSON_MAPPER.readTree(VECTORS.block("json", MINT_REQUEST_BLOCK).text());
    }

    private static List<BlindedMessage> outputs(JsonNode request) {
        List<BlindedMessage> outputs = new ArrayList<>();
        for (JsonNode output : request.get("outputs")) {
            BlindedMessage message = new BlindedMessage();
            message.setAmount(output.get("amount").asInt());
            message.setKeySetId(KeysetId.fromString(output.get("id").asText()));
            message.setBlindedMessage(PublicKey.fromString(output.get("B_").asText()));
            outputs.add(message);
        }
        return outputs;
    }

    /** Reads a {@code name = value} line from the vector's plain code block. */
    private static String publishedValue(String name) {
        Matcher matcher = Pattern.compile(Pattern.quote(name) + " = ([0-9a-f]+)")
                .matcher(VECTORS.getMarkdown());
        if (!matcher.find()) {
            throw new IllegalStateException("vectors publish no " + name);
        }
        return matcher.group(1);
    }
}
