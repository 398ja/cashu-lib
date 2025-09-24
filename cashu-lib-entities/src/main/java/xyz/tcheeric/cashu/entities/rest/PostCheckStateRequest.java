package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.BaseKey;
import xyz.tcheeric.cashu.common.CompressedPublicKey;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostCheckStateRequest {

    @JsonProperty("Ys")
    @JsonDeserialize(contentAs = CompressedPublicKey.class)
    private List<BaseKey> hashToCurveSecrets;
}
