package cashu.vault.impl.fs;

import cashu.common.protocol.CashuException;
import cashu.common.protocol.Error;
import cashu.vault.FSVault;
import cashu.vault.config.KeyConfiguration;
import cashu.vault.config.KeysetConfiguration;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
public class FSKeyVault extends FSVault<KeyConfiguration> {

    @NonNull
    private final KeyConfiguration keyConfiguration;

    @Override
    public void store() throws CashuException {
        KeysetConfiguration keyset = keyConfiguration.getKeyset();
        BigInteger amount = keyConfiguration.getAmount();

        FSKeysetVault keysetVault = new FSKeysetVault(keyConfiguration.getKeyset());
        String keysetPath = keysetVault.retrieve(keyset.getId());
        Path filePath = Paths.get(keysetPath, amount.toString(), keyConfiguration.getPrivateKey().toString());

        try {
            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);
        } catch (IOException e) {
            Error error = new Error(e);
            error.setDetail("Failed to write to file: " + filePath);
            throw new CashuException(error);
        }
    }

    @Override
    public String retrieve(@NonNull String key) throws CashuException {
        FSKeysetVault keysetVault = new FSKeysetVault(keyConfiguration.getKeyset());
        String keysetPath = keysetVault.retrieve(keyConfiguration.getKeyset().getId());
        Path dirPath = Paths.get(keysetPath, keyConfiguration.getAmount().toString());

        try (Stream<Path> paths = Files.list(dirPath)) {
            Optional<Path> keyFilePath = paths
                    .filter(path -> path.getFileName().toString().equals(key))
                    .sorted((p1, p2) -> -Long.compare(p1.toFile().lastModified(), p2.toFile().lastModified()))
                    .findFirst();

            if (keyFilePath.isPresent()) {
                return keyFilePath.get().toString();
            }
        } catch (IOException e) {
            Error error = new Error(e);
            error.setDetail("Failed to retrieve key: " + key);
            throw new CashuException(error);
        }

        return null;
    }

    @Override
    public void archive(@NonNull String key) throws CashuException {
        var keyPath = retrieve(key);
        if (keyPath == null) {
            return;
        }

        Path archivePath = Paths.get(FSVault.mintArchivePath(keyConfiguration.getKeyset().getMint()), "keyset", keyConfiguration.getKeyset().getId(), keyConfiguration.getAmount().toString(), key);
        try {
            Files.createDirectories(archivePath.getParent());
            Files.move(Paths.get(keyPath), archivePath);
        } catch (IOException e) {
            throw new CashuException(e);
        }
    }
}
