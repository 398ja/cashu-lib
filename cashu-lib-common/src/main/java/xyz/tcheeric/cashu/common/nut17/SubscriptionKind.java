package xyz.tcheeric.cashu.common.nut17;

/**
 * NUT-17 subscription kinds for WebSocket subscriptions.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/17.md">NUT-17 Specification</a>
 */
public enum SubscriptionKind {
    /**
     * Subscribe to bolt11 mint quote state changes (NUT-04).
     */
    bolt11_mint_quote,

    /**
     * Subscribe to bolt11 melt quote state changes (NUT-05).
     */
    bolt11_melt_quote,

    /**
     * Subscribe to proof state changes (NUT-07).
     */
    proof_state
}
