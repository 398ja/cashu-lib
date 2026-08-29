package xyz.tcheeric.cashu.common.nut20;

import lombok.NonNull;
import xyz.tcheeric.cashu.common.BlindedMessage;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The message a wallet signs to prove it may mint a locked quote (NUT-20).
 *
 * <p>The signature commits to the quote id <em>and</em> every output, in request order. Committing
 * to the quote alone would leave a captured signature usable with substituted outputs, which is the
 * same value redirected to someone else's blinded messages, so the outputs are part of the message
 * rather than an afterthought.
 *
 * <p>The encoding is fixed by the spec and is deliberately unambiguous: every variable-length part
 * is preceded by its 32-bit big-endian length, so no combination of quote id and outputs can encode
 * to the same bytes as a different combination.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/20.md">NUT-20</a>
 */
public final class MintQuoteSignatureMessage {

    /**
     * The domain-separation tag, written as raw ASCII and deliberately not length-prefixed.
     *
     * <p>It keeps a signature made for a mint quote from ever being valid in another context that
     * happens to sign a similar-looking byte string.
     */
    private static final byte[] DOMAIN_TAG = "Cashu_MintQuoteSig_v1".getBytes(StandardCharsets.US_ASCII);

    private MintQuoteSignatureMessage() {
    }

    /**
     * Builds the message to sign for a quote and its outputs.
     *
     * @param quoteId the quote id exactly as the mint issued it
     * @param outputs the blinded messages, in the order they appear in the request
     */
    public static byte[] forQuote(@NonNull String quoteId, @NonNull List<BlindedMessage> outputs) {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        message.writeBytes(DOMAIN_TAG);
        writeLengthPrefixed(message, quoteId.getBytes(StandardCharsets.UTF_8));
        for (BlindedMessage output : outputs) {
            writeLengthPrefixed(message, minimalBigEndian(output.getAmount()));
            writeLengthPrefixed(message, output.getBlindedMessage().getBytes());
        }
        return message.toByteArray();
    }

    private static void writeLengthPrefixed(ByteArrayOutputStream message, byte[] value) {
        message.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(value.length).array());
        message.writeBytes(value);
    }

    /**
     * Renders an amount as canonical minimal big-endian bytes, so zero is empty, 1 is one byte and
     * 256 is two.
     *
     * <p>{@link BigInteger#toByteArray()} prepends a sign byte when the leading bit is set, which
     * would make 128 two bytes rather than one and put this mint's message a byte away from every
     * other implementation's.
     */
    private static byte[] minimalBigEndian(int amount) {
        if (amount == 0) {
            return new byte[0];
        }
        byte[] twosComplement = BigInteger.valueOf(amount).toByteArray();
        if (twosComplement[0] != 0) {
            return twosComplement;
        }
        byte[] minimal = new byte[twosComplement.length - 1];
        System.arraycopy(twosComplement, 1, minimal, 0, minimal.length);
        return minimal;
    }
}
