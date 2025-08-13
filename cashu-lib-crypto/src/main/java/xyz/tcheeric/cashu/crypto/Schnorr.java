package xyz.tcheeric.cashu.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
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

public class Schnorr {

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
     * @throws Exception if the message or key have invalid sizes, the secret key is
     *                   out of range, nonce generation fails, or the resulting
     *                   signature does not verify
     */
    public static byte[] sign(byte[] msg, byte[] secKey) throws Exception {
        if (msg.length != 32) {
            throw new Exception("The message must be a 32-byte array.");
        }
        BigInteger secKey0 = bigIntFromBytes(secKey);

        if (!(BigInteger.ONE.compareTo(secKey0) <= 0 && secKey0.compareTo(Point.getn().subtract(BigInteger.ONE)) <= 0)) {
            throw new Exception("The secret key must be an integer in the range 1..n-1.");
        }
        Point P = Point.mul(Point.getG(), secKey0);
        if (!P.hasEvenY()) {
            secKey0 = Point.getn().subtract(secKey0);
        }
        int len = Utils.bytesFromBigInteger(secKey0).length + P.toBytes().length + msg.length;
        byte[] buf = new byte[len];
        byte[] auxRand = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(auxRand);
        byte[] t = Utils.xor(Utils.bytesFromBigInteger(secKey0), Point.taggedHash("BIP0340/aux", auxRand));

        if (t == null) {
            throw new RuntimeException("Unexpected error. Null array");
        }

        System.arraycopy(t, 0, buf, 0, t.length);
        System.arraycopy(P.toBytes(), 0, buf, t.length, P.toBytes().length);
        System.arraycopy(msg, 0, buf, t.length + P.toBytes().length, msg.length);
        BigInteger k0 = Utils.bigIntFromBytes(Point.taggedHash("BIP0340/nonce", buf)).mod(Point.getn());
        if (k0.compareTo(BigInteger.ZERO) == 0) {
            throw new Exception("Failure. This happens only with negligible probability.");
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
            throw new Exception("The signature does not pass verification.");
        }
        return sig;
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
     * @throws Exception if any input has an incorrect size
     */
    public static boolean verify(byte[] msg, byte[] pubkey, byte[] sig) throws Exception {

        if (msg.length != 32) {
            throw new Exception("The message must be a 32-byte array.");
        }

        if (pubkey.length != 32) {
            throw new Exception("The public key must be a 32-byte array.");
        }
        if (sig.length != 64) {
            throw new Exception("The signature must be a 64-byte array.");
        }

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
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDSA", "BC");
            kpg.initialize(new ECGenParameterSpec("secp256k1"), SecureRandom.getInstanceStrong());
            KeyPair processorKeyPair = kpg.genKeyPair();

            return Utils.bytesFromBigInteger(((ECPrivateKey) processorKeyPair.getPrivate()).getS());

        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
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
     * @throws Exception if the secret key is out of range
     */
    public static byte[] genPubKey(byte[] secKey) throws Exception {
        BigInteger x = Utils.bigIntFromBytes(secKey);
        if (!(BigInteger.ONE.compareTo(x) <= 0 && x.compareTo(Point.getn().subtract(BigInteger.ONE)) <= 0)) {
            throw new Exception("The secret key must be an integer in the range 1..n-1.");
        }
        Point ret = Point.mul(Point.G, x);
        return Point.bytesFromPoint(ret);
    }
}
