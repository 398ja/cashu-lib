package cashu.common.model.rest;

import cashu.common.model.BlindSignature;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PostMintResponse {

    @JsonProperty("signatures")
    private final List<BlindSignature> blindSignatures;
}
