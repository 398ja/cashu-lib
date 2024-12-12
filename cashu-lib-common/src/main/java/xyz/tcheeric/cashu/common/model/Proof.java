package xyz.tcheeric.cashu.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"amount", "id", "secret", "C"})
public class Proof {

    @JsonProperty
    private int amount;

    @JsonProperty
    private Secret secret;

    @JsonProperty("id")
    private String keySetId;

    @JsonProperty("C")
    private Signature unblindedSignature;
}
