package xyz.tcheeric.cashu.entities.rest.nut05;

/**
 * NUT-05 melt quote lifecycle state.
 *
 * <p>{@code PENDING} is the reason this enum exists: an in-flight Lightning payment is neither
 * unpaid nor paid, and a wallet that cannot tell the difference may retry a payment that is still
 * live. The deprecated {@code paid} boolean cannot express it.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/05.md">NUT-05</a>
 */
public enum MeltQuoteState {

    UNPAID,
    PENDING,
    PAID;

    /**
     * The state implied by the deprecated {@code paid} boolean, for a caller that has nothing else.
     * A boolean can never mean {@link #PENDING}.
     */
    public static MeltQuoteState fromPaidFlag(boolean paid) {
        return paid ? PAID : UNPAID;
    }

    /**
     * Whether this state means the payment has settled, for the deprecated {@code paid} boolean.
     */
    public boolean isPaid() {
        return this == PAID;
    }
}
