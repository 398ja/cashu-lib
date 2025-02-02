package xyz.tcheeric.cashu.common.model;

import xyz.tcheeric.cashu.common.json.deserializer.PrivateKeyDeserializer;
import xyz.tcheeric.cashu.common.json.deserializer.RandomStringSecretDeserializer;
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

@JsonDeserialize(using = PrivateKeyDeserializer.class)
public class PrivateKey extends CryptoElement {

    protected PrivateKey(@NonNull String value) {
        super(value, PRIVATE_KEY_LENGTH);
    }

    protected PrivateKey(byte[] value) {
        super(value, PRIVATE_KEY_LENGTH);
    }

    public static PrivateKey fromString(@NonNull String s) {
        return new PrivateKey(s);
    }

    public static PrivateKey fromBytes(byte[] bytes) {
        return new PrivateKey(bytes);
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