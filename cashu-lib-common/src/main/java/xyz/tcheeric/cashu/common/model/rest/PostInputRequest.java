package xyz.tcheeric.cashu.common.model.rest;

import xyz.tcheeric.cashu.common.model.KeySet;
import xyz.tcheeric.cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.model.Secret;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class PostInputRequest<T extends Secret> {

    @JsonProperty("inputs")
    private List<Proof<T>> inputs;

    public int getFees(@NonNull KeySet keySet) {
        int sum_fees = 0;
        for (Proof<T> proof : inputs) {
            String keysetId = proof.getKeySetId();

            assert keysetId.equals(keySet.getId()) : "Keyset and proof keyset id do not match";

            sum_fees += keySet.getPartPerThousand();
        }

        return Math.floorDiv (sum_fees + 999, 1000);
    }

}
