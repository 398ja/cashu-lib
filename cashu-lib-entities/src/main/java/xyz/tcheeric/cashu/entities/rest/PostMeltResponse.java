package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostMeltResponse {

    @JsonProperty
    private boolean paid;

    @JsonProperty("payment_preimage")
    private String paymentPreimage;
}
