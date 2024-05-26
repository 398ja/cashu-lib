package cashu.common.model;

import cashu.common.json.deserializer.SecretDeserializer;
import cashu.util.Utils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

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

@JsonDeserialize(using = SecretDeserializer.class)
public class PrivateKey extends Hex {

    private PrivateKey(@NonNull String value) {
        super(value, PRIVATE_KEY_LENGTH);
    }

    private PrivateKey(byte[] value) {
        super(value, PRIVATE_KEY_LENGTH);
    }

    public static PrivateKey fromString(@NonNull String s) {
        Hex hex = Hex.fromString(s);
        return fromHex(hex);
    }

    public static PrivateKey fromBytes(byte[] bytes) {
        return fromString(Utils.bytesToHexString(bytes));
    }

    public static PrivateKey fromBigInteger(@NonNull BigInteger b) {
        return fromString(Utils.bytesToHexString(b.toByteArray()));
    }

    public static PublicKey derivePublicKey(@NonNull PrivateKey privateKey) {
        return KeysUtils.derivePublicKey(privateKey);
    }

    public static PrivateKey generateRandom() {
        return KeysUtils.generatePrivateKey();
    }

    private static PrivateKey fromHex(@NonNull Hex hex) {
        if (hex.toString().length() != PRIVATE_KEY_LENGTH) {
            throw new IllegalArgumentException(String.format("Invalid length: %d", hex.toString().length()));
        }
        return new PrivateKey(hex.getBytes());
    }

    private static class KeysUtils {

        /**
         * Generate a random private key that can be used with Secp256k1.
         *
         * @return
         */
        static byte[] generateRawPrivateKey() {
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

        static PrivateKey generatePrivateKey() {
            return fromBytes(generateRawPrivateKey());
        }

        static byte[] derivePublicKey(byte[] secKey) {
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            ECDomainParameters domain = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN());
            ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(Utils.bigIntFromBytes(secKey), domain);
            ECPoint Q = domain.getG().multiply(privateKeyParameters.getD());
            return Q.getEncoded(true);
        }

        static PublicKey derivePublicKey(@NonNull PrivateKey privateKey) {
            return PublicKey.fromBytes(derivePublicKey(privateKey.getBytes()));
        }

    }
}