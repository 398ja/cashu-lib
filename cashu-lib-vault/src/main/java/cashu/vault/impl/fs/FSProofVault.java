package cashu.vault.impl.fs;

import cashu.common.model.PrivateKey;
import cashu.common.model.Signature;
import cashu.common.protocol.CashuException;
import cashu.common.protocol.Error;
import cashu.vault.FSVault;
import cashu.vault.config.ProofConfiguration;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@AllArgsConstructor
public class FSProofVault extends FSVault<ProofConfiguration> {

    @NonNull
    private final ProofConfiguration proofConfiguration;

    @Override
    public void store() throws CashuException {
        try {
            var baseDir = getBaseDir();

            PrivateKey privateKey = PrivateKey.fromString(proofConfiguration.getMint().getPrivateKey());
            String secret = proofConfiguration.getSecret();
            String unblindedSignature = proofConfiguration.getUnblindedSignature();

            // <baseDir>/mint/<privateKey>/.proofs/[secret]
            Path path = Paths.get(baseDir, "mint", privateKey.toString(), ".proofs", secret);
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
    public String retrieve(@NonNull String key, boolean archive) throws CashuException {
        try {
            var baseDir = getBaseDir(archive);

            Path path = Paths.get(baseDir, "mint", proofConfiguration.getMint().getPrivateKey(), ".proofs", key);

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
