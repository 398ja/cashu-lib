package cashu.vault.impl.fs;

import cashu.common.model.KeySet;
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
import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
public class FSKeysetVault extends FSVault<KeysetConfiguration> {

    @NonNull
    private final KeysetConfiguration keysetConfiguration;

    @Override
    public void store() throws CashuException {
        var mint = keysetConfiguration.getMint();
        var id = keysetConfiguration.getId();
        var unit = keysetConfiguration.getUnit();

        // <baseDir>/mint/<privateKey>/<unit>/[.keyset_id]
        String mintPath = mintPath(mint);
        Path filePath = Paths.get(mintPath, unit, "." + id);

        try {
            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);
        } catch (IOException e) {
            Error error = new Error(e);
            error.setDetail("Failed to create directory: " + filePath);
            throw new CashuException(error);
        }
    }

    @Override
    public String retrieve(@NonNull String keysetId, boolean archive) {
        var mintPath = mintPath(keysetConfiguration.getMint(), archive);
        var unit = keysetConfiguration.getUnit();

        Path keysetPath = Paths.get(mintPath, unit, "." + keysetId);

        return keysetPath.toString();
    }

    public static KeySet load(@NonNull KeysetConfiguration keysetConfiguration, boolean archive) {
        FSMintVault mintVault = new FSMintVault(keysetConfiguration.getMint());
        String mintPath = mintVault.retrieve(keysetConfiguration.getMint().getPrivateKey(), archive);
        Path keysPath = Paths.get(mintPath, keysetConfiguration.getUnit());
        loadKeysetId(keysetConfiguration);

        return KeySet.builder().id(keysetConfiguration.getId()).build();
    }

    static void loadKeysetId(@NonNull KeysetConfiguration keysetConfiguration) {
        String mintPath = mintPath(keysetConfiguration.getMint());
        String unit = keysetConfiguration.getUnit();
        Path unitPath = Paths.get(mintPath, unit);

        try (Stream<Path> paths = Files.list(unitPath)) {
            Optional<Path> keysetIdPath = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("."))
                    .findFirst();

            if (keysetIdPath.isPresent()) {
                var result = keysetIdPath.get().getFileName().toString().substring(1); // remove the leading dot
                keysetConfiguration.setId(result);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return;
    }

    @Override
    public void archive(@NonNull String key) throws CashuException {
        var keysetPath = retrieve(key, false);
        Path sourcePath = Paths.get(keysetPath);

        String mintArchivePath = mintArchivePath(keysetConfiguration.getMint());
        String unit = keysetConfiguration.getUnit();

        Path keysetArchivePath = Paths.get(mintArchivePath, unit, "." + key);

        try {
            Files.createDirectories(keysetArchivePath.getParent());
            Files.move(sourcePath, keysetArchivePath);
        } catch (IOException e) {
            throw new CashuException(e);
        }
    }

    private static String mintPath(@NonNull MintConfiguration mint) {
        return mintPath(mint, false);
    }

    private static String mintPath(@NonNull MintConfiguration mint, boolean archive) {
        PrivateKey privateKey = PrivateKey.fromString(mint.getPrivateKey());

        var baseDir = getBaseDir(archive);

        Path dirPath = Paths.get(baseDir, "mint", privateKey.toString());
        return dirPath.toString();
    }
}
