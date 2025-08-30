package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.util.Point;
import xyz.tcheeric.cashu.crypto.util.Utils;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SignatureTest {

    // Verifies equals is reflexive, symmetric, transitive, and consistent for identical signatures
    @Test
    public void equalsAndHashCodeForIdenticalSignatures() {
        String hex = "02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea";
        Signature s1 = Signature.fromString(hex);
        Signature s2 = Signature.fromString(hex);

        assertTrue(s1.equals(s1)); // reflexive
        assertEquals(s1, s2);      // symmetric (part 1)
        assertEquals(s2, s1);      // symmetric (part 2)
        assertEquals(s1.hashCode(), s2.hashCode()); // consistent hashCode for equal objects
    }

    // Ensures signatures with different values are not equal
    @Test
    public void notEqualsForDifferentSignatures() {
        Signature s1 = Signature.fromString("02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea");
        Signature s2 = Signature.fromString("029e8e5050b890a7d6c0968db16bc1d5d5fa040ea1de284f6ec69d61299f671059");

        assertNotEquals(s1, s2);
    }

    // Confirms equals returns false when compared to an object of a different class
    @Test
    public void equalsWithDifferentTypeReturnsFalse() {
        Signature s1 = Signature.fromString("02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea");
        assertFalse(s1.equals("not a signature"));
    }

    // Verifies that fromBytes (x||y) and fromString (02/03||x) produce equal Signatures for the same point
    @Test
    public void fromBytesAndFromStringAreEquivalent() {
        String compressed = "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2";
        // Extract x and compute y from curve equation; adjust parity to match prefix (02 => even y)
        byte[] xBytes = Utils.hexStringToBytes(compressed.substring(2));
        Point p = Point.liftX(xBytes);
        assertNotNull(p);

        boolean needsEven = compressed.startsWith("02");
        BigInteger y = p.getY();
        if (needsEven != Point.hasEvenY(p)) {
            y = Point.getp().subtract(y); // flip parity
        }

        byte[] x32 = leftPad32(p.getX().toByteArray());
        byte[] y32 = leftPad32(y.toByteArray());
        byte[] uncompressed = new byte[64];
        System.arraycopy(x32, 0, uncompressed, 0, 32);
        System.arraycopy(y32, 0, uncompressed, 32, 32);

        Signature a = Signature.fromString(compressed);
        Signature b = Signature.fromBytes(uncompressed);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
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

    // Ensures Signature.fromBytes rejects byte arrays not equal to 64 bytes (x||y)
    @Test
    public void fromBytesInvalidLengthThrows() {
        assertThrows(IllegalArgumentException.class, () -> Signature.fromBytes(new byte[63]));
        assertThrows(IllegalArgumentException.class, () -> Signature.fromBytes(new byte[65]));
    }

    // Validates end-to-end sign and verify using a fixed private key and derived public key
    @Test
    public void signAndVerifyRoundTrip() throws Exception {
        // Schnorr.sign expects a 32-byte message; use a 32-char ASCII string
        String message = "0123456789abcdef0123456789abcdef";
        PrivateKey priv = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        PublicKey pub = PublicKey.derivePublicKey(priv);

        Signature sig = Signature.sign(message, priv);
        assertTrue(Signature.verify(message, pub, sig));
        assertTrue(sig.verify(message, pub));
    }

    // Validates that verification fails when the message changes
    @Test
    public void verifyFailsOnDifferentMessage() throws Exception {
        String message1 = "0123456789abcdef0123456789abcdef";
        String message2 = "fedcba9876543210fedcba9876543210";
        PrivateKey priv = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        PublicKey pub = PublicKey.derivePublicKey(priv);

        Signature sig = Signature.sign(message1, priv);
        assertFalse(Signature.verify(message2, pub, sig));
        assertFalse(sig.verify(message2, pub));
    }
}
