package xyz.tcheeric.cashu.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;


@Data
@JsonPropertyOrder({"detail", "code"})
public class Error {

    @JsonProperty("detail")
    private String key;

    @JsonProperty
    private int code;
}
