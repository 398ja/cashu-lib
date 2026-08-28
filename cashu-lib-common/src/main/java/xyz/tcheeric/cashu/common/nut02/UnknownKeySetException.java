package xyz.tcheeric.cashu.common.nut02;

import lombok.Getter;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.nut00.CashuErrorCode;
import xyz.tcheeric.cashu.common.util.CashuErrorException;

/**
 * Thrown when a proof references a keyset id the resolver does not know.
 *
 * <p>NUT-02 names this condition {@code 12001 Keyset is not known}. It is a protocol validation,
 * so it must surface as a typed error a caller can map onto that code rather than as an
 * assertion, which is disabled at runtime by default.
 */
@Getter
public class UnknownKeySetException extends CashuErrorException {

    private final String keySetId;

    public UnknownKeySetException(@NonNull String keySetId) {
        super(CashuErrorCode.keyset_not_known, "Keyset is not known. Got keyset id: " + keySetId);
        this.keySetId = keySetId;
    }
}
