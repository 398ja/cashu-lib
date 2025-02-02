package xyz.tcheeric.cashu.common.model.rest;

import xyz.tcheeric.cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.model.Secret;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class PostMeltRequest<T extends Secret> extends PostInputRequest<T> {

    public PostMeltRequest(@NonNull String quoteId, @NonNull List<Proof<T>> proofs) {
        super(proofs);
        this.quoteId = quoteId;
    }

    @JsonProperty("quote")
    private String quoteId;

}
