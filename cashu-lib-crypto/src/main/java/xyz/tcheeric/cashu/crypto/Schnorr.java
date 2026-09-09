package xyz.tcheeric.cashu.crypto;

import net.jcip.annotations.ThreadSafe;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import xyz.tcheeric.cashu.crypto.exception.CashuCryptoException;
import xyz.tcheeric.cashu.crypto.exception.InvalidKeyException;
import xyz.tcheeric.cashu.crypto.exception.SignatureException;
import xyz.tcheeric.cashu.crypto.util.Point;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import static xyz.tcheeric.cashu.crypto.util.Utils.bigIntFromBytes;

/**
 * BIP-340 Schnorr signature utilities for NUT-11 P2PK signatures.
 *
 * <p>This class provides methods for creating and verifying BIP-340 Schnorr
 * signatures on the secp256k1 curve, as used in the Cashu protocol for
 * Pay-to-Pubkey (P2PK) spending conditions.
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Methods operate on local variables without shared mutable
 * state; the one shared field is a {@link SecureRandom}, which is itself thread-safe.
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><b>Private Key Handling:</b> Private keys passed to {@link #sign} are
 *       not zeroed after use. Callers should use {@link xyz.tcheeric.cashu.common.PrivateKey}
 *       with try-with-resources for automatic key zeroing.</li>
 *   <li><b>Nonce Generation:</b> Signing uses RFC 6979-style deterministic
 *       nonces derived from the private key and message, preventing nonce reuse
 *       attacks.</li>
 *   <li><b>Verification:</b> The {@link #verify} method returns {@code false}
 *       for invalid signatures without revealing which part of verification
 *       failed, preventing oracle attacks.</li>
 *   <li><b>Timing:</b> This implementation does not provide constant-time
 *       guarantees. For high-security applications, consider additional
 *       countermeasures.</li>
 * </ul>
 *
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0340.mediawiki">BIP-340</a>
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/11.md">NUT-11: P2PK</a>
 */
@ThreadSafe
public final class Schnorr {

    /**
     * Shared CSPRNG for per-signature auxiliary randomness.
     *
     * <p>{@code SecureRandom.getInstanceStrong()} was being called once per signature (audit
     * L-10). On Linux that resolves to a blocking source by default, so a host with a depleted
     * entropy pool stalls every signing operation, and constructing a fresh instance each time
     * pays the seeding cost repeatedly for no benefit.
     *
     * <p>A single shared {@code new SecureRandom()} is the right tool here. It is
     * cryptographically secure, it is thread-safe, it does not block once seeded, and BIP-340
     * treats {@code aux_rand} as a hardening measure rather than the source of the nonce: the
     * nonce is derived from the private key and the message, so a signature remains secure even
     * if this value were entirely predictable.
     */
    private static final SecureRandom AUX_RAND = new SecureRandom();

    private Schnorr() {
        // Utility class - prevent instantiation
    }

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Create a BIP-340 Schnorr signature for the provided message using the
     * given secret key.
     * <p>
     * The implementation follows the algorithm defined in BIP-340 and operates on
     * the secp256k1 elliptic curve. The message and secret key must each be exactly
     * 32 bytes. The secret key is interpreted as an integer in the range
     * {@code [1, n-1]} where {@code n} is the curve order. The produced signature is
     * 64 bytes long and is verified internally before being returned.
     * </p>
     *
     * @param msg    32-byte message to sign
     * @param secKey 32-byte secp256k1 secret key
     * @return 64-byte Schnorr signature
     * @throws SignatureException if the message has invalid size, nonce generation fails,
     *                            or the resulting signature does not verify
     * @throws InvalidKeyException if the secret key is out of range
     */
    public static byte[] sign(byte[] msg, byte[] secKey) {
        if (msg.length != 32) {
            throw SignatureException.signatureFailed("message must be a 32-byte array");
        }
        BigInteger secKey0 = bigIntFromBytes(secKey);

        if (!(BigInteger.ONE.compareTo(secKey0) <= 0 && secKey0.compareTo(Point.getn().subtract(BigInteger.ONE)) <= 0)) {
            throw InvalidKeyException.privateKeyOutOfRange();
        }
        try {
            Point P = Point.mul(Point.getG(), secKey0);
            if (!P.hasEvenY()) {
                secKey0 = Point.getn().subtract(secKey0);
            }
            int len = Utils.bytesFromBigInteger(secKey0).length + P.toBytes().length + msg.length;
            byte[] buf = new byte[len];
            byte[] auxRand = new byte[32];
            AUX_RAND.nextBytes(auxRand);
            byte[] t = Utils.xor(Utils.bytesFromBigInteger(secKey0), Point.taggedHash("BIP0340/aux", auxRand));

            if (t == null) {
                throw new CashuCryptoException("Unexpected error during nonce derivation");
            }

            System.arraycopy(t, 0, buf, 0, t.length);
            System.arraycopy(P.toBytes(), 0, buf, t.length, P.toBytes().length);
            System.arraycopy(msg, 0, buf, t.length + P.toBytes().length, msg.length);
            BigInteger k0 = Utils.bigIntFromBytes(Point.taggedHash("BIP0340/nonce", buf)).mod(Point.getn());
            if (k0.compareTo(BigInteger.ZERO) == 0) {
                throw SignatureException.signatureFailed("nonce derivation produced zero");
            }
            Point R = Point.mul(Point.getG(), k0);
            BigInteger k;
            if (!R.hasEvenY()) {
                k = Point.getn().subtract(k0);
            } else {
                k = k0;
            }
            len = R.toBytes().length + P.toBytes().length + msg.length;
            buf = new byte[len];
            System.arraycopy(R.toBytes(), 0, buf, 0, R.toBytes().length);
            System.arraycopy(P.toBytes(), 0, buf, R.toBytes().length, P.toBytes().length);
            System.arraycopy(msg, 0, buf, R.toBytes().length + P.toBytes().length, msg.length);
            BigInteger e = Utils.bigIntFromBytes(Point.taggedHash("BIP0340/challenge", buf)).mod(Point.getn());
            BigInteger kes = k.add(e.multiply(secKey0)).mod(Point.getn());
            len = R.toBytes().length + Utils.bytesFromBigInteger(kes).length;
            byte[] sig = new byte[len];
            System.arraycopy(R.toBytes(), 0, sig, 0, R.toBytes().length);
            System.arraycopy(Utils.bytesFromBigInteger(kes), 0, sig, R.toBytes().length, Utils.bytesFromBigInteger(kes).length);
            if (!verify(msg, P.toBytes(), sig)) {
                throw SignatureException.signatureFailed("self-verification failed");
            }
            return sig;
        } catch (NoSuchAlgorithmException e) {
            throw new CashuCryptoException("Cryptographic algorithm not available", e);
        }
    }

