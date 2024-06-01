package cashu.common.model.rest;

import cashu.common.model.BlindedMessage;
import cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostSwapRequest {

    @JsonProperty("inputs")
    private List<Proof> proofs;

    @JsonProperty("outputs")
    private List<BlindedMessage> blindedMessages;
}
