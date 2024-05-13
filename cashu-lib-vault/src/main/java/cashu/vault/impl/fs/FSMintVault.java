package cashu.vault.impl.fs;

import cashu.common.model.PrivateKey;
import cashu.common.model.PublicKey;
import cashu.common.protocol.CashuException;
import cashu.crypto.KeysUtils;
import cashu.util.Utils;
import cashu.vault.FSVault;
import cashu.vault.config.MintConfiguration;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@AllArgsConstructor
public class FSMintVault extends FSVault<MintConfiguration> {

    @NonNull
    private final MintConfiguration mintConfiguration;

    @Override
    public void store() throws CashuException {
        PrivateKey privateKey = PrivateKey.fromString(mintConfiguration.getPrivateKey());
        PublicKey publicKey = KeysUtils.derivePublicKey(privateKey);
        String privateKeyHex = Utils.bytesToHexString(privateKey.getBytes());

        var baseDir = getBaseDir();
        Path dirPath = Paths.get(baseDir, "mint", privateKeyHex);

        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new CashuException(e);
        }
    }

    @Override
    public String retrieve(@NonNull String key) {
        var baseDir = getBaseDir();

        Path dirPath = Paths.get(baseDir, "mint", key);

        return dirPath.toString();
    }

    @Override
    public void archive(String key) throws CashuException {
        throw new CashuException(new IllegalAccessException("Not implemented"));
    }
}