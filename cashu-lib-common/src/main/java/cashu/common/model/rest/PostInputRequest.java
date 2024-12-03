package cashu.common.model.rest;

import cashu.common.model.KeySet;
import cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public abstract class PostInputRequest {

    @JsonProperty("inputs")
    private List<Proof> inputs;

    public int getFees(@NonNull KeySet keySet) {
        int sum_fees = 0;
        for (Proof proof : inputs) {
            String keysetId = proof.getKeySetId();

            assert keysetId.equals(keySet.getId()) : "Keyset and proof keyset id do not match";

            sum_fees += keySet.getPartPerThousand();
        }

        return Math.floorDiv (sum_fees + 999, 1000);
    }

}
