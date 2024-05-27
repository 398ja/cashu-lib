package cashu.crypto;

import cashu.common.model.Hex;
import lombok.NonNull;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class BDHKEUtils {

    private static final String DOMAIN_SEPARATOR = "Secp256k1_HashToCurve_Cashu_";

    public static ECPoint hashToCurve(byte[] message) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] msgToHash = sha256.digest(concat(DOMAIN_SEPARATOR.getBytes(StandardCharsets.UTF_8), message));
        int counter = 0;
        while (counter < Math.pow(2, 16)) {
            byte[] counterBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(counter).array();
            byte[] hash = sha256.digest(concat(msgToHash, counterBytes));
            byte[] pkHash = concat(new byte[]{0x02}, hash);
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            ECCurve curve = spec.getCurve();

            try {
                ECPoint publicKey = curve.decodePoint(pkHash);
                if (publicKey.isValid()) {
                    return publicKey;
                }
            } catch (IllegalArgumentException e) {
                // Ignore and continue with the next counter value
            }
            counter++;
        }
        throw new RuntimeException("No valid point found");
    }

    public static byte[][] blindMessage(byte[] secret) {
        byte[][] result = new byte[2][];
        ECPoint[] blindedMessage = blindMessage(new String(secret));
        result[0] = blindedMessage[0].getEncoded(true);
        result[1] = blindedMessage[1].getEncoded(true);
        return result;
    }

    public static ECPoint[] blindMessage(@NonNull String secret) {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECCurve curve = spec.getCurve();
        BigInteger r = new BigInteger(256, new SecureRandom());
        ECPoint G = spec.getG();
        ECPoint Y = hashToCurve(secret.getBytes());
        ECPoint rG = G.multiply(r);
        ECPoint B_ = Y.add(rG);
        return new ECPoint[]{B_, curve.createPoint(r, BigInteger.ZERO)};
    }

    public static byte[] signBlindedMessage(byte[] B_, byte[] k) {
        return signBlindedMessage(ECNamedCurveTable.getParameterSpec("secp256k1").getCurve().decodePoint(B_), new BigInteger(k)).getEncoded(true);
    }

    public static ECPoint signBlindedMessage(ECPoint B_, BigInteger k) {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECCurve curve = spec.getCurve();
        ECPoint C_ = B_.multiply(k);
        return C_;
    }

    public static byte[] unblindSignature(byte[] C_, byte[] r, byte[] K) {
        return unblindSignature(ECNamedCurveTable.getParameterSpec("secp256k1").getCurve().decodePoint(C_), new BigInteger(r), ECNamedCurveTable.getParameterSpec("secp256k1").getCurve().decodePoint(K)).getEncoded(true);
    }

    public static ECPoint unblindSignature(ECPoint C_, BigInteger r, ECPoint K) {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECCurve curve = spec.getCurve();
        ECPoint rK = K.multiply(r.negate());
        ECPoint C = C_.add(rK);
        return C;
    }

    public static boolean verify(String secret, byte[] k, byte[] C) {
        return verify(secret, new BigInteger(k), ECNamedCurveTable.getParameterSpec("secp256k1").getCurve().decodePoint(C));
    }

    public static boolean verify(String secret, BigInteger k, ECPoint C) {
        ECPoint Y = hashToCurve(secret.getBytes());
        boolean valid = verify(Y, k, C);
        if (!valid) {
            Y = hashToCurveDeprecated(secret.getBytes());
            valid = verify(Y, k, C);
        }
        return valid;
    }

    public static ECPoint hashToCurveDeprecated(byte[] message) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECCurve curve = spec.getCurve();
        ECPoint point = null;
        while (point == null || !point.isValid()) {
            byte[] hash = sha256.digest(message);
            byte[] pkHash = concat(new byte[]{0x02}, hash);
            point = curve.decodePoint(pkHash);
            message = hash;
        }
        return point;
    }

    public static Hex pointToHex(@NonNull ECPoint point) {
        byte[] pointBytes = point.getEncoded(true); // true for compressed point
        return Hex.fromBytes(pointBytes);
    }

    private static boolean verify(byte[] Y, byte[] k, byte[] C) {
        return verify(ECNamedCurveTable.getParameterSpec("secp256k1").getCurve().decodePoint(Y), new BigInteger(k), ECNamedCurveTable.getParameterSpec("secp256k1").getCurve().decodePoint(C));
    }

    private static boolean verify(ECPoint Y, BigInteger k, ECPoint C) {
        ECPoint result = Y.multiply(k);
        return C.equals(result);
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