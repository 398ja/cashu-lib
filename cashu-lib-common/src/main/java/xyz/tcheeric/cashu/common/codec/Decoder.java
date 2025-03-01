package xyz.tcheeric.cashu.common.codec;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface Decoder<T> {

    T decode() throws JsonProcessingException;
}
