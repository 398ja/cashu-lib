package xyz.tcheeric.cashu.entities.rest.nut03;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.entities.rest.PostInputRequest;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class PostSwapRequest<T extends Secret> extends PostInputRequest<T> {

    /**
     * Maximum number of outputs allowed per request to prevent DoS attacks.
     */
    public static final int MAX_OUTPUTS = 1000;

    public PostSwapRequest(@NonNull List<Proof<T>> proofs, @NonNull List<BlindedMessage> blindedMessages) {
        super(proofs);
        this.blindedMessages = blindedMessages;
    }

    @JsonProperty("outputs")
    @NotNull(message = "Outputs are required")
    @NotEmpty(message = "At least one output is required")
    @Size(max = MAX_OUTPUTS, message = "Maximum " + MAX_OUTPUTS + " outputs allowed")
    @Valid
    private List<BlindedMessage> blindedMessages;
}
