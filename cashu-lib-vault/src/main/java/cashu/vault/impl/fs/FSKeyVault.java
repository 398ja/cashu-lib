package cashu.vault.impl.fs;

import cashu.common.model.PrivateKey;
import cashu.common.protocol.CashuException;
import cashu.common.protocol.Error;
import cashu.vault.FSVault;
import cashu.vault.config.KeyConfiguration;
import cashu.vault.config.KeysetConfiguration;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
public class FSKeyVault extends FSVault<KeyConfiguration> {

    @NonNull
    private final KeysetConfiguration keyset;

    @NonNull
    private final BigInteger amount;

    @Override
    public void store(@NonNull KeyConfiguration entity) throws CashuException {
        KeysetConfiguration keyset = entity.getKeyset();
        BigInteger amount = entity.getAmount();

        if (amount != this.amount) {
            throw new CashuException(new AssertionError("Invalid amount/key"));
        }

        FSKeysetVault keysetVault = new FSKeysetVault();
        String keysetPath = keysetVault.retrieve(keyset.getId());
        Path filePath = Paths.get(keysetPath, amount.toString(), entity.getPrivateKey().toString());

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
        FSKeysetVault keysetVault = new FSKeysetVault();
        String keysetPath = keysetVault.retrieve(keyset.getId());
        Path dirPath = Paths.get(keysetPath, amount.toString());

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

        FSKeysetVault keysetVault = new FSKeysetVault();
        Path archivePath = Paths.get(keysetVault.mintArchivePath(keyset.getMint()), "keyset", keyset.getId(), key);
        try {
            Files.createDirectories(archivePath.getParent());
            Files.move(Paths.get(keyPath), archivePath);
        } catch (IOException e) {
            throw new CashuException(e);
        }
    }
}
