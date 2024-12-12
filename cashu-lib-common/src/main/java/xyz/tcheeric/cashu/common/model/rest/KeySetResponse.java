package xyz.tcheeric.cashu.common.model.rest;

import xyz.tcheeric.cashu.common.model.KeySet;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@NoArgsConstructor
public class KeySetResponse {

    private List<KeySet> keysets;

    public KeySetResponse(@NonNull List<KeySet> keysets) {
        this.keysets = keysets;
    }
}