package xyz.tcheeric.cashu.entities.rest.nut07;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.HashToCurveSecret;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostCheckStateRequest {

    @JsonProperty("Ys")
    private List<HashToCurveSecret> hashToCurveSecrets;
}
