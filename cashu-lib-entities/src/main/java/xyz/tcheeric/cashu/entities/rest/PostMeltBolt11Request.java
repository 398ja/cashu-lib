package xyz.tcheeric.cashu.entities.rest;

import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;

import java.util.List;

@NoArgsConstructor
public class PostMeltBolt11Request<T extends Secret> extends PostMeltRequest<T> {
    public PostMeltBolt11Request(String quoteId, List<Proof<T>> proofs) {
        super(quoteId, proofs);
    }
}
