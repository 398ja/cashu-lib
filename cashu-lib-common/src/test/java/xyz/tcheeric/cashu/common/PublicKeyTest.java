package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import org.bouncycastle.util.encoders.DecoderException;
import xyz.tcheeric.cashu.crypto.util.Point;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class PublicKeyTest {

    // Verifies that providing uncompressed bytes (x||y) compresses back to the expected 02/03||x format
    @Test
    public void compressUncompressedMatchesCompressed() {
        String compressed = "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2";

        byte[] xBytes = Utils.hexStringToBytes(compressed.substring(2));
        Point p = Point.liftX(xBytes);
        assertNotNull(p);

        boolean needsEven = compressed.startsWith("02");
        BigInteger y = p.getY();
        if (needsEven != Point.hasEvenY(p)) {
            y = Point.getp().subtract(y); // flip parity to match prefix
        }

        byte[] x32 = leftPad32(p.getX().toByteArray());
        byte[] y32 = leftPad32(y.toByteArray());
        byte[] uncompressed = new byte[64];
        System.arraycopy(x32, 0, uncompressed, 0, 32);
        System.arraycopy(y32, 0, uncompressed, 32, 32);

        PublicKey fromUncompressed = PublicKey.fromBytes(uncompressed, false);
        assertEquals(compressed, fromUncompressed.toString());

        PublicKey fromCompressed = PublicKey.fromString(compressed);
        assertEquals(compressed, fromCompressed.toString());
    }

    // Ensures getSchnorr() instance and static method return the same bytes (x-coordinate)
    @Test
    public void getSchnorrInstanceAndStaticMatch() {
        String compressed = "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2";
        PublicKey pk = PublicKey.fromString(compressed);
        byte[] a = pk.getSchnorr();
        byte[] b = PublicKey.getSchnorr(pk);
        assertArrayEquals(a, b);
        assertArrayEquals(Utils.hexStringToBytes(compressed.substring(2)), a);
    }

    // Confirms invalid compressed length triggers an exception on construction
    @Test
    public void invalidCompressedLengthThrows() {
        String tooShort = "02" + "a".repeat(63);
        String tooLong = "02" + "a".repeat(65);
        assertThrows(DecoderException.class, () -> PublicKey.fromString(tooShort));
        assertThrows(DecoderException.class, () -> PublicKey.fromString(tooLong));
    }

    // Confirms invalid uncompressed lengths (not 64 bytes) trigger an exception on construction
    @Test
    public void invalidUncompressedLengthThrows() {
        assertThrows(IllegalArgumentException.class, () -> PublicKey.fromBytes(new byte[63]));
        assertThrows(IllegalArgumentException.class, () -> PublicKey.fromBytes(new byte[65]));
    }

    private static byte[] leftPad32(byte[] src) {
        if (src.length == 32) return src;
        byte[] out = new byte[32];
        if (src.length > 32) {
            System.arraycopy(src, src.length - 32, out, 0, 32);
        } else {
            System.arraycopy(src, 0, out, 32 - src.length, src.length);
        }
        return out;
    }
}
