package cashu.vault.impl.fs;

import cashu.common.model.PrivateKey;
import cashu.common.model.PublicKey;
import cashu.common.model.Signature;
import cashu.common.protocol.CashuException;
import cashu.common.protocol.Error;
import cashu.util.Configuration;
import cashu.util.Utils;
import cashu.vault.FSVault;
import cashu.vault.config.MintConfiguration;
import cashu.vault.config.ProofConfiguration;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@AllArgsConstructor
public class FSProofVault extends FSVault<ProofConfiguration> {

    private final MintConfiguration mint;

    @Override
    public void store(@NonNull ProofConfiguration entity) throws CashuException {
        try {
            var baseDir = getBaseDir();

            PrivateKey privateKey = PrivateKey.fromString(entity.getMint().getPrivateKey());
            PublicKey publicKey = PublicKey.fromBytes(Utils.getPublicKey(privateKey.getBytes()));
            String secret = entity.getSecret();
            String unblindedSignature = entity.getUnblindedSignature();

            Path path = Paths.get(baseDir, "mint", publicKey.toString(), "proofs", secret);
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(path, unblindedSignature.getBytes());
        } catch (Exception e) {
            Error error = new Error(e);
            error.setDetail("Failed to store proof");
            throw new CashuException(error);
        }
    }

    @Override
    public String retrieve(@NonNull String key) throws CashuException {
        try {
            var baseDir = getBaseDir();

            Path path = Paths.get(baseDir, "mint", mint.getPrivateKey().toString(), "proofs", key);

            if (Files.exists(path)) {
                byte[] keyBytes = Files.readAllBytes(path);
                return Signature.fromBytes(keyBytes).toString();
            } else {
                return null;
            }
        } catch (Exception e) {
            Error error = new Error(e);
            error.setDetail("Failed to retrieve proof");
            throw new CashuException(error);
        }
    }

    @Override
    public void archive(String key) throws CashuException {
        throw new CashuException(new IllegalAccessException("Not implemented"));
    }
}
