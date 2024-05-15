package cashu.test.vault;

import cashu.common.protocol.CashuException;
import cashu.crypto.KeysUtils;
import cashu.util.Configuration;
import cashu.vault.FSVault;
import cashu.vault.config.KeyConfiguration;
import cashu.vault.config.KeysetConfiguration;
import cashu.vault.config.MintConfiguration;
import cashu.vault.config.ProofConfiguration;
import cashu.vault.impl.fs.FSKeyVault;
import cashu.vault.impl.fs.FSKeysetVault;
import cashu.vault.impl.fs.FSMintVault;
import cashu.vault.impl.fs.FSProofVault;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FSVaultTest {

    @Test
    public void testMintVault() throws CashuException {
        String privateKey = KeysUtils.generatePrivateKey().toString();
        MintConfiguration mint = new MintConfiguration(privateKey);
        FSMintVault vault = new FSMintVault(mint);

        vault.store();

        Path mintVault = Path.of(getBaseDir(), "mint", privateKey);
        Assert.assertTrue(Files.exists(mintVault));

        var vaultPath = vault.retrieve(privateKey);
        assertEquals(mintVault.toString(), vaultPath);
    }

    @Test
    public void testKeySetVault() throws CashuException {
        String privateKey = KeysUtils.generatePrivateKey().toString();
        MintConfiguration mint = new MintConfiguration(privateKey);
        KeysetConfiguration keyset = new KeysetConfiguration(mint, "keyset1", "sat");
        FSKeysetVault vault = new FSKeysetVault(keyset);

        vault.store();
        Path ksPath = Path.of(getBaseDir(), "mint", privateKey, "keyset", "sat", "keyset1");
        Assert.assertTrue(Files.exists(ksPath));

        var vaultPath = vault.retrieve("keyset1");
        assertEquals(ksPath.toString(), vaultPath);

        vault.archive("keyset1");
        Assert.assertFalse(Files.exists(ksPath));

        var ksArchivePath = Path.of(getArchiveDir(), "mint", privateKey, "keyset", "sat", "keyset1");
        Assert.assertTrue(Files.exists(ksArchivePath));
    }

    @Test
    public void testKeyVault() throws CashuException {
        String privateKey = KeysUtils.generatePrivateKey().toString();
        MintConfiguration mint = new MintConfiguration(privateKey);
        KeysetConfiguration keyset = new KeysetConfiguration(mint, "keyset1", "sat");
        BigInteger amount = generateRandomBigInteger();
        KeyConfiguration key = new KeyConfiguration(keyset, amount, KeysUtils.generatePrivateKey().toString());

        FSKeyVault vault = new FSKeyVault(key);
        vault.store();

        Path keyPath = Paths.get(getBaseDir(), "mint", privateKey, "keyset", "sat", "keyset1", amount.toString(), key.getPrivateKey());
        assertTrue(Files.exists(keyPath));

        var vaultPath = vault.retrieve(key.getPrivateKey());
        assertEquals(keyPath.toString(), vaultPath);

        var archivePath = Paths.get(getArchiveDir(), "mint", privateKey, "keyset", "sat", "keyset1", amount.toString(), key.getPrivateKey());
        vault.archive(key.getPrivateKey());
        assertTrue(Files.exists(archivePath));
        assertFalse(Files.exists(keyPath));
    }

    @Test
    public void testProofVault() throws CashuException {
        var proofConfiguration = createProof();
        FSProofVault vault = new FSProofVault(proofConfiguration);

        vault.store();

        var proofPath = Paths.get(getBaseDir(), "mint", proofConfiguration.getMint().getPrivateKey(), "proofs", proofConfiguration.getSecret());
        assertTrue(Files.exists(proofPath));

/*
        var retrieveProof = vault.retrieve(proofConfiguration.getSecret());
        assertEquals(proofConfiguration.getUnblindedSignature(), retrieveProof);
*/
    }

    private static String getBaseDir() {
        InputStream inputStream = FSVault.class.getResourceAsStream("/application.properties");
        Configuration configuration = Configuration.load(inputStream);
        return configuration.getValue("base_dir");
    }

    private static String getArchiveDir() {
        InputStream inputStream = FSVault.class.getResourceAsStream("/application.properties");
        Configuration configuration = Configuration.load(inputStream);
        return configuration.getValue("archive_dir");
    }

    public static BigInteger generateRandomBigInteger() {
        BigInteger maxLimit = new BigInteger("1024");
        BigInteger minLimit = BigInteger.ONE;
        BigInteger bigInteger = maxLimit.subtract(minLimit);
        Random randNum = new Random();
        int len = maxLimit.bitLength();
        BigInteger res = new BigInteger(len, randNum);
        if (res.compareTo(minLimit) < 0)
            res = res.add(minLimit);
        if (res.compareTo(bigInteger) >= 0)
            res = res.mod(bigInteger).add(minLimit);
        return res;
    }

    private ProofConfiguration createProof() {
        String privateKey = KeysUtils.generatePrivateKey().toString();
        MintConfiguration mint = new MintConfiguration(privateKey);
        var unblindedSignature = generateRandomHexString(66);
        var secret = generateRandomHexString(64);

        return ProofConfiguration.builder()
                .unblindedSignature(unblindedSignature)
                .secret(secret)
                .mint(mint)
                .build();
    }

    public static String generateRandomHexString(int numchars) {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        while (sb.length() < numchars) {
            sb.append(Integer.toHexString(r.nextInt()));
        }
        return sb.toString().substring(0, numchars);
    }
}
