package cashu.vault.impl.fs;

import cashu.common.model.PrivateKey;
import cashu.common.model.PublicKey;
import cashu.common.protocol.CashuException;
import cashu.util.Utils;
import cashu.vault.FSVault;
import cashu.vault.config.MintConfiguration;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class FSMintVault extends FSVault<MintConfiguration> {

    @Override
    public void store(@NonNull MintConfiguration entity) throws CashuException {
        PrivateKey privateKey = PrivateKey.fromString(entity.getPrivateKey());
        PublicKey publicKey = PublicKey.fromBytes(Utils.getPublicKey(privateKey.getBytes()));

        String publicKeyHex = Utils.bytesToHex(publicKey.getBytes());
        String privateKeyHex = Utils.bytesToHex(privateKey.getBytes());

        var baseDir = getBaseDir();

        Path dirPath = Paths.get(baseDir, "mint", publicKeyHex);
        Path filePath = dirPath.resolve(privateKeyHex);

        try {
            Files.createDirectories(dirPath);
            Files.createFile(filePath);
        } catch (IOException e) {
            throw new CashuException(e);
        }
    }

    @Override
    public String retrieve(@NonNull String key) throws CashuException {
        String publicKeyHex = Utils.bytesToHex(PublicKey.fromBytes(key.getBytes()).getBytes());

        var baseDir = getBaseDir();

        Path dirPath = Paths.get(baseDir, "mint", publicKeyHex);

        try (Stream<Path> paths = Files.list(dirPath)) {
            return paths
                    .findFirst()
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .orElse(null);
        } catch (IOException e) {
            throw new CashuException(e);
        }
    }

    @Override
    public void archive(String key) throws CashuException {
        throw new CashuException(new IllegalAccessException("Not implemented"));
    }
}