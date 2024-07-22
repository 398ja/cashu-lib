package cashu.common.protocol;

import lombok.NonNull;

public class CashuErrorException extends Exception {

    public CashuErrorException(@NonNull Throwable t) {
        super(t);
    }

    public CashuErrorException(@NonNull String message) {
        super(message);
    }

    public CashuErrorException(@NonNull Error error) {
        super(error.toString());
    }
}
