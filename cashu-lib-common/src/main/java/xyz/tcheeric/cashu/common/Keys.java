package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.json.deserializer.KeysDeserializer;
import xyz.tcheeric.cashu.common.json.serializer.KeysSerializer;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Getter
@JsonDeserialize(using = KeysDeserializer.class)
@JsonSerialize(using = KeysSerializer.class)
@AllArgsConstructor
//@NoArgsConstructor
public class Keys {

    private final Map<BigInteger, PublicKey> values = new HashMap<>();

    public Keys put(BigInteger key, PublicKey value) {
        values.put(key, value);
        return this;
    }

    public PublicKey get(int key) {
        return values.get(BigInteger.valueOf(key));
    }

    public Map<BigInteger, byte[]> values() {
        Map<BigInteger, byte[]> keys = new HashMap<>();
        for (Map.Entry<BigInteger, PublicKey> entry : values.entrySet()) {
            keys.put(entry.getKey(), entry.getValue().toBytes());
        }
        return keys;
    }
}
