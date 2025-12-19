package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.util.SecretUtil;

/**
 * Represents the hash-to-curve result Y = hash_to_curve(secret) for a Cashu secret.
 *
 * <p>This is an elliptic curve point derived from a secret using the hash-to-curve
 * algorithm specified in NUT-00. Used for BDHKE blind signature operations.
 */
public class HashToCurveSecret {

    private final PublicKey publicKey;

    public HashToCurveSecret(@NonNull Secret secret) {
        this(PublicKey.fromString(SecretUtil.toY(secret)));
    }

    public HashToCurveSecret(@NonNull Proof proof) {
        this(proof.getSecret());
    }

    HashToCurveSecret(@NonNull PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static HashToCurveSecret fromString(@NonNull String htcs) {
        PublicKey publicKey = PublicKey.fromString(htcs);
        return new HashToCurveSecret(publicKey);
    }

    @JsonValue
    public String toJson() {
        return publicKey.toString();
    }

    @Override
    public String toString() {
        return toJson();
    }
}
