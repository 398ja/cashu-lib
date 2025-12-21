package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.json.deserializer.SecretDeserializer;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"amount", "id", "secret", "C", "dleq", "witness"})
public class Proof<T extends Secret> {

    @JsonProperty
    private int amount;

    @JsonProperty
    @JsonDeserialize(using = SecretDeserializer.class)
    private T secret;

    @JsonProperty("id")
    private String keySetId;

    @JsonProperty("C")
    private Signature unblindedSignature;

    /**
     * Optional DLEQ proof for NUT-12 offline verification.
     */
    @JsonProperty("dleq")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DLEQProof dleq;

    @JsonProperty("witness")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Witness witness;

    public void setSecretData(byte[] data) {
        this.secret.setData(data);
    }

    /**
     * Checks if this Proof includes a DLEQ proof.
     */
    public boolean hasDLEQProof() {
        return dleq != null;
    }

    /**
     * Checks if this Proof includes a DLEQ proof with the blinding factor.
     */
    public boolean hasDLEQWithBlindingFactor() {
        return dleq != null && dleq.hasBlindingFactor();
    }
}