    /**
     * Verify a BIP-340 Schnorr signature.
     * <p>
     * All inputs are expected to be fixed-size byte arrays: the message and public
     * key must both be 32 bytes, while the signature must be 64 bytes consisting of
     * the 32-byte X coordinate and 32-byte scalar {@code s}. The method validates
     * that the signature is consistent with the provided data under the secp256k1
     * curve using the challenge computation described in BIP-340.
     * </p>
     *
     * @param msg    32-byte message that was signed
     * @param pubkey 32-byte x-only public key
     * @param sig    64-byte Schnorr signature
     * @return {@code true} if the signature is valid according to BIP-340,
     *         {@code false} otherwise
     * @throws SignatureException if message or signature has incorrect size
     * @throws InvalidKeyException if public key has incorrect size
     */
    public static boolean verify(byte[] msg, byte[] pubkey, byte[] sig) {

        if (msg.length != 32) {
            throw new SignatureException(SignatureException.OperationType.VERIFY,
                    "Message must be a 32-byte array");
        }

        if (pubkey.length != 32) {
            throw InvalidKeyException.invalidPublicKey("must be a 32-byte x-only key");
        }
        if (sig.length != 64) {
            throw new SignatureException(SignatureException.OperationType.VERIFY,
                    "Signature must be a 64-byte array");
        }

        try {
            Point P = Point.liftX(pubkey);
            if (P == null) {
                return false;
            }
            BigInteger r = Utils.bigIntFromBytes(Arrays.copyOfRange(sig, 0, 32));
            BigInteger s = Utils.bigIntFromBytes(Arrays.copyOfRange(sig, 32, 64));
            if (r.compareTo(Point.getp()) >= 0 || s.compareTo(Point.getn()) >= 0) {
                return false;
            }
            int len = 32 + pubkey.length + msg.length;
            byte[] buf = new byte[len];
            System.arraycopy(sig, 0, buf, 0, 32);
            System.arraycopy(pubkey, 0, buf, 32, pubkey.length);
            System.arraycopy(msg, 0, buf, 32 + pubkey.length, msg.length);
            BigInteger e = Utils.bigIntFromBytes(Point.taggedHash("BIP0340/challenge", buf)).mod(Point.getn());
            Point R = Point.add(Point.mul(Point.getG(), s), Point.mul(P, Point.getn().subtract(e)));
            return R != null && R.hasEvenY() && R.getX().compareTo(r) == 0;
        } catch (NoSuchAlgorithmException e) {
            throw new CashuCryptoException("Cryptographic algorithm not available", e);
        }
    }

    /**
     * Generate a cryptographically-secure random private key for the secp256k1
     * curve.
     * <p>
     * The key is generated using the BouncyCastle provider and {@link
     * SecureRandom#getInstanceStrong()}. The returned byte array is 32 bytes long
     * and suitable for use with Schnorr signatures.
     * </p>
     *
     * @return 32-byte secp256k1 private key
     * @throws RuntimeException if the underlying crypto primitives are unavailable
     */
    public static byte[] generatePrivateKey() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
            kpg.initialize(new ECGenParameterSpec("secp256k1"), SecureRandom.getInstanceStrong());
            KeyPair processorKeyPair = kpg.genKeyPair();

            return Utils.bytesFromBigInteger(((ECPrivateKey) processorKeyPair.getPrivate()).getS());

        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new CashuCryptoException("Failed to generate private key", e);
        }
    }

    /**
     * Derive the x-only public key corresponding to the supplied secret key.
     * <p>
     * The secret key must be a 32-byte scalar in the range {@code [1, n-1]}. The
     * returned public key is the 32-byte X coordinate of the point {@code secKey * G}
     * on the secp256k1 curve, as required by BIP-340.
     * </p>
     *
     * @param secKey 32-byte secp256k1 secret key
     * @return 32-byte x-only public key
     * @throws InvalidKeyException if the secret key is out of range
     */
    public static byte[] genPubKey(byte[] secKey) {
        BigInteger x = Utils.bigIntFromBytes(secKey);
        if (!(BigInteger.ONE.compareTo(x) <= 0 && x.compareTo(Point.getn().subtract(BigInteger.ONE)) <= 0)) {
            throw InvalidKeyException.privateKeyOutOfRange();
        }
        Point ret = Point.mul(Point.G, x);
        return Point.bytesFromPoint(ret);
    }
}
