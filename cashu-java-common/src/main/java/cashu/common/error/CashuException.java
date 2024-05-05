package cashu.common.error;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class CashuException extends Exception {

    private final Error error;

    public CashuException(@NonNull Exception e) {
        this.error = new Error(e);
    }

    @Override
    public String getMessage() {
        return "%d:%s".formatted(error.getCode(), error.getDetail());
    }
}
