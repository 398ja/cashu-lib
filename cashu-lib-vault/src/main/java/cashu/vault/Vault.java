package cashu.vault;

import cashu.common.protocol.CashuException;
import cashu.vault.config.EntityConfiguration;

public interface Vault<T extends EntityConfiguration> {

    void store(T entity) throws CashuException;

    String retrieve(String key) throws CashuException;

    void archive(String key) throws CashuException;
}
