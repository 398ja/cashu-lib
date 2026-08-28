package xyz.tcheeric.cashu.crypto;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.ThreadSafe;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import xyz.tcheeric.cashu.crypto.exception.CashuCryptoException;
import xyz.tcheeric.cashu.crypto.exception.InvalidKeyException;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Discrete Log Equality (DLEQ) proof utilities for NUT-12.
 *
 * <p>DLEQ proofs demonstrate that the mint used the same private key for creating
 * its public key and signing blinded messages. This prevents the mint from using
 * different keys for different users, which could enable tracking.
 *
 * <h2>Protocol Flow</h2>
 * <ol>
 *   <li>Mint generates proof: {@link #generateProof} creates (e, s) scalars</li>
 *   <li>Alice verifies: {@link #verifyProof} checks the proof against B', C', A</li>
 *   <li>Carol verifies with r: {@link #verifyProofWithBlindingFactor} reconstructs
 *       B' and C' from the unblinded signature and blinding factor</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The static {@code SecureRandom} instance is
 * internally synchronized and safe for concurrent access.
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><b>Proof Verification:</b> Wallets MUST verify DLEQ proofs before accepting
 *       blind signatures. Accepting unverified signatures allows a malicious mint
 *       to provide invalid proofs that could compromise privacy.</li>
 *   <li><b>Nonce Security:</b> Proof generation uses cryptographically secure random
 *       nonces. Nonce reuse would leak the private key.</li>
 *   <li><b>Exception Safety:</b> Invalid private key ranges are rejected with
 *       exceptions that do not reveal the key value.</li>
 *   <li><b>Timing Attacks:</b> Verification timing may vary based on inputs.
 *       For high-security applications, consider additional countermeasures.</li>
 * </ul>
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/12.md">NUT-12: DLEQ Proofs</a>
 * @see BDHKEUtils
 */
@Slf4j
@ThreadSafe
public final class DLEQUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private DLEQUtils() {
    }

    /**
     * Generates a DLEQ proof for a blind signature.
     *
     * <p>The mint calls this when creating a BlindSignature to prove it used
     * the same private key for both the public key and the signature.
     *
     * @param privateKey The mint's private key (a)
     * @param blindedMessage The blinded message point B'
     * @param blindSignature The blind signature point C' = a*B'
     * @return DLEQProofResult containing (e, s) scalars
     */
    public static DLEQProofResult generateProof(
            @NonNull BigInteger privateKey,
            @NonNull ECPoint blindedMessage,
            @NonNull ECPoint blindSignature
    ) {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint generator = spec.getG();
        BigInteger n = spec.getN();

        if (privateKey.signum() <= 0 || privateKey.compareTo(n) >= 0) {
            throw InvalidKeyException.privateKeyOutOfRange();
        }

        ECPoint publicKey = generator.multiply(privateKey).normalize();

        BigInteger nonce = generateRandomScalar(n);
        ECPoint rG = generator.multiply(nonce).normalize();
        ECPoint rB = blindedMessage.multiply(nonce).normalize();

        byte[] eBytes = dleqHash(rG, rB, publicKey, blindSignature);
        BigInteger e = new BigInteger(1, eBytes).mod(n);

        BigInteger s = nonce.add(e.multiply(privateKey)).mod(n);

        return new DLEQProofResult(
                Utils.bytesToHexString(Utils.bytesFromBigInteger(e)),
                Utils.bytesToHexString(Utils.bytesFromBigInteger(s))
        );
    }

    /**
     * Verifies a DLEQ proof for a BlindSignature (Alice's verification).
     *
     * @param e The challenge scalar (hex string)
     * @param s The response scalar (hex string)
     * @param blindedMessage The blinded message point B'
     * @param blindSignature The blind signature point C'
     * @param publicKey The mint's public key point A
     * @return true if the proof is valid
     */
    public static boolean verifyProof(
            @NonNull String e,
            @NonNull String s,
            @NonNull ECPoint blindedMessage,
            @NonNull ECPoint blindSignature,
            @NonNull ECPoint publicKey
    ) {
        try {
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            ECPoint generator = spec.getG();
            BigInteger n = spec.getN();

            BigInteger challenge = Utils.bigIntFromBytes(Utils.hexStringToBytes(e));
            BigInteger response = Utils.bigIntFromBytes(Utils.hexStringToBytes(s));

            ECPoint sG = generator.multiply(response).normalize();
            ECPoint eA = publicKey.multiply(challenge).normalize();
            ECPoint R1 = sG.subtract(eA).normalize();

            ECPoint sB = blindedMessage.multiply(response).normalize();
            ECPoint eC = blindSignature.multiply(challenge).normalize();
            ECPoint R2 = sB.subtract(eC).normalize();

            BigInteger expectedChallenge = new BigInteger(1, dleqHash(R1, R2, publicKey, blindSignature)).mod(n);

            boolean valid = challenge.equals(expectedChallenge);
            log.debug("DLEQ verification result: {}", valid);
            return valid;
        } catch (Exception ex) {
            log.error("DLEQ verification failed with exception", ex);
            return false;
        }
    }

    /**
     * Verifies a DLEQ proof from a received Proof (Carol's verification).
     *
     * <p>Reconstructs B' and C' using the provided blinding factor before
     * deferring to {@link #verifyProof(String, String, ECPoint, ECPoint, ECPoint)}.
     *
     * @param e The challenge scalar (hex string)
     * @param s The response scalar (hex string)
     * @param r The blinding factor (hex string)
     * @param secret The proof secret (for hash_to_curve)
     * @param unblindedSignature The unblinded signature point C
     * @param publicKey The mint's public key point A
     * @return true if the proof is valid
     */
    public static boolean verifyProofWithBlindingFactor(
            @NonNull String e,
            @NonNull String s,
            @NonNull String r,
            @NonNull byte[] secret,
            @NonNull ECPoint unblindedSignature,
            @NonNull ECPoint publicKey
    ) {
        try {
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            ECPoint generator = spec.getG();

            BigInteger blindingFactor = Utils.bigIntFromBytes(Utils.hexStringToBytes(r));

            ECPoint Y = BDHKEUtils.hashToCurve(secret);
            ECPoint blindedMessage = Y.add(generator.multiply(blindingFactor)).normalize();
            ECPoint blindedSignature = unblindedSignature.add(publicKey.multiply(blindingFactor)).normalize();

            return verifyProof(e, s, blindedMessage, blindedSignature, publicKey);
        } catch (Exception ex) {
            log.error("DLEQ verification with blinding factor failed", ex);
            return false;
        }
    }

    /**
     * Verifies a DLEQ proof carried by a received Proof, given the secret as a string.
     *
     * <p>The secret is fed to {@code hash_to_curve} under each encoding in
     * {@link SecretEncoding#verificationOrder()}, so a proof issued before the NUT-00 secret
     * encoding was corrected still verifies.
     *
     * @param e The challenge scalar (hex string)
     * @param s The response scalar (hex string)
     * @param r The blinding factor (hex string)
     * @param secret The proof secret string
     * @param unblindedSignature The unblinded signature point C
     * @param publicKey The mint's public key point A
     * @return true if the proof is valid under any accepted secret encoding
     */
    public static boolean verifyProofWithBlindingFactor(
            @NonNull String e,
            @NonNull String s,
            @NonNull String r,
            @NonNull String secret,
            @NonNull ECPoint unblindedSignature,
            @NonNull ECPoint publicKey
    ) {
        for (SecretEncoding encoding : SecretEncoding.verificationOrder()) {
            if (!encoding.supports(secret)) {
                continue;
            }
            if (verifyProofWithBlindingFactor(e, s, r, encoding.encode(secret), unblindedSignature, publicKey)) {
                log.debug("dleq verify_succeeded encoding={}", encoding);
                return true;
            }
        }
        return false;
    }

    /**
     * Computes the DLEQ hash: SHA256(R1 || R2 || A || C').
     *
     * <p>Points are serialized in uncompressed format (04 || X || Y) and
     * concatenated as UTF-8 text before hashing.
     *
     * <p>Package-private: internal implementation detail.
     */
    static byte[] dleqHash(
            @NonNull ECPoint R1,
            @NonNull ECPoint R2,
            @NonNull ECPoint publicKey,
            @NonNull ECPoint blindedSignature
    ) {
        try {
            String concatenated =
                    pointToUncompressedHex(R1) +
                    pointToUncompressedHex(R2) +
                    pointToUncompressedHex(publicKey) +
                    pointToUncompressedHex(blindedSignature);

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(concatenated.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new CashuCryptoException("SHA-256 not available", e);
        }
    }

    /**
     * Converts an EC point to uncompressed hex format (04 || X || Y).
     *
     * <p>Package-private: internal implementation detail.
     */
    static String pointToUncompressedHex(@NonNull ECPoint point) {
        return Utils.bytesToHexString(point.normalize().getEncoded(false));
    }

    private static BigInteger generateRandomScalar(BigInteger n) {
        BigInteger candidate;
        do {
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            candidate = new BigInteger(1, bytes).mod(n);
        } while (candidate.equals(BigInteger.ZERO));
        return candidate;
    }

    /**
     * Result of DLEQ proof generation.
     */
    public record DLEQProofResult(String e, String s) {
    }
}
