package cashu.common.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostMeltQuoteBolt11Request extends PostMeltQuoteRequest {

    @JsonProperty
    private String unit;

    public PostMeltQuoteBolt11Request(String requestId, String unit) {
        super(requestId);
        this.unit = unit;
    }
}
