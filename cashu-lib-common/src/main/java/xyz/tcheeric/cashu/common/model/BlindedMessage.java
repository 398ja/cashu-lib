package xyz.tcheeric.cashu.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonPropertyOrder({"amount", "id", "B_", "witness"})
public class BlindedMessage {

    @JsonProperty
    private int amount;

    @JsonProperty("id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String keySetId;

    @JsonProperty("B_")
    private PublicKey blindedMessage;

    @JsonProperty("witness")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Witness witness;
}
