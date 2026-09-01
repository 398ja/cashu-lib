package xyz.tcheeric.cashu.common.nut20;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.crypto.Schnorr;

import java.security.MessageDigest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whether a mint request may issue against a locked quote.
 *
 * <p>Each signature here is produced with a real key rather than pasted in, so a test that passes
 * says the mint accepts what a conforming wallet actually sends, and the rejection cases say what
 * it refuses.
 */
class MintQuoteSignatureTest {

    private static final String QUOTE = "9d745270-1405-46de-b5c5-e2762b4f5e00";
    private static final byte[] PRIVATE_KEY =
            Hex.decode("0000000000000000000000000000000000000000000000000000000000000003");
    private static final String PUBLIC_KEY =
            "02f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9";

    private static BlindedMessage output(int amount, String blinded) {
        BlindedMessage message = new BlindedMessage();
        message.setAmount(amount);
        message.setKeySetId(KeysetId.fromString("009a1f293253e41e"));
        message.setBlindedMessage(PublicKey.fromString(blinded));
        return message;
    }

    private static List<BlindedMessage> outputs() {
        return List.of(
                output(8, "035015e6d7ade60ba8426cefaf1832bbd27257636e44a76b922d78e79b47cb689d"),
                output(2, "0288d7649652d0a83fc9c966c969fb217f15904431e61a44b14999fabc1b5d9ac6"));
    }

    private static String signatureOver(String quoteId, List<BlindedMessage> outputs) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(MintQuoteSignatureMessage.forQuote(quoteId, outputs));
        return Hex.toHexString(Schnorr.sign(hash, PRIVATE_KEY));
    }

    /**
     * Ensures the holder of the quote's key can mint it, which is the whole point of locking.
     */
    @Test
    void shouldAcceptASignatureFromTheKeyTheQuoteIsLockedTo() throws Exception {
        // Arrange
        String signature = signatureOver(QUOTE, outputs());

        // Act
        boolean valid = MintQuoteSignature.isValid(QUOTE, outputs(), PUBLIC_KEY, signature);

        // Assert
        assertThat(valid).isTrue();
    }

    /**
     * Ensures a signature for one quote cannot mint another. Without this, capturing a single
     * signature would unlock every quote the same wallet ever created.
     */
    @Test
    void shouldRejectASignatureMadeForADifferentQuote() throws Exception {
        // Arrange
        String signature = signatureOver("a-different-quote", outputs());

        // Act
        boolean valid = MintQuoteSignature.isValid(QUOTE, outputs(), PUBLIC_KEY, signature);

        // Assert
        assertThat(valid).isFalse();
    }

    /**
     * Ensures the signature is bound to the outputs, so an attacker who captures one cannot
     * redirect the ecash to blinded messages of their own.
     */
    @Test
    void shouldRejectASignatureWhenTheOutputsAreSubstituted() throws Exception {
        // Arrange
        String signature = signatureOver(QUOTE, outputs());
        List<BlindedMessage> attackerOutputs = List.of(
                output(8, "02c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5"),
                output(2, "0288d7649652d0a83fc9c966c969fb217f15904431e61a44b14999fabc1b5d9ac6"));

        // Act
        boolean valid = MintQuoteSignature.isValid(QUOTE, attackerOutputs, PUBLIC_KEY, signature);

        // Assert
        assertThat(valid).isFalse();
    }

    /**
     * Ensures reordering the outputs invalidates the signature, since the message commits to the
     * order they appear in the request.
     */
    @Test
    void shouldRejectASignatureWhenTheOutputsAreReordered() throws Exception {
        // Arrange
        String signature = signatureOver(QUOTE, outputs());
        List<BlindedMessage> reordered = List.of(outputs().get(1), outputs().get(0));

        // Act
        boolean valid = MintQuoteSignature.isValid(QUOTE, reordered, PUBLIC_KEY, signature);

        // Assert
        assertThat(valid).isFalse();
    }

    /**
     * Ensures a signature from an unrelated key is refused, which is the case that would otherwise
     * let anyone holding the quote id mint it.
     */
    @Test
    void shouldRejectASignatureFromAnotherKey() throws Exception {
        // Arrange
        byte[] otherKey =
                Hex.decode("0000000000000000000000000000000000000000000000000000000000000005");
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(MintQuoteSignatureMessage.forQuote(QUOTE, outputs()));
        String signature = Hex.toHexString(Schnorr.sign(hash, otherKey));

        // Act
        boolean valid = MintQuoteSignature.isValid(QUOTE, outputs(), PUBLIC_KEY, signature);

        // Assert
        assertThat(valid).isFalse();
    }

    /**
     * Ensures a malformed signature is refused rather than raising, so a client sending nonsense
     * gets a refusal instead of taking the mint's request handler down.
     */
    @Test
    void shouldRejectAMalformedSignatureWithoutThrowing() {
        // Arrange
        String notASignature = "not-hex";

        // Act
        boolean valid = MintQuoteSignature.isValid(QUOTE, outputs(), PUBLIC_KEY, notASignature);

        // Assert
        assertThat(valid).isFalse();
    }
}
