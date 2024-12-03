package cashu.gateway;

import lombok.NonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

public interface Gateway {

    String createQuote(@NonNull Integer amount, String description);

    String createQuote(@NonNull Integer amount, @NonNull String lnIvoice, String description);

    String getRequest(@NonNull String paymentId);

    boolean checkPaymentStatus(@NonNull String lnInvoice);

    String getPaymentPreimage(@NonNull String lnInvoice);

    String pay(@NonNull String lnInvoice);

    Integer getAmount(@NonNull String quoteId);

    Integer getPaymentExpiry(@NonNull String quoteId);

    Integer getFeeReserve(@NonNull String lnInvoice);

    @Deprecated(forRemoval = true)
    String getMethod();
}
