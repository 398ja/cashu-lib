package cashu.common.model.rest;

import cashu.common.model.Proof;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
public class PostMeltBolt11Request extends PostMeltRequest {
    public PostMeltBolt11Request(String quoteId, List<Proof> proofs) {
        super(quoteId, proofs);
    }
}
