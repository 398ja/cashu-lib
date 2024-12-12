package xyz.tcheeric.cashu.common.model.rest;

import xyz.tcheeric.cashu.common.model.ActiveKeySet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetActiveKeySetsResponse {

    private List<ActiveKeySet> activeKeySets;

    public void addActiveKeySet(@NonNull ActiveKeySet activeKeySet) {
        activeKeySets.add(activeKeySet);
    }
}
