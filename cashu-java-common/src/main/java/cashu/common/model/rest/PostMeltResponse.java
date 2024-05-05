package cashu.common.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PostMeltResponse {

    @JsonProperty
    private final boolean paid;

    @JsonProperty("payment_preimage")
    private final String paymentPreimage;
}
