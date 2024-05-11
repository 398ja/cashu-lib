package cashu.vault.impl.fs;

import cashu.common.model.PrivateKey;
import cashu.common.model.PublicKey;
import cashu.common.protocol.CashuException;
import cashu.common.protocol.Error;
import cashu.util.Configuration;
import cashu.util.Utils;
import cashu.vault.FSVault;
import cashu.vault.config.KeysetConfiguration;
import cashu.vault.config.MintConfiguration;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class FSKeysetVault extends FSVault<KeysetConfiguration> {

    @Override
    public void store(@NonNull KeysetConfiguration entity) throws CashuException {
        var mint = entity.getMint();
        var id = entity.getId();

        String mintPath = mintPath(mint);
        Path dirPath = Paths.get(mintPath, "keyset", id);

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
        KeysetConfiguration keysetConfiguration = getKeysetConfiguration(keysetId);
        if (keysetConfiguration == null) {
            return null;
        }

        MintConfiguration mint = keysetConfiguration.getMint();
        String mintPath = mintPath(mint);
        Path keysetPath = Paths.get(mintPath, "keyset", keysetId);

        return keysetPath.toString();
    }

    @Override
    public void archive(@NonNull String key) throws CashuException {
        var keysetPath = retrieve(key);
        Path sourcePath = Paths.get(keysetPath);

        KeysetConfiguration keysetConfiguration = getKeysetConfiguration(key);
        if (keysetConfiguration == null) {
            return;
        }

        MintConfiguration mint = keysetConfiguration.getMint();
        String mintArchivePath = mintArchivePath(mint);
        Path keysetArchivePath = Paths.get(mintArchivePath, "keyset", key);

        try {
            Files.createDirectories(keysetArchivePath.getParent());
            Files.move(sourcePath, keysetArchivePath);
        } catch (IOException e) {
            throw new CashuException(e);
        }
    }

    private KeysetConfiguration getKeysetConfiguration(@NonNull String keysetId) throws CashuException {
        var baseDir = getBaseDir();

        try (Stream<Path> paths = Files.list(Paths.get(baseDir))) {
            Optional<Path> mintPath = paths
                    .filter(path -> Files.isDirectory(path))
                    .filter(path -> Files.exists(Paths.get(path.toString(), "keyset", keysetId)))
                    .findFirst();

            if (mintPath.isPresent()) {
                MintConfiguration mint = getMintConfiguration(mintPath.get().toString());
                if (mint != null) {
                    KeysetConfiguration keysetConfiguration = new KeysetConfiguration(mint, keysetId);
                    return keysetConfiguration;
                }
            }
        } catch (IOException e) {
            throw new CashuException(e);
        }

        return null;
    }

    private MintConfiguration getMintConfiguration(@NonNull String mintPath) throws CashuException {
        Path dirPath = Paths.get(mintPath);

        try (Stream<Path> paths = Files.list(dirPath)) {
            Optional<String> privateKeyHex = paths
                    .findFirst()
                    .map(Path::getFileName)
                    .map(Path::toString);

            if (privateKeyHex.isPresent()) {
                MintConfiguration mint = new MintConfiguration(Utils.hexToBytes(privateKeyHex.get()).toString());
                return mint;
            }
        } catch (IOException e) {
            throw new CashuException(e);
        }

        return null;
    }

    private static String mintPath(@NonNull MintConfiguration mint) {
        PrivateKey privateKey = PrivateKey.fromString(mint.getPrivateKey());
        PublicKey publicKey = PublicKey.fromBytes(Utils.getPublicKey(privateKey.getBytes()));

        String publicKeyHex = Utils.bytesToHex(publicKey.getBytes());

        var baseDir = getBaseDir();

        Path dirPath = Paths.get(baseDir, "mint", publicKeyHex);
        return dirPath.toString();
    }
}
