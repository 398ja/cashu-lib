package cashu.vault.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@AllArgsConstructor
@Getter
public class KeysetConfiguration implements EntityConfiguration {

    private final MintConfiguration mint;

    @Setter
    private String id;
    private final String unit;
}
