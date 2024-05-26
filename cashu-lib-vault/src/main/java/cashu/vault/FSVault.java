package cashu.vault;

import cashu.common.model.PrivateKey;
import cashu.util.Configuration;
import cashu.vault.config.EntityConfiguration;
import cashu.vault.config.MintConfiguration;
import lombok.NonNull;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public abstract class FSVault<T extends EntityConfiguration> implements Vault<T> {

    protected static String getBaseDir() {
        return getBaseDir(false);
    }

    public static String getBaseDir(boolean archive) {
        InputStream inputStream = FSVault.class.getResourceAsStream("/vault.properties");
        Configuration configuration = Configuration.load(Objects.requireNonNull(inputStream));
        return archive ? configuration.getValue("archive_dir") : configuration.getValue("base_dir");
    }

    protected static String getArchiveDir() {
        InputStream inputStream = FSVault.class.getResourceAsStream("/vault.properties");
        Configuration configuration = Configuration.load(Objects.requireNonNull(inputStream));
        return configuration.getValue("archive_dir");
    }

    protected static String mintArchivePath(@NonNull MintConfiguration mint) {
        PrivateKey privateKey = PrivateKey.fromString(mint.getPrivateKey());

        var archiveDir = getArchiveDir();

        Path dirPath = Paths.get(archiveDir, "mint", privateKey.toString());
        return dirPath.toString();
    }

}
