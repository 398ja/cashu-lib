package cashu.common.model.rest;

import cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PostMeltRequest {

    @JsonProperty("quote")
    private String quoteId;

    @JsonProperty("inputs")
    private List<Proof> proofs;
}
