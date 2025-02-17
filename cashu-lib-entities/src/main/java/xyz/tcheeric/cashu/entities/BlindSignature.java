package xyz.tcheeric.cashu.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"amount", "id", "C_"})
public class BlindSignature {

    @JsonProperty
    private int amount;

    @JsonProperty("id")
    private String keySetId;

    @JsonProperty("C_")
    private Signature blindedSignature;
}
