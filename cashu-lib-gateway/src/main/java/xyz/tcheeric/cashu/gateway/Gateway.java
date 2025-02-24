package xyz.tcheeric.cashu.gateway;

import lombok.NonNull;
import xyz.tcheeric.cashu.common.PaymentMethod;
import xyz.tcheeric.cashu.entities.annotation.Supports;

import java.util.Arrays;

public interface Gateway {

    /**
     * Create a quote for minting a certain amount
     * @param amount
     * @param description
     * @return
     */
    String createMintQuote(@NonNull Integer amount, String description);

    /**
     * Create a quote for melting a certain amount
     * @param amount
     * @param request
     * @param description
     * @return
     */
    String createMeltQuote(@NonNull Integer amount, @NonNull String request, String description);

    /**
     * Create a quote for melting, based on the request
     * @param request
     * @return
     */
    String createMeltQuote(@NonNull String request);

    /**
     * Get the request for a payment
     * @param quoteId
     * @return
     */
    String getRequest(@NonNull String quoteId);

    /**
     *
     * @param quoteId
     * @return
     *
    String getRequestWithQuoteId(@NonNull String quoteId);
    */

    /**
     * Check the payment status
     * @param request
     * @return
     */
    boolean checkPaymentStatus(@NonNull String request);

    /**
     * Get the payment preimage
     * @param request
     * @return
     */
    String getPaymentPreimage(@NonNull String request);

    /**
     * Make the payment (melt)
     * @param request
     * @return
     */
    String pay(@NonNull String request);

    /**
     * Get the amount of a quote
     * @param quoteId
     * @return
     */
    Integer getAmount(@NonNull String quoteId);

    /**
     * Get the expiry of a payment
     * @param quoteId
     * @return
     */
    Integer getPaymentExpiry(@NonNull String quoteId);

    /**
     * Get the fee reserve of a payment
     * @param request
     * @return
     */
    Integer getFeeReserve(@NonNull String request);

    String getName();

    /**
     * Test if the gateway supports the given payment method
     * @param method
     * @return
     */
    default boolean supports(@NonNull PaymentMethod method) {
        Supports supports = this.getClass().getAnnotation(Supports.class);
        if (supports != null) {
            return Arrays.asList(supports.value()).contains(method);
        }
        return false;
    }
}
