package xyz.tcheeric.cashu.entities.rest.nut04;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostMintQuoteRequest {

    @JsonProperty
    private int amount;

    @JsonProperty
    private String unit;

    /**
     * NUT-20 — the compressed secp256k1 key that will be required to sign the mint request.
     *
     * <p>Optional, and absent means the quote is unlocked: NUT-04 warns that such a quote can be
     * minted by anyone who learns its id, since the id is the only thing standing between a paid
     * quote and its ecash. A wallet SHOULD use a fresh key per quote, so the mint cannot link a
     * wallet's quotes to each other.
     */
    @JsonProperty("pubkey")
    private String pubkey;

    /** An unlocked quote, which anyone knowing its id can mint. See {@link #pubkey}. */
    public PostMintQuoteRequest(int amount, String unit) {
        this(amount, unit, null);
    }

    protected PostMintQuoteRequest(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0. Got: " + amount);
        }
        this.amount = amount;
    }
}
