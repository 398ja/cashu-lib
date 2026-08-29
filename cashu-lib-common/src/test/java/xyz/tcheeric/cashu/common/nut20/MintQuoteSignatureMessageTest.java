package xyz.tcheeric.cashu.common.nut20;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.PublicKey;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The bytes a wallet signs to mint a locked quote.
 *
 * <p>These assert the encoding rather than a round trip, because a round trip passes for any
 * self-consistent encoding. What matters is that our bytes are the bytes every other
 * implementation produces for the same quote.
 */
class MintQuoteSignatureMessageTest {

    private static final String QUOTE = "9d745270-1405-46de-b5c5-e2762b4f5e00";
    private static final String B_ =
            "035015e6d7ade60ba8426cefaf1832bbd27257636e44a76b922d78e79b47cb689d";

    private static BlindedMessage output(int amount) {
        BlindedMessage message = new BlindedMessage();
        message.setAmount(amount);
        message.setKeySetId(KeysetId.fromString("009a1f293253e41e"));
        message.setBlindedMessage(PublicKey.fromString(B_));
        return message;
    }

    /**
     * Ensures the message opens with the domain tag written as raw ASCII and not length-prefixed,
     * which is what stops a signature made here being valid in another context.
     */
    @Test
    void shouldStartWithTheDomainSeparationTag() {
        // Arrange
        byte[] tag = "Cashu_MintQuoteSig_v1".getBytes(StandardCharsets.US_ASCII);

        // Act
        byte[] message = MintQuoteSignatureMessage.forQuote(QUOTE, List.of(output(8)));

        // Assert
        assertThat(message).startsWith(tag);
    }

    /**
     * Ensures reordering the outputs changes the message, so a captured signature cannot be
     * replayed against the same outputs in a different order.
     */
    @Test
    void shouldDifferWhenOutputsAreReordered() {
        // Arrange
        List<BlindedMessage> ordered = List.of(output(8), output(2));
        List<BlindedMessage> reordered = List.of(output(2), output(8));

        // Act
        byte[] first = MintQuoteSignatureMessage.forQuote(QUOTE, ordered);
        byte[] second = MintQuoteSignatureMessage.forQuote(QUOTE, reordered);

        // Assert
        assertThat(first).isNotEqualTo(second);
    }

    /**
     * Ensures the message is bound to its quote, so a signature for one quote cannot mint another.
     */
    @Test
    void shouldDifferWhenTheQuoteDiffers() {
        // Arrange
        List<BlindedMessage> outputs = List.of(output(8));

        // Act
        byte[] first = MintQuoteSignatureMessage.forQuote(QUOTE, outputs);
        byte[] second = MintQuoteSignatureMessage.forQuote("a-different-quote", outputs);

        // Assert
        assertThat(first).isNotEqualTo(second);
    }

    /**
     * Ensures an amount of zero contributes an empty byte array, as the spec's canonical minimal
     * big-endian encoding requires, so its length prefix is zero.
     */
    @Test
    void shouldEncodeAZeroAmountAsNoBytes() {
        // Arrange
        int tagLength = "Cashu_MintQuoteSig_v1".length();
        int quoteLength = Integer.BYTES + QUOTE.length();

        // Act
        byte[] message = MintQuoteSignatureMessage.forQuote(QUOTE, List.of(output(0)));

        // Assert
        int amountLengthAt = tagLength + quoteLength;
        assertThat(message[amountLengthAt + 3]).isZero();
    }

    /**
     * Ensures 128 encodes as one byte rather than two.
     *
     * <p>Java's two's-complement encoding prepends a sign byte whenever the leading bit is set, so
     * an amount of 128 would otherwise be a byte longer than every other implementation produces,
     * and every signature over it would fail to verify elsewhere.
     */
    @Test
    void shouldEncodeAnAmountWithAHighLeadingBitWithoutASignByte() {
        // Arrange
        int tagLength = "Cashu_MintQuoteSig_v1".length();
        int quoteLength = Integer.BYTES + QUOTE.length();

        // Act
        byte[] message = MintQuoteSignatureMessage.forQuote(QUOTE, List.of(output(128)));

        // Assert
        int amountLengthAt = tagLength + quoteLength;
        assertThat(message[amountLengthAt + 3]).isEqualTo((byte) 1);
        assertThat(message[amountLengthAt + 4]).isEqualTo((byte) 0x80);
    }

    /**
     * Ensures 256 encodes as the two bytes 0x01 0x00, pinning the big-endian order.
     */
    @Test
    void shouldEncodeALargerAmountBigEndian() {
        // Arrange
        int tagLength = "Cashu_MintQuoteSig_v1".length();
        int quoteLength = Integer.BYTES + QUOTE.length();

        // Act
        byte[] message = MintQuoteSignatureMessage.forQuote(QUOTE, List.of(output(256)));

        // Assert
        int amountLengthAt = tagLength + quoteLength;
        assertThat(message[amountLengthAt + 3]).isEqualTo((byte) 2);
        assertThat(message[amountLengthAt + 4]).isEqualTo((byte) 0x01);
        assertThat(message[amountLengthAt + 5]).isZero();
    }
}
