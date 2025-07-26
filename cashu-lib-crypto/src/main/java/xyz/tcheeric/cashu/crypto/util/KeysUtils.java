package xyz.tcheeric.cashu.crypto.util;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;

public class KeysUtils {

    /**
     * Generate a random private key that can be used with Secp256k1.
     *
     * @return
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

    public static byte[] derivePublicKey(byte[] secKey) {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECDomainParameters domain = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN());
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(Utils.bigIntFromBytes(secKey), domain);
        ECPoint Q = domain.getG().multiply(privateKeyParameters.getD());
        return Q.getEncoded(true);
    }
}
