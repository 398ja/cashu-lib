package xyz.tcheeric.cashu.common.nut13;

import lombok.Getter;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.KeysetIdVersion;

/**
 * Thrown when NUT-13 deterministic derivation is requested for a keyset id version this
 * library cannot derive secrets for.
 *
 * <p>Deriving version 1 secrets for a version 2 keyset would produce secrets the mint never
 * signed, so restore would return nothing and look exactly like an empty wallet. Failing loudly
 * keeps that failure distinguishable.
 */
@Getter
public class UnsupportedKeysetVersionException extends IllegalArgumentException {

    private final transient KeysetId keysetId;
    private final KeysetIdVersion version;

    public UnsupportedKeysetVersionException(@NonNull KeysetId keysetId, @NonNull KeysetIdVersion version) {
        super("NUT-13 deterministic derivation supports keyset id version " + KeysetIdVersion.V1
                + " only. Got version " + version + " for keyset id: " + keysetId);
        this.keysetId = keysetId;
        this.version = version;
    }
}
