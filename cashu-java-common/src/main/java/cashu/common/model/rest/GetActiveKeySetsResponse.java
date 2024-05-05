package cashu.common.model.rest;

import cashu.common.model.ActiveKeySet;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.List;

@AllArgsConstructor
public class GetActiveKeySetsResponse {

    private final List<ActiveKeySet> activeKeySets;

    public void addActiveKeySet(@NonNull ActiveKeySet activeKeySet) {
        activeKeySets.add(activeKeySet);
    }
}
