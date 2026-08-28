package xyz.tcheeric.cashu.common.util;

import lombok.Getter;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.nut00.CashuErrorCode;

/**
 * A Cashu protocol failure that a mint reports to a client as a NUT-00 error body.
 *
 * <p>Carrying the {@link CashuErrorCode} as a field is what lets a REST layer map the failure to a
 * numeric code and HTTP status directly, rather than re-parsing the exception message.
 */
public class CashuErrorException extends Exception {

    /** The error code, or {@code null} when the failure predates typed codes. */
    @Getter
    private final CashuErrorCode errorCode;

    public CashuErrorException(@NonNull Throwable t) {
        super(t);
        this.errorCode = null;
    }

    public CashuErrorException(@NonNull String message) {
        super(message);
        this.errorCode = null;
    }

    public CashuErrorException(@NonNull CashuErrorCode errorCode) {
        super(errorCode.getDefaultDetail());
        this.errorCode = errorCode;
    }

    public CashuErrorException(@NonNull CashuErrorCode errorCode, @NonNull String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public CashuErrorException(@NonNull CashuErrorCode errorCode, @NonNull String detail, @NonNull Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
    }

    /** Whether this failure carries a typed error code. */
    public boolean hasErrorCode() {
        return errorCode != null;
    }
}
