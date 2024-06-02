package cashu.common.model.rest;

import cashu.common.model.BlindSignature;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
public class PostMintResponse {

    @JsonProperty("signatures")
    private List<BlindSignature> blindSignatures;

    public PostMintResponse () {
        this.blindSignatures = new ArrayList<>();
    }

    public void addBlindSignature(@NonNull BlindSignature blindSignature) {
        this.blindSignatures.add(blindSignature);
    }
}
