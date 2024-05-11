package cashu.vault.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@AllArgsConstructor
@Builder
@Getter
public class KeyConfiguration implements EntityConfiguration {

    private final KeysetConfiguration keyset;
    private final BigInteger amount;
    private final String privateKey;

}
