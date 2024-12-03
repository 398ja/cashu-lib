package cashu.common.model.rest;

import cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class PostMeltRequest extends PostInputRequest {

    public PostMeltRequest(@NonNull String quoteId, @NonNull List<Proof> proofs) {
        super(proofs);
        this.quoteId = quoteId;
    }

    @JsonProperty("quote")
    private String quoteId;

    @JsonProperty("inputs")
    private List<Proof> proofs;
}
