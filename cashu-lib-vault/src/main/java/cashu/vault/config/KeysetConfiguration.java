package cashu.vault.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@Getter
public class KeysetConfiguration implements EntityConfiguration {

    private final MintConfiguration mint;
    private final String id;
}
