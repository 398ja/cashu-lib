package xyz.tcheeric.cashu.entities.codec;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface Encoder<T> {

    String encode() throws JsonProcessingException;
}
