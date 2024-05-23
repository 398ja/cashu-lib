package cashu.common.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class PostMeltQuoteBolt11Request extends PostMeltQuoteRequest {

    @JsonProperty
    private final String unit;

    public PostMeltQuoteBolt11Request(String requestId, String unit) {
        super(requestId);
        this.unit = unit;
    }
}
