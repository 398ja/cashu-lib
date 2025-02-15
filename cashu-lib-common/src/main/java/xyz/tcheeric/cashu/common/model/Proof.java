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
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"amount", "id", "secret", "C", "witness"})
public class Proof<T extends Secret> {

    @JsonProperty
    private int amount;

    @JsonProperty
    private T secret;

    @JsonProperty("id")
    private String keySetId;

    @JsonProperty("C")
    private Signature unblindedSignature;

    @JsonProperty("witness")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Witness witness;

    public void setSecretData(byte[] data) {
        this.secret.setData(data);
    }
}
