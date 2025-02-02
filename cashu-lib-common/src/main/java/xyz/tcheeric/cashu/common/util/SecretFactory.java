package xyz.tcheeric.cashu.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.model.P2PKSecret;
import xyz.tcheeric.cashu.common.model.PublicKey;
import xyz.tcheeric.cashu.common.model.RandomStringSecret;
import xyz.tcheeric.cashu.common.model.Secret;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecretFactory<T extends Secret> {

    private PublicKey p2pkPublicKey;

    public T create() {
        if (p2pkPublicKey == null) {
            return (T) RandomStringSecret.create();
        }

        return (T) new P2PKSecret(p2pkPublicKey);
    }

    public static <T extends Secret> T create(PublicKey p2pkPublicKey) {
        return (T) new SecretFactory<T>(p2pkPublicKey).create();
    }
}
