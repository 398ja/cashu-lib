package xyz.tcheeric.cashu.common.model.rest;

import xyz.tcheeric.cashu.common.model.KeySet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetKeySetsResponse {

    private List<KeySet> keySets;

    public void addActiveKeySet(@NonNull KeySet keySet) {
        keySets.add(keySet);
    }

}
