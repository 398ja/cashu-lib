package cashu.common.error;

import cashu.common.json.deserializer.ErrorDeserializer;
import cashu.common.json.serializer.ErrorSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NonNull;

@Data
@JsonSerialize(using = ErrorSerializer.class)
@JsonDeserialize(using = ErrorDeserializer.class)
public class Error {

    public Error(@NonNull Exception e) {
        this.detail = e.getMessage();
    }

    @JsonProperty
    private String detail;

    @JsonProperty
    private int code;
}
