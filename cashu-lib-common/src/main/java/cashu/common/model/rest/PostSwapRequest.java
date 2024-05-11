package cashu.common.model.rest;

import cashu.common.model.BlindedMessage;
import cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class PostSwapRequest {

    @JsonProperty("inputs")
    private final List<Proof> proofs;

    @JsonProperty("outputs")
    private final List<BlindedMessage> blindedMessages;
}
