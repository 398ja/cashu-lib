package xyz.tcheeric.cashu.common.model.rest;

import xyz.tcheeric.cashu.common.model.ActiveKeySet;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@NoArgsConstructor
public class ActiveKeySetResponse {
    private List<ActiveKeySet> keysets;

    public ActiveKeySetResponse(@NonNull List<ActiveKeySet> keysets) {
        this.keysets = keysets;
    }
}
