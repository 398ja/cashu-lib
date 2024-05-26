package cashu.common.model;

import cashu.common.json.deserializer.KeysDeserializer;
import cashu.common.json.serializer.KeysSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Getter
@JsonDeserialize(using = KeysDeserializer.class)
@JsonSerialize(using = KeysSerializer.class)
@AllArgsConstructor
public class Keys implements Archivable {

    private final Map<BigInteger, PublicKey> values = new HashMap<>();

    public Keys put(BigInteger key, PublicKey value) {
        values.put(key, value);
        return this;
    }

    public PublicKey get(int key) {
        return values.get(BigInteger.valueOf(key));
    }
}
