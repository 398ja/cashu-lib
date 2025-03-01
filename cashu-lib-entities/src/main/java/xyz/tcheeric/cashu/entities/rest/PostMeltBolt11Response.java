package xyz.tcheeric.cashu.entities.rest;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PostMeltBolt11Response extends PostMeltResponse {
    public PostMeltBolt11Response(boolean paid, String paymentPreimage) {
        super(paid, paymentPreimage);
    }
}
