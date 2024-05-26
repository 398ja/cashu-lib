package cashu.vault.impl.fs;

import cashu.common.model.Keys;
import cashu.common.model.PrivateKey;
import cashu.common.protocol.CashuException;
import cashu.common.protocol.Error;
import cashu.vault.FSVault;
import cashu.vault.config.KeyConfiguration;
import cashu.vault.config.KeysetConfiguration;
import cashu.vault.config.MintConfiguration;
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

        // <baseDir>/mint/<privateKey>/<unit>/<amount>/[key]
        FSMintVault mintVault = new FSMintVault(keyset.getMint());
        String mintPath = mintVault.retrieve(keyset.getMint().getPrivateKey(), false);
        Path filePath = Paths.get(mintPath, keyset.getUnit(), amount.toString(), keyConfiguration.getPrivateKey());

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
    public String retrieve(@NonNull String key, boolean archive) throws CashuException {
        KeysetConfiguration keyset = keyConfiguration.getKeyset();
        FSMintVault mintVault = new FSMintVault(keyset.getMint());
        String mintPath = mintVault.retrieve(keyset.getMint().getPrivateKey(), archive);
        Path dirPath = Paths.get(mintPath, keyset.getUnit(), keyConfiguration.getAmount().toString());

        try (Stream<Path> paths = Files.list(dirPath)) {
            Optional<Path> keyFilePath = paths
                    .filter(path -> path.getFileName().toString().equals(key)).min((p1, p2) -> -Long.compare(p1.toFile().lastModified(), p2.toFile().lastModified()));

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

    public static Keys load(@NonNull KeyConfiguration keyConfiguration, boolean archive) {
        KeysetConfiguration keysetConfiguration = keyConfiguration.getKeyset();
        FSMintVault mintVault = new FSMintVault(keysetConfiguration.getMint());
        String mintPath = mintVault.retrieve(keysetConfiguration.getMint().getPrivateKey(), archive);
        Path dirPath = Paths.get(mintPath, keysetConfiguration.getUnit());

        Keys keys = new Keys();

        try (Stream<Path> paths = Files.list(dirPath)) {
            paths.filter(Files::isDirectory).forEach(dir -> {
                try (Stream<Path> fileStream = Files.list(dir)) {
                    fileStream.forEach(file -> {
                        String privateKeyStr = file.getFileName().toString();
                        PrivateKey privateKey = PrivateKey.fromString(privateKeyStr);
                        BigInteger dirNameAsBigInt = new BigInteger(dir.getFileName().toString());
                        keys.put(dirNameAsBigInt, PrivateKey.derivePublicKey(privateKey));
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return keys;
    }

    // TODO - Create unit test
    public static Keys get(@NonNull String mintPrivateKey, @NonNull String unit) {
        return get(mintPrivateKey, unit, false);
    }

    public static Keys get(@NonNull String mintPrivateKey, @NonNull String unit, boolean archive) {
        FSMintVault mintVault = new FSMintVault(new MintConfiguration(mintPrivateKey));
        String mintPath = mintVault.retrieve(mintPrivateKey, archive);
        Path unitDir = Paths.get(mintPath, unit);
        Keys keys = new Keys();
        try (Stream<Path> paths = Files.list(unitDir)) {
            paths.filter(Files::isDirectory).forEach(dir -> {
                try (Stream<Path> fileStream = Files.list(dir)) {
                    Optional<Path> keyFilePath = fileStream
                            .min((p1, p2) -> -Long.compare(p1.toFile().lastModified(), p2.toFile().lastModified()));
                    if (keyFilePath.isPresent()) {
                        String privateKeyStr = keyFilePath.get().getFileName().toString();
                        PrivateKey privateKey = PrivateKey.fromString(privateKeyStr);
                        BigInteger dirNameAsBigInt = new BigInteger(dir.getFileName().toString());
                        keys.put(dirNameAsBigInt, PrivateKey.derivePublicKey(privateKey));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return keys;
    }

    @Override
    public void archive(@NonNull String key) throws CashuException {
        var keyPath = retrieve(key, false);
        if (keyPath == null) {
            return;
        }

        Path archivePath = Paths.get(FSVault.mintArchivePath(keyConfiguration.getKeyset().getMint()), keyConfiguration.getKeyset().getUnit(), keyConfiguration.getAmount().toString(), key);
        try {
            Files.createDirectories(archivePath.getParent());
            Files.move(Paths.get(keyPath), archivePath);
        } catch (IOException e) {
            throw new CashuException(e);
        }
    }
}
