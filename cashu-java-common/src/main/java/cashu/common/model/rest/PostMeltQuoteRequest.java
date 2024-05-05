package cashu.common.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public abstract class PostMeltQuoteRequest {

    @JsonProperty("request")
    private final String requestId;
}
