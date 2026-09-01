package xyz.tcheeric.cashu.common.nut20;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.crypto.Schnorr;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Decides whether a mint request may issue against a locked quote (NUT-20).
 *
 * <p>A quote carrying a public key can only be minted by whoever holds the matching private key.
 * Without that, a quote id is a bearer token: NUT-04 warns that anyone who learns the id of a paid
 * quote can take its ecash, and a quote id travels through logs, webhooks and traces.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/20.md">NUT-20</a>
 */
@Slf4j
public final class MintQuoteSignature {

    /** Length of a compressed secp256k1 key in hex characters. */
    private static final int COMPRESSED_KEY_HEX_LENGTH = 66;

    /** Where the x-only key starts in the compressed form, past the parity prefix byte. */
    private static final int X_ONLY_KEY_HEX_OFFSET = 2;

    private MintQuoteSignature() {
    }

    /**
     * Verifies a BIP-340 signature over the quote id and outputs.
     *
     * @return whether the signature was made by the key the quote is locked to
     */
    public static boolean isValid(@NonNull String quoteId,
                                  @NonNull List<BlindedMessage> outputs,
                                  @NonNull String pubkey,
                                  @NonNull String signature) {
        try {
            byte[] hash = sha256(MintQuoteSignatureMessage.forQuote(quoteId, outputs));
            return Schnorr.verify(hash, Hex.decode(xOnly(pubkey)), Hex.decode(signature));
        } catch (RuntimeException | NoSuchAlgorithmException e) {
            // A malformed key or signature is an invalid signature, not a mint failure: the caller
            // refuses to issue either way, and the difference is the client's to fix.
            log.warn("nut20 signature_rejected quote={} reason={}", quoteId, e.getMessage());
            return false;
        }
    }

    /**
     * Strips the parity prefix from a compressed key.
     *
     * <p>BIP-340 verifies against the 32-byte x-only key while NUT-20 carries the 33-byte
     * compressed form. Passing the compressed form straight through makes every spec-conformant key
     * fail verification, which would look like a wrong signature rather than a wrong encoding.
     */
    private static String xOnly(String pubkey) {
        return pubkey.length() == COMPRESSED_KEY_HEX_LENGTH
                ? pubkey.substring(X_ONLY_KEY_HEX_OFFSET)
                : pubkey;
    }

    private static byte[] sha256(byte[] message) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(message);
    }
}
