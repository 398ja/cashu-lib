package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.BaseKey;
import xyz.tcheeric.cashu.common.CompressedPublicKey;
import xyz.tcheeric.cashu.common.PublicKey;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostCheckStateRequest {

    @JsonProperty("Ys")
    private List<PublicKey> hashToCurveSecrets;
}
