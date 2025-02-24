package xyz.tcheeric.cashu.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import xyz.tcheeric.cashu.common.json.deserializer.ErrorDeserializer;
import xyz.tcheeric.cashu.common.json.serializer.ErrorSerializer;

@Data
@JsonSerialize(using = ErrorSerializer.class)
@JsonDeserialize(using = ErrorDeserializer.class)
public class Error {

    @JsonProperty
    private String key;

    @JsonProperty
    private int code;
}
