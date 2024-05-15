package cashu.vault.impl.fs;

import cashu.common.model.PrivateKey;
import cashu.common.protocol.CashuException;
import cashu.common.protocol.Error;
import cashu.vault.FSVault;
import cashu.vault.config.KeysetConfiguration;
import cashu.vault.config.MintConfiguration;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@AllArgsConstructor
public class FSKeysetVault extends FSVault<KeysetConfiguration> {

    @NonNull
    private final KeysetConfiguration keysetConfiguration;

    @Override
    public void store() throws CashuException {
        var mint = keysetConfiguration.getMint();
        var id = keysetConfiguration.getId();
        var unit = keysetConfiguration.getUnit();

        String mintPath = mintPath(mint);
        Path dirPath = Paths.get(mintPath, "keyset", unit, id);

        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            Error error = new Error(e);
            error.setDetail("Failed to create directory: " + dirPath);
            throw new CashuException(error);
        }
    }

    @Override
    public String retrieve(@NonNull String keysetId) throws CashuException {
        var mintPath = mintPath(keysetConfiguration.getMint());
        var unit = keysetConfiguration.getUnit();

        Path keysetPath = Paths.get(mintPath, "keyset", unit, keysetId);

        return keysetPath.toString();
    }

    @Override
    public void archive(@NonNull String key) throws CashuException {
        var keysetPath = retrieve(key);
        Path sourcePath = Paths.get(keysetPath);

        String mintArchivePath = mintArchivePath(keysetConfiguration.getMint());
        String unit = keysetConfiguration.getUnit();

        Path keysetArchivePath = Paths.get(mintArchivePath, "keyset", unit, key);

        try {
            Files.createDirectories(keysetArchivePath.getParent());
            Files.move(sourcePath, keysetArchivePath);
        } catch (IOException e) {
            throw new CashuException(e);
        }
    }

    private static String mintPath(@NonNull MintConfiguration mint) {
        PrivateKey privateKey = PrivateKey.fromString(mint.getPrivateKey());

        var baseDir = getBaseDir();

        Path dirPath = Paths.get(baseDir, "mint", privateKey.toString());
        return dirPath.toString();
    }
}
