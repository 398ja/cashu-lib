package cashu.common.model.rest;

import cashu.common.model.BlindSignature;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class PostSwapResponse {

    @JsonProperty("signatures")
    private final List<BlindSignature> blindSignatures;
 }
