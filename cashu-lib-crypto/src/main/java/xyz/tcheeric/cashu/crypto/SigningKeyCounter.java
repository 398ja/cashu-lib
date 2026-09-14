package xyz.tcheeric.cashu.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Counts how many <em>distinct</em> public keys have signed a message, for NUT-11 thresholds.
 *
 * <h2>Why distinct keys and not signatures</h2>
 *
 * <p>A NUT-11 {@code n_sigs} threshold counts keys, not signatures. Counting signatures would let
 * one key satisfy an n-of-m by submitting its signature twice — and worse, Schnorr signatures are
 * non-deterministic, so a single key can produce unlimited distinct valid signatures over the
 * same message. A crafted secret repeating a pubkey in its {@code pubkeys} tag is the other way
 * in. Either would be a forged multisig: value locked to "2 of these 3 people" released by one.
 *
 * <h2>Why it lives here</h2>
 *
 * <p>This rule was implemented correctly inside {@code cashu-mint} and had no test at all
 * (AppSec review, issue #265). Deleting the deduplication let one key satisfy a 2-of-2 with the
 * whole suite green.
 *
 * <p>It belongs in {@code cashu-lib} because it is a property of NUT-11 rather than of any one
 * mint: it is built on {@link Schnorr} and on the 33-byte-compressed versus 32-byte-x-only key
 * encoding split that this module already owns. Any other consumer of these primitives would
 * otherwise have to rediscover the rule, and nothing in the primitives themselves hints that it
 * is needed.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/11.md">NUT-11</a>
 */
@Slf4j
public final class SigningKeyCounter {

    /** Length in hex characters of a 33-byte compressed public key. */
    private static final int COMPRESSED_HEX_LENGTH = 66;

    private SigningKeyCounter() {
    }

    /**
     * The number of distinct keys in {@code publicKeys} holding a valid signature over
     * {@code message}.
     *
     * <p>{@code message} is the raw material; it is hashed here, because BIP-340 signs a 32-byte
     * digest and every NUT-11 caller was otherwise repeating the same hash step.
     *
     * @param publicKeys the pathway's keys, compressed (33-byte) or x-only (32-byte) hex
     * @param signatures candidate signatures, hex; order is not significant
     * @param message    the signed material, unhashed
     * @return how many distinct keys signed, never more than {@code publicKeys} holds
     */
    public static int countSigningKeys(List<String> publicKeys, List<String> signatures,
                                       byte[] message) {
        if (publicKeys == null || signatures == null) {
            return 0;
        }
        byte[] hash = hashOrNull(message);
        if (hash == null) {
            return 0;
        }
        Set<String> countedKeys = new HashSet<>();
        int signingKeyCount = 0;
        for (String publicKey : publicKeys) {
            if (publicKey == null || publicKey.isBlank()) {
                continue;
            }
            String xOnly = xCoordinate(publicKey);
            // The deduplication that makes this a key count rather than a signature count. A
            // repeated pubkey -- whether from a crafted secret or an honest mistake -- gets one
            // vote, not two.
            if (!countedKeys.add(xOnly)) {
                continue;
            }
            if (hasValidSignature(xOnly, signatures, hash)) {
                signingKeyCount++;
            }
        }
        return signingKeyCount;
    }

    private static boolean hasValidSignature(String xOnlyKey, List<String> signatures,
                                             byte[] hash) {
        for (String signature : signatures) {
            if (signature == null || signature.isBlank()) {
                continue;
            }
            try {
                if (Schnorr.verify(hash, Hex.decode(xOnlyKey), Hex.decode(signature))) {
                    return true;
                }
            } catch (Exception malformed) {
                // A malformed key or signature is not a match, and not a reason to abandon the
                // other candidates: one bad entry in a witness must not veto a valid one.
                log.warn("Error verifying signature. Continuing...", malformed);
            }
        }
        return false;
    }

    private static byte[] hashOrNull(byte[] message) {
        if (message == null) {
            return null;
        }
        try {
            return Utils.sha256(message);
        } catch (Exception e) {
            log.warn("Error hashing spend data. Rejecting signatures.", e);
            return null;
        }
    }

    /**
     * The lowercase x-coordinate of a public key, stripping a compressed {@code 02}/{@code 03}
     * prefix.
     *
     * <p>NUT-11 carries 33-byte compressed keys while BIP-340 verifies against the 32-byte x-only
     * form, so without this every spec-conformant key would throw inside
     * {@link #hasValidSignature} and silently count as "did not sign". Normalising here also
     * means two spellings of one key -- the compressed and x-only forms -- deduplicate to the
     * same entry rather than counting twice.
     */
    private static String xCoordinate(String publicKeyHex) {
        String hex = publicKeyHex.toLowerCase();
        if (hex.length() == COMPRESSED_HEX_LENGTH && (hex.startsWith("02") || hex.startsWith("03"))) {
            return hex.substring(2);
        }
        return hex;
    }
}
