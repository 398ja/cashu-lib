package xyz.tcheeric.cashu.entities.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostMeltQuoteTestRequest extends PostMeltQuoteRequest {
    private String unit;

    public PostMeltQuoteTestRequest(@NonNull String requestId, @NonNull String unit) {
        super(requestId);
        this.unit = unit;
    }
}
