package xyz.tcheeric.cashu.common.model.rest;

import xyz.tcheeric.cashu.common.model.BlindedMessage;
import xyz.tcheeric.cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
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
