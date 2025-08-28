package xyz.tcheeric.cashu.entities.rest;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.KeySet;

@Data
@NoArgsConstructor
public class KeySetResponse {

  private List<KeySet> keysets;

  public KeySetResponse(@NonNull List<KeySet> keysets) {
    this.keysets = keysets;
  }
}
