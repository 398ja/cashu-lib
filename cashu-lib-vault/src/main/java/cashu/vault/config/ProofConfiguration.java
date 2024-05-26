package cashu.vault.config;

import cashu.common.model.Proof;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ProofConfiguration implements EntityConfiguration {

    private final MintConfiguration mint;
    private final String unblindedSignature;
    private final String secret;
}
