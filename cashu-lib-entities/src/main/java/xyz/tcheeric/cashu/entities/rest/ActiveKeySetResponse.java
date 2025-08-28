package xyz.tcheeric.cashu.entities.rest;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.ActiveKeySet;

@Data
@NoArgsConstructor
public class ActiveKeySetResponse {
  private List<ActiveKeySet> keysets;

  public ActiveKeySetResponse(@NonNull List<ActiveKeySet> keysets) {
    this.keysets = keysets;
  }
}
