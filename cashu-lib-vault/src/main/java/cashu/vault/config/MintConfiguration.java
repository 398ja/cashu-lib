package cashu.vault.config;

import cashu.common.model.Mint;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MintConfiguration implements EntityConfiguration {

    private final String privateKey;
}
