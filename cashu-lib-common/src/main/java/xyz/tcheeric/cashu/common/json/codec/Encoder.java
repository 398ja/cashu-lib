package xyz.tcheeric.cashu.common.json.codec;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface Encoder<T> {

    String encode() throws JsonProcessingException;
}
