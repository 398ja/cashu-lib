package xyz.tcheeric.cashu.crypto;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.ThreadSafe;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.exception.CashuCryptoException;
import xyz.tcheeric.cashu.crypto.util.KeysUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;



/**
 * Blind Diffie-Hellman Key Exchange (BDHKE) utilities for the Cashu protocol.
 *
 * <p>This class implements the core cryptographic operations for Cashu ecash:
 * <ul>
 *   <li>{@link #hashToCurve} - Deterministically maps secrets to curve points (Y)</li>
 *   <li>{@link #blindMessage} - Creates blinded messages (B') for blind signing</li>
 *   <li>{@link #signBlindedMessage} - Mint signs blinded messages (C')</li>
 *   <li>{@link #unblindSignature} - Wallet unblinds signatures to get proofs (C)</li>
 *   <li>{@link #verify} - Verifies that C = k*Y for a given secret</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All methods operate solely on local variables
 * and method parameters without accessing shared mutable state. The static
 * {@code CURVE} field is immutable and safe for concurrent access.
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><b>Blinding Factor Secrecy:</b> The blinding factor {@code r} must be
 *       kept secret by the wallet and never revealed to the mint. Disclosure
 *       allows the mint to identify which blinded message corresponds to which
 *       unblinded proof, breaking unlinkability.</li>
 *   <li><b>Secret Uniqueness:</b> Each secret should be used only once. Reusing
 *       secrets allows double-spend detection by the mint.</li>
 *   <li><b>Hash-to-Curve Security:</b> The {@link #hashToCurve} implementation
 *       uses the domain-separated hash function specified in NUT-00 to prevent
 *       cross-protocol attacks.</li>
 *   <li><b>DLEQ Verification:</b> Wallets should verify DLEQ proofs (NUT-12)
 *       when receiving blind signatures to ensure the mint used the correct
 *       private key.</li>
 * </ul>
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/00.md">NUT-00: BDHKE</a>
 * @see DLEQUtils
 */
@Slf4j
@ThreadSafe
public final class BDHKEUtils {

    private BDHKEUtils() {
        // Utility class - prevent instantiation
    }

    private static final byte[] DOMAIN_SEPARATOR = "Secp256k1_HashToCurve_Cashu_".getBytes(StandardCharsets.UTF_8);

    private static final SecP256K1Curve CURVE = new SecP256K1Curve();

