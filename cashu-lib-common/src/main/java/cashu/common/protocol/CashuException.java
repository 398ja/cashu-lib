package cashu.common.protocol;

import cashu.common.protocol.Error;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class CashuException extends Exception {

    private final Error error;

    public CashuException(@NonNull Throwable e) {
        this.error = new Error(e);
    }

    @Override
    public String getMessage() {
        return "%d:%s".formatted(error.getCode(), error.getDetail());
    }
}
