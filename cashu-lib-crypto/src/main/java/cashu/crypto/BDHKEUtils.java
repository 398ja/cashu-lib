package cashu.crypto;

import cashu.common.model.PrivateKey;
import cashu.crypto.util.Utils;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;


@Log
public class BDHKEUtils {

    private static final byte[] DOMAIN_SEPARATOR = "Secp256k1_HashToCurve_Cashu_".getBytes(StandardCharsets.UTF_8);

    private static final SecP256K1Curve CURVE = new SecP256K1Curve();

    public static byte[] hashToCurve(@NonNull String secret) {
        ECPoint result = hashToCurve(Utils.hexStringToBytes(secret));
        return result.getEncoded(true);
    }

    public static ECPoint hashToCurve(byte[] secret) {
        log.log(Level.FINE, "hashToCurve({0})", Utils.bytesToHexString(secret));
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] secretToHash = sha256.digest(concat(DOMAIN_SEPARATOR, secret));
        int counter = 0;
        while (counter < Math.pow(2, 16)) {

            byte[] counterBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(counter).array();
            byte[] hash = sha256.digest(concat(secretToHash, counterBytes));
            byte[] pkHash = concat(new byte[]{0x02}, hash);

            try {
                ECPoint publicKey = CURVE.decodePoint(pkHash);
                if (publicKey.isValid()) {
                    return publicKey;
                }
            } catch (IllegalArgumentException e) {
                // Ignore and continue with the next counter value
                log.log(Level.FINE, "Invalid point: {0}. Ignoring...", Utils.bytesToHexString(pkHash));
            }
            counter++;
        }
        throw new RuntimeException("No valid point found");
    }

    public static byte[][] blindMessage(byte[] secret) {
        byte[][] result = new byte[2][];

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        BigInteger r = PrivateKey.generateRandom().toBigInteger();
        ECPoint G = spec.getG();
        ECPoint Y = hashToCurve(secret);
        ECPoint rG = G.multiply(r);
        ECPoint B_ = Y.add(rG);

        result[0] = B_.getEncoded(true);
        result[1] = Utils.bytesFromBigInteger(r);

        return result;
    }

    public static byte[] blindMessage(byte[] secret, byte[] r) {

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint G = spec.getG();
        ECPoint Y = hashToCurve(secret);
        ECPoint rG = G.multiply(Utils.bigIntFromBytes(r));
        ECPoint B_ = Y.add(rG);

        return B_.getEncoded(true);
    }

    public static byte[] signBlindedMessage(byte[] B_, byte[] k) {
        return signBlindedMessage(CURVE.decodePoint(B_), Utils.bigIntFromBytes(k)).getEncoded(true);
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

    public synchronized static boolean verify(@NonNull String secret, byte[] k, byte[] C) {
        boolean valid = verify(
                secret,
                Utils.bigIntFromBytes(k),
                CURVE.decodePoint(C)
        );
        log.log(Level.FINE, "Verification successful? {0}", valid);
        return valid;
    }

    public static boolean verify(@NonNull String secret, @NonNull BigInteger k, @NonNull ECPoint C) {
        ECPoint Y = hashToCurve(Utils.hexStringToBytes(secret));
        boolean valid = verify(Y, k, C);
        return valid;
    }


    private static boolean verify(byte[] Y, byte[] k, byte[] C) {
        log.log(Level.FINE, "verify({0}, {1}, {2})", new Object[]{Utils.bytesToHexString(Y), Utils.bytesToHexString(k), Utils.bytesToHexString(C)});
        return verify(CURVE.decodePoint(Y), Utils.bigIntFromBytes(k), CURVE.decodePoint(C));
    }

    private static boolean verify(ECPoint Y, BigInteger k, ECPoint C) {
        log.log(Level.FINE, "verify({0}, {1}, {2})", new Object[]{pointToHex(Y), Utils.bytesToHexString(Utils.bytesFromBigInteger(k)), pointToHex(C)});
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