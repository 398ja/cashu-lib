package cashu.vault.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MintConfiguration implements EntityConfiguration {

    private final String privateKey;
}
