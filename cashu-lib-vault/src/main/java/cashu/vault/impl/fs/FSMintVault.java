package cashu.vault.impl.fs;

import cashu.common.model.KeySet;
import cashu.common.model.Mint;
import cashu.common.model.PrivateKey;
import cashu.common.model.PublicKey;
import cashu.common.protocol.CashuException;
import cashu.util.Utils;
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
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
public class FSMintVault extends FSVault<MintConfiguration> {

    @NonNull
    private final MintConfiguration mintConfiguration;

    @Override
    public void store() throws CashuException {
        PrivateKey privateKey = PrivateKey.fromString(mintConfiguration.getPrivateKey());
        PublicKey publicKey = PrivateKey.derivePublicKey(privateKey);
        String privateKeyHex = Utils.bytesToHexString(privateKey.getBytes());

        // <baseDir>/mint/<privateKey>
        var baseDir = getBaseDir();
        Path dirPath = Paths.get(baseDir, "mint", privateKeyHex);

        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new CashuException(e);
        }
    }

    @Override
    public String retrieve(@NonNull String key, boolean archive) {
        var baseDir = getBaseDir(archive);

        Path dirPath = Paths.get(baseDir, "mint", key);

        return dirPath.toString();
    }

    @Override
    public void archive(String key) throws CashuException {
        throw new CashuException(new IllegalAccessException("Not implemented"));
    }

    public static Mint load(boolean archive) {
        return load(archive, false);
    }

    public static Mint load(boolean archive, boolean lazy) {
        String baseDir = getBaseDir(archive);
        try (Stream<Path> paths = Files.list(Paths.get(baseDir, "mint"))) {
            Optional<Path> mintDir = paths
                    .filter(Files::isDirectory)
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
            if (mintDir.isPresent()) {
                String privateKey = mintDir.get().getFileName().toString();
                MintConfiguration configuration = new MintConfiguration(privateKey);
                return load(configuration, archive, lazy);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Mint load(@NonNull MintConfiguration mintConfiguration, boolean archive, boolean lazy) {
        FSMintVault mintVault = new FSMintVault(mintConfiguration);
        String mintPath = mintVault.retrieve(mintConfiguration.getPrivateKey(), archive);

        Mint mint = new Mint(PrivateKey.fromString(mintConfiguration.getPrivateKey()));

        if (lazy) {
            return mint;
        }

        try (Stream<Path> paths = Files.list(Paths.get(mintPath))) {
            paths.filter(Files::isDirectory)
                    .forEach(unitPath -> { // Unit
                        String unit = unitPath.getFileName().toString();
                        KeysetConfiguration keysetConfiguration = new KeysetConfiguration(mintConfiguration, null, unit);
                        FSKeysetVault.loadKeysetId(keysetConfiguration);
                        KeySet keySet = KeySet.builder().id(keysetConfiguration.getId()).unit(unit).build();

                        try (Stream<Path> unitPaths = Files.list(unitPath)) {
                            unitPaths
                                    .filter(Files::isDirectory)
                                    .forEach(keyPath -> { // Amount
                                        String amount = keyPath.getFileName().toString();
                                        try (Stream<Path> keyPaths = Files.list(Paths.get(keyPath.toString()))) {
                                            keyPaths
                                                    .filter(Files::isRegularFile)
                                                    .forEach(orivateKeyFile -> { // Private Key
                                                        String privateKey = orivateKeyFile.getFileName().toString();
                                                        KeyConfiguration keyConfiguration = new KeyConfiguration(keysetConfiguration, new BigInteger(amount), privateKey);
                                                        keySet.setKeys(FSKeyVault.load(keyConfiguration, archive));
                                                    });
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                            mint.addKeySet(keySet);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mint;
    }
}