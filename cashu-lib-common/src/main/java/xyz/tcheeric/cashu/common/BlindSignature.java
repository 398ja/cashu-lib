package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.json.deserializer.BlindSignatureDeserializer;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = BlindSignatureDeserializer.class)
@JsonPropertyOrder({"amount", "id", "C_", "dleq"})
public class BlindSignature {

    @JsonProperty
    private int amount;

    @JsonProperty("id")
    private KeysetId keySetId;

    @JsonProperty("C_")
    private Signature blindedSignature;

    /**
     * Optional DLEQ proof for NUT-12 offline verification.
     */
    @JsonProperty("dleq")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DLEQProof dleq;

    /**
     * Checks if this BlindSignature includes a DLEQ proof.
     */
    public boolean hasDLEQProof() {
        return dleq != null;
    }
}
