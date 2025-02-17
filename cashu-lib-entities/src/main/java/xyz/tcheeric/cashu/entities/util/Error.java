package xyz.tcheeric.cashu.entities.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import xyz.tcheeric.cashu.entities.json.deserializer.ErrorDeserializer;
import xyz.tcheeric.cashu.entities.json.serializer.ErrorSerializer;

@Data
@JsonSerialize(using = ErrorSerializer.class)
@JsonDeserialize(using = ErrorDeserializer.class)
public class Error {

    @JsonProperty
    private String key;

    @JsonProperty
    private int code;
}
