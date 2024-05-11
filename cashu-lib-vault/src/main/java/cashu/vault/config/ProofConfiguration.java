package cashu.vault.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@Builder
public class ProofConfiguration implements EntityConfiguration {

    private final MintConfiguration mint;
    private final String unblindedSignature;
    private final String secret;
}
