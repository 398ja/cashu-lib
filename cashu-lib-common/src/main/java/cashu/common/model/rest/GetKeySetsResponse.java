package cashu.common.model.rest;

import cashu.common.model.KeySet;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.List;

@AllArgsConstructor
public class GetKeySetsResponse {

    private final List<KeySet> keySets;

    public void addActiveKeySet(@NonNull KeySet keySet) {
        keySets.add(keySet);
    }

}
