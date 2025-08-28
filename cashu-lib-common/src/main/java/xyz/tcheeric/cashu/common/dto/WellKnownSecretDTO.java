package xyz.tcheeric.cashu.common.dto;

import java.util.List;
import lombok.Data;
import xyz.tcheeric.cashu.common.WellKnownSecret;

@Data
public class WellKnownSecretDTO {
  private WellKnownSecret.Kind kind;
  private String nonce;
  private String data;
  private List<WellKnownSecret.Tag> tags;
}
