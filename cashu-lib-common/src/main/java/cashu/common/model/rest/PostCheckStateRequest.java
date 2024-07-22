package cashu.common.model.rest;

import cashu.common.model.CryptoElement;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostCheckStateRequest {

    @JsonProperty("Ys")
    private List<CryptoElement> hashToCurveSecrets;
}
