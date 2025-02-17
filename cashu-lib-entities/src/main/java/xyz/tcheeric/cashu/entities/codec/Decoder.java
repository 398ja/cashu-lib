package xyz.tcheeric.cashu.entities.codec;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface Decoder<T> {

    T decode() throws JsonProcessingException;
}
