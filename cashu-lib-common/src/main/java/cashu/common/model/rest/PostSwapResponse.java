package cashu.common.model.rest;

import cashu.common.model.BlindSignature;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostSwapResponse {

    @JsonProperty("signatures")
    private List<BlindSignature> blindSignatures;
 }
