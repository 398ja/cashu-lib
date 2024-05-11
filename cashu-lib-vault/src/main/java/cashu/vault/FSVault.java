package cashu.vault;

import cashu.common.model.PrivateKey;
import cashu.common.model.PublicKey;
import cashu.util.Configuration;
import cashu.util.Utils;
import cashu.vault.config.EntityConfiguration;
import cashu.vault.config.MintConfiguration;
import cashu.vault.impl.fs.FSProofVault;
import lombok.NonNull;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class FSVault<T extends EntityConfiguration> implements Vault<T> {

    protected static String getBaseDir() {
        InputStream inputStream = FSVault.class.getResourceAsStream("application.properties");
        Configuration configuration = Configuration.load(inputStream);
        return configuration.getValue("base_dir");
    }

    protected static String getArchiveDir() {
        InputStream inputStream = FSVault.class.getResourceAsStream("application.properties");
        Configuration configuration = Configuration.load(inputStream);
        return configuration.getValue("archive_dir");
    }

    protected static String mintArchivePath(@NonNull MintConfiguration mint) {
        PrivateKey privateKey = PrivateKey.fromString(mint.getPrivateKey());
        PublicKey publicKey = PublicKey.fromBytes(Utils.getPublicKey(privateKey.getBytes()));

        String publicKeyHex = Utils.bytesToHex(publicKey.getBytes());

        var archiveDir = getArchiveDir();

        Path dirPath = Paths.get(archiveDir, "mint", publicKeyHex);
        return dirPath.toString();
    }

}
