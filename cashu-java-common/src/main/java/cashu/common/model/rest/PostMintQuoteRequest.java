package cashu.common.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostMintQuoteRequest {

    @JsonProperty
    private int amount;

    @JsonProperty
    private String unit;

    protected PostMintQuoteRequest(@NonNull int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0. Got: " + amount);
        }
        this.amount = amount;
    }
}
