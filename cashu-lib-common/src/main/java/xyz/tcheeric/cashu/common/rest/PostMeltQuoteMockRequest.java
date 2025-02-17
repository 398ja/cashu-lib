package xyz.tcheeric.cashu.common.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostMeltQuoteMockRequest extends PostMeltQuoteRequest {

    private String unit;

    public PostMeltQuoteMockRequest(@NonNull String requestId, @NonNull String unit) {
        super(requestId);
        this.unit = unit;
    }
}
