package cashu.common.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class Payment {

    public enum Protocol {
        BOLT11,
        BOLT12,
        ON_CHAIN,
        CASH,
        CREDIT_CARD,
        MOBILE_MONEY,
        PAYMENT_API,
        MOCK
    }

    private final Protocol protocol;
    private String carrier;

}
