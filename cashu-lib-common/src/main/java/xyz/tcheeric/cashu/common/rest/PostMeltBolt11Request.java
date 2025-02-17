package xyz.tcheeric.cashu.common.rest;

import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.entities.Proof;
import xyz.tcheeric.cashu.entities.Secret;

import java.util.List;

@NoArgsConstructor
public class PostMeltBolt11Request<T extends Secret> extends PostMeltRequest<T> {
    public PostMeltBolt11Request(String quoteId, List<Proof<T>> proofs) {
        super(quoteId, proofs);
    }
}
