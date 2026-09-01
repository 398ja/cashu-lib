package xyz.tcheeric.cashu.crypto;

import lombok.NonNull;
import org.bouncycastle.math.ec.ECPoint;

/**
 * Test-only access to the package-private NUT-12 challenge hash.
 *
 * <p>{@code DLEQUtils.dleqHash} is deliberately not public: it is an implementation detail of proof
 * generation and verification. The NUT-12 vectors publish its inputs and output directly, though,
 * so this bridge lets {@code Nut12VectorTest} drive it without widening the production API.
 */
public final class DleqChallengeHash {

    private DleqChallengeHash() {
        // Utility class - prevent instantiation
    }

    /**
     * Computes {@code hash_e(R1, R2, K, C_)}.
     */
    public static byte[] of(
            @NonNull ECPoint r1,
            @NonNull ECPoint r2,
            @NonNull ECPoint publicKey,
            @NonNull ECPoint blindSignature) {
        return DLEQUtils.dleqHash(r1, r2, publicKey, blindSignature);
    }
}
