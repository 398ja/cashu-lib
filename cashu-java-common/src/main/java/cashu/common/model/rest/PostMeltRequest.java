package cashu.common.model.rest;

import cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
@Builder
public class PostMeltRequest {

    @JsonProperty("quote")
    private final String quoteId;

    @JsonProperty("inputs")
    private final List<Proof> proofs;
}
