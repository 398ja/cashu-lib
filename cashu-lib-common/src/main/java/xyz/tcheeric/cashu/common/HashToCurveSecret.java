package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.util.SecretUtil;


public class HashToCurveSecret {

    private CompressedPublicKey publicKey;

    public HashToCurveSecret(@NonNull Secret secret) {
        this((CompressedPublicKey) PublicKey.fromString(SecretUtil.toY(secret), true));
    }

    public HashToCurveSecret(@NonNull Proof proof) {
        this(proof.getSecret());
    }

    HashToCurveSecret(@NonNull CompressedPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static HashToCurveSecret fromString(@NonNull String htcs) {
        CompressedPublicKey publicKey = CompressedPublicKey.fromString(htcs);
        return new HashToCurveSecret(publicKey);
    }

    @Override
    public String toString() {
        return publicKey.toString();
    }
}
