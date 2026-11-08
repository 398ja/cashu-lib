package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.nut00.CashuErrorCode;
import xyz.tcheeric.cashu.common.nut02.InputFeeCalculator;
import xyz.tcheeric.cashu.common.nut02.KeySetResolver;
import xyz.tcheeric.cashu.common.nut02.UnknownKeySetException;
import xyz.tcheeric.cashu.common.util.CashuErrorException;
import xyz.tcheeric.cashu.common.voucher.VoucherDocuments;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class PostInputRequest<T extends Secret> {

    /**
     * Maximum number of inputs allowed per request to prevent DoS attacks.
     */
    public static final int MAX_INPUTS = 1000;

    @JsonProperty("inputs")
    @NotNull(message = "Inputs are required")
    @NotEmpty(message = "At least one input is required")
    @Size(max = MAX_INPUTS, message = "Maximum " + MAX_INPUTS + " inputs allowed")
    @Valid
    private List<Proof<T>> inputs;

    /**
     * Voucher documents for by-reference inputs (cashu-voucher#64): {@code hex(SHA256(D)) -> hex(D)}.
     * Omitted when empty, so requests without documents serialize exactly as before.
     */
    @JsonProperty("vouchers")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Size(max = MAX_INPUTS, message = "Maximum " + MAX_INPUTS + " voucher documents allowed")
    private Map<String, String> vouchers = new TreeMap<>();

    protected PostInputRequest(List<Proof<T>> inputs) {
        this.inputs = inputs;
    }

    /**
     * Decodes and checks the carried voucher documents. Entries no input references are kept here;
     * the caller ignores them.
     *
     * @return key to document bytes, sorted by key; empty when the request carries none
     * @throws CashuErrorException with {@link CashuErrorCode#voucher_document_invalid} when a key is
     *                             malformed or does not hash its value, a value is empty, not hex or
     *                             larger than {@link VoucherDocuments#MAX_DOCUMENT_BYTES}, or there are
     *                             more documents than inputs
     */
    public SortedMap<String, byte[]> voucherDocuments() throws CashuErrorException {
        try {
            return VoucherDocuments.fromHex(vouchers == null ? Map.of() : vouchers, inputs == null ? 0 : inputs.size());
        } catch (IllegalArgumentException e) {
            throw new CashuErrorException(CashuErrorCode.voucher_document_invalid, e.getMessage(), e);
        }
    }

    /**
     * Returns the NUT-02 input fee for these inputs, pricing each proof against its own keyset.
     *
     * @param keySetResolver resolves a keyset id to the keyset that issued it
     * @throws UnknownKeySetException when an input names a keyset the resolver does not know
     */
    public int getFees(@NonNull KeySetResolver keySetResolver) throws UnknownKeySetException {
        return new InputFeeCalculator(keySetResolver).calculateFee(inputs);
    }

}