    public static byte[] hashToCurve(String secret) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("secret must not be null or empty");
        }
        // NUT-10 well-known secrets are JSON arrays (start with '[')
        // They should be UTF-8 encoded for hashing, not hex decoded
        byte[] secretBytes;
        if (secret.startsWith("[")) {
            // NUT-10 JSON secret: UTF-8 encode the JSON string
            secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        } else {
            // Legacy hex secret: hex decode
            secretBytes = Utils.hexStringToBytes(secret);
        }
        ECPoint result = hashToCurve(secretBytes);
        return result.getEncoded(true);
    }

    public static ECPoint hashToCurve(byte[] secret) {
        if (secret == null || secret.length == 0) {
            throw new IllegalArgumentException("secret must not be null or empty");
        }
        log.debug("hashToCurve invoked with secret length {}", secret.length);
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new CashuCryptoException("SHA-256 not available", e);
        }
        byte[] secretToHash = sha256.digest(concat(DOMAIN_SEPARATOR, secret));
        long counter = 0;
        while (counter <= 0xFFFF_FFFFL) {
            byte[] counterBytes = ByteBuffer.allocate(4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt((int) counter)
                    .array();
            byte[] hash = sha256.digest(concat(secretToHash, counterBytes));
            byte[] pkHash = concat(new byte[]{0x02}, hash);

            try {
                ECPoint publicKey = CURVE.decodePoint(pkHash);
                if (publicKey.isValid()) {
                    return publicKey;
                }
            } catch (IllegalArgumentException e) {
                // Ignore and continue with the next counter value without revealing point data
                log.debug("Invalid point derived at counter {}. Retrying...", counter);
            }
            counter++;
        }
        throw new CashuCryptoException("hash_to_curve: no valid point found after exhausting counter");
    }

    public static byte[][] blindMessage(byte[] secret) {
        byte[][] result = new byte[2][];

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        BigInteger r = Utils.bigIntFromBytes(KeysUtils.generatePrivateKey());
        ECPoint G = spec.getG();
        ECPoint Y = hashToCurve(secret);
        ECPoint rG = G.multiply(r);
        ECPoint B_ = Y.add(rG);

        // Return B_ as uncompressed point without the 0x04 prefix: X(32) || Y(32) = 64 bytes
        byte[] uncompressed = B_.getEncoded(false); // 0x04 || X || Y
        result[0] = java.util.Arrays.copyOfRange(uncompressed, 1, uncompressed.length);
        result[1] = Utils.bytesFromBigInteger(r);

        return result;
    }

    public static byte[] blindMessage(byte[] secret, byte[] r) {

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint G = spec.getG();
        ECPoint Y = hashToCurve(secret);
        ECPoint rG = G.multiply(Utils.bigIntFromBytes(r));
        ECPoint B_ = Y.add(rG);

        byte[] uncompressed = B_.getEncoded(false); // 0x04 || X || Y
        return java.util.Arrays.copyOfRange(uncompressed, 1, uncompressed.length);
    }

    public static byte[] signBlindedMessage(byte[] B_, byte[] k) {
        // Accept raw64 (X||Y) or SEC1-encoded point. If raw64, add uncompressed prefix 0x04.
        byte[] sec1 = (B_ != null && B_.length == 64)
                ? concat(new byte[]{0x04}, B_)
                : B_;
        return signBlindedMessage(CURVE.decodePoint(sec1), Utils.bigIntFromBytes(k)).getEncoded(true);
    }

    public static ECPoint signBlindedMessage(@NonNull ECPoint B_, @NonNull BigInteger k) {
        ECPoint C_ = B_.multiply(k);
        return C_;
    }

    public static byte[] unblindSignature(byte[] C_, byte[] r, byte[] K) {
        return unblindSignature(CURVE.decodePoint(C_), Utils.bigIntFromBytes(r), CURVE.decodePoint(K)).getEncoded(true);
    }

    public static ECPoint unblindSignature(@NonNull ECPoint C_, @NonNull BigInteger r, @NonNull ECPoint K) {
        ECPoint rK = K.multiply(r.negate());
        ECPoint C = C_.add(rK);
        return C;
    }

    /**
     * Verify that the provided commitment {@code C} corresponds to the secret and key.
     * <p>
     * Thread-safe: this method operates solely on local variables and does not mutate
     * shared state.
     * </p>
     */
    public static boolean verify(@NonNull String secret, byte[] k, byte[] C) {
        boolean valid = verify(
                secret,
                Utils.bigIntFromBytes(k),
                CURVE.decodePoint(C)
        );
        log.debug("Verification successful? {}", valid);
        return valid;
    }

    public static boolean verify(@NonNull String secret, @NonNull BigInteger k, @NonNull ECPoint C) {
        // NUT-10 well-known secrets are JSON arrays (start with '[')
        // They should be UTF-8 encoded for hashing, not hex decoded
        // Legacy hex secrets continue to be hex decoded for backward compatibility
        byte[] secretBytes;
        if (secret.startsWith("[")) {
            // NUT-10 JSON secret: UTF-8 encode the JSON string
            secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            log.debug("verify: NUT-10 secret detected, using UTF-8 encoding, length={}", secretBytes.length);
        } else {
            // Legacy hex secret: hex decode
            secretBytes = Utils.hexStringToBytes(secret);
            log.debug("verify: hex secret detected, using hex decoding, length={}", secretBytes.length);
        }
        ECPoint Y = hashToCurve(secretBytes);
        boolean valid = verify(Y, k, C);
        return valid;
    }


    private static boolean verify(byte[] Y, byte[] k, byte[] C) {
        log.debug("verify(bytes): Y={}, k={}, C={}",
                Utils.bytesToHexString(Y), Utils.bytesToHexString(k), Utils.bytesToHexString(C));
        return verify(CURVE.decodePoint(Y), Utils.bigIntFromBytes(k), CURVE.decodePoint(C));
    }

    private static boolean verify(ECPoint Y, BigInteger k, ECPoint C) {
        log.debug("verify(points): Y={}, k={}, C={}",
                pointToHex(Y), Utils.bytesToHexString(Utils.bytesFromBigInteger(k)), pointToHex(C));
        ECPoint result = Y.multiply(k);
        return C.equals(result);
    }

    public static String pointToHex(@NonNull ECPoint point) {
        byte[] pointBytes = point.getEncoded(true); // true for compressed point
        return Hex.toHexString(pointBytes);
    }

    private static byte[] concat(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }
        byte[] result = new byte[totalLength];
        int currentIndex = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, currentIndex, array.length);
            currentIndex += array.length;
        }
        return result;
    }
}
