package xyz.tcheeric.cashu.entities.rest;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.ActiveKeySet;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetActiveKeySetsResponse {

  private List<ActiveKeySet> activeKeySets;

  public void addActiveKeySet(@NonNull ActiveKeySet activeKeySet) {
    activeKeySets.add(activeKeySet);
  }
}
