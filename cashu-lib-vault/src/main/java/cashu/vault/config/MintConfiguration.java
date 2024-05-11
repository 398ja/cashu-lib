package cashu.vault.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
public class MintConfiguration implements EntityConfiguration {

    private final String privateKey;
}
