package xyz.tcheeric.cashu.common.nut17;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for proof state change notifications (NUT-07 state).
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProofStatePayload {
    /**
     * The proof's Y value (compressed point of secret).
     */
    @JsonProperty("Y")
    private String y;

    /**
     * The current state: UNSPENT, PENDING, or SPENT.
     */
    private String state;

    /**
     * Optional witness data (for P2PK spending conditions).
     */
    private String witness;
}
