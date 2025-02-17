package xyz.tcheeric.cashu.entities.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.entities.P2PKSecret;
import xyz.tcheeric.cashu.entities.RandomStringSecret;
import xyz.tcheeric.cashu.entities.Secret;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecretFactory<T extends Secret> {

    private byte[] p2pkPublicKey;

    public T create() {
        if (p2pkPublicKey == null) {
            return (T) RandomStringSecret.create();
        }

        return (T) new P2PKSecret(p2pkPublicKey);
    }

    public static <T extends Secret> T create(byte[] p2pkPublicKey) {
        return (T) new SecretFactory<T>(p2pkPublicKey).create();
    }
}
