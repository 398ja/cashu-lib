package xyz.tcheeric.cashu.entities.rest;

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
import xyz.tcheeric.cashu.common.nut02.InputFeeCalculator;
import xyz.tcheeric.cashu.common.nut02.KeySetResolver;
import xyz.tcheeric.cashu.common.nut02.UnknownKeySetException;

import java.util.List;

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
     * Returns the NUT-02 input fee for these inputs, pricing each proof against its own keyset.
     *
     * @param keySetResolver resolves a keyset id to the keyset that issued it
     * @throws UnknownKeySetException when an input names a keyset the resolver does not know
     */
    public int getFees(@NonNull KeySetResolver keySetResolver) throws UnknownKeySetException {
        return new InputFeeCalculator(keySetResolver).calculateFee(inputs);
    }

}
