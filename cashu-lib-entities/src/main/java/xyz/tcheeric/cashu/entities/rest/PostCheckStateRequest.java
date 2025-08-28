package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.CryptoElement;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostCheckStateRequest {

  @JsonProperty("Ys")
  private List<CryptoElement> hashToCurveSecrets;
}
