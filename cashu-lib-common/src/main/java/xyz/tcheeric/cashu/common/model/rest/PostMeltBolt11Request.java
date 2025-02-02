package xyz.tcheeric.cashu.common.model.rest;

import xyz.tcheeric.cashu.common.model.Proof;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.model.Secret;

import java.util.List;

@NoArgsConstructor
public class PostMeltBolt11Request<T extends Secret> extends PostMeltRequest<T> {
    public PostMeltBolt11Request(String quoteId, List<Proof<T>> proofs) {
        super(quoteId, proofs);
    }
}
