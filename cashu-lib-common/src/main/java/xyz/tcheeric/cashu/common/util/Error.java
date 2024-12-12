package xyz.tcheeric.cashu.common.util;

import xyz.tcheeric.cashu.common.json.deserializer.ErrorDeserializer;
import xyz.tcheeric.cashu.common.json.serializer.ErrorSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

@Data
@JsonSerialize(using = ErrorSerializer.class)
@JsonDeserialize(using = ErrorDeserializer.class)
public class Error {

    @JsonProperty
    private String key;

    @JsonProperty
    private int code;
}
