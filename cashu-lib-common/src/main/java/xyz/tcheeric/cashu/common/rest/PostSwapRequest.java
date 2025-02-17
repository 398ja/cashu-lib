package xyz.tcheeric.cashu.common.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.entities.BlindedMessage;
import xyz.tcheeric.cashu.entities.Proof;
import xyz.tcheeric.cashu.entities.Secret;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class PostSwapRequest<T extends Secret> extends PostInputRequest<T> {

    public PostSwapRequest(@NonNull List<Proof<T>> proofs, @NonNull List<BlindedMessage> blindedMessages) {
        super(proofs);
        this.blindedMessages = blindedMessages;
    }

/*
    @JsonProperty("inputs")
    private List<Proof> proofs;
*/

    @JsonProperty("outputs")
    private List<BlindedMessage> blindedMessages;
}
