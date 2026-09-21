package xyz.tcheeric.cashu.entities.rest.nut05;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostMeltQuoteBolt11Response extends PostMeltQuoteResponse {

    public PostMeltQuoteBolt11Response(String quoteId, int amount, int feeReserve, boolean paid, int expiry) {
        super(quoteId, amount, feeReserve, paid, expiry);
    }
}
