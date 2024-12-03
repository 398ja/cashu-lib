package cashu.common.model.rest;

import cashu.common.model.BlindedMessage;
import cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@NoArgsConstructor
@Data
public class PostSwapRequest extends PostInputRequest {

    public PostSwapRequest(@NonNull List<Proof> proofs, @NonNull List<BlindedMessage> blindedMessages) {
        super(proofs);
        this.blindedMessages = blindedMessages;
    }

    @JsonProperty("inputs")
    private List<Proof> proofs;

    @JsonProperty("outputs")
    private List<BlindedMessage> blindedMessages;
}
