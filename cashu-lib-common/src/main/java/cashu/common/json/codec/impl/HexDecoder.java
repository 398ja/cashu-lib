package cashu.common.json.codec.impl;

import cashu.common.json.codec.Decoder;
import cashu.common.model.Hex;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class HexDecoder implements Decoder<Hex> {

    private final String jsonString;

    @Override
    public Hex decode() {
        return Hex.fromString(jsonString);
    }
}
