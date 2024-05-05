package cashu.common.json.codec;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface Decoder<T> {

    T decode() throws JsonProcessingException;
}
