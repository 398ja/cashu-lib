package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import xyz.tcheeric.cashu.common.json.deserializer.KeysDeserializer;
import xyz.tcheeric.cashu.common.json.serializer.KeysSerializer;

@JsonDeserialize(using = KeysDeserializer.class)
@JsonSerialize(using = KeysSerializer.class)
public class Keys {

  private final Map<BigInteger, PublicKey> values = new HashMap<>();

  public Keys() {}

  public Keys put(BigInteger key, PublicKey value) {
    values.put(key, value);
    return this;
  }

  public PublicKey get(int key) {
    return values.get(BigInteger.valueOf(key));
  }

  public Map<BigInteger, PublicKey> getValues() {
    return Collections.unmodifiableMap(values);
  }

  public Map<BigInteger, byte[]> values() {
    Map<BigInteger, byte[]> keys = new HashMap<>();
    for (Map.Entry<BigInteger, PublicKey> entry : values.entrySet()) {
      keys.put(entry.getKey(), entry.getValue().toBytes());
    }
    return keys;
  }
}
