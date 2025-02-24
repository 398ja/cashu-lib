package xyz.tcheeric.cashu.common.codec;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface Encoder<T> {

    String encode() throws JsonProcessingException;
}
