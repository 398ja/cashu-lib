package cashu.crypto;

import cashu.common.model.Signature;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;

@Deprecated(forRemoval = true)
@RequiredArgsConstructor
public class SignatureUtils {

    private final Signature signature;
    private static final SecP256K1Curve CURVE = new SecP256K1Curve();

    public Signature normalize() {
        String normalizedSignature = normalize(signature.toString());
        return Signature.fromString((normalizedSignature));
    }

    public static Signature normalize(@NonNull Signature signature) {
        String normalizedSignature = normalize(signature.toString());
        return Signature.fromString((normalizedSignature));
    }

    private static String normalize(@NonNull String hexSignature) {
        // Assuming hexSignature is the CryptoElement representation of the signature
        BigInteger[] rs = decodeSignature(Hex.decode(hexSignature)); //decodeSignature(Utils.hexStringToBytes(hexSignature)); //
        BigInteger r = rs[0];
        BigInteger s = rs[1];

        //SecP256K1Curve curve = new SecP256K1Curve();
        BigInteger order = CURVE.getOrder();

        // Normalize s if it's in the upper half of the range
        if (s.compareTo(order.shiftRight(1)) > 0) {
            s = order.subtract(s);
        }

        // Re-encode the signature with the normalized s value
        return Hex.toHexString(encodeSignature(r, s));
    }

    private static byte[] encodeSignature(BigInteger r, BigInteger s) {
        // This method will encode the r and s components of a signature into a byte array.
        // The encoding format used here is a simple concatenation of the byte representations of r and s.
        // Note: This is a simplified approach. In a real-world scenario, you might use a more complex encoding scheme, like DER.

        // Convert r and s to byte arrays. The byte array size is determined by the maximum length needed to represent the numbers.
        byte[] rBytes = r.toByteArray();
        byte[] sBytes = s.toByteArray();

        // Determine the maximum length to ensure both parts are equal in size for the concatenation
        int maxLength = Math.max(rBytes.length, sBytes.length);

        // Prepare byte arrays to hold the r and s components, ensuring they are of equal length
        byte[] rPadded = new byte[maxLength];
        byte[] sPadded = new byte[maxLength];

        // Copy the r and s bytes to the rightmost part of the padded arrays
        System.arraycopy(rBytes, 0, rPadded, maxLength - rBytes.length, rBytes.length);
        System.arraycopy(sBytes, 0, sPadded, maxLength - sBytes.length, sBytes.length);

        // Concatenate the padded r and s arrays
        byte[] encodedSignature = new byte[2 * maxLength];
        System.arraycopy(rPadded, 0, encodedSignature, 0, maxLength);
        System.arraycopy(sPadded, 0, encodedSignature, maxLength, maxLength);

        return encodedSignature;
    }

    private static BigInteger[] decodeSignature(byte[] signature) {
        // Decode the signature bytes to r and s components
        // This is a simplified placeholder. Actual decoding will depend on the signature format (e.g., DER)
        BigInteger r = new BigInteger(1, new byte[]{signature[0], signature[1], signature[2], signature[3]});
        BigInteger s = new BigInteger(1, new byte[]{signature[4], signature[5], signature[6], signature[7]});
        return new BigInteger[]{r, s};
    }

}
