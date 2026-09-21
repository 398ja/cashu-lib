package xyz.tcheeric.cashu.entities.rest.nut07;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.HashToCurveSecret;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostCheckStateResponse {

    @JsonProperty
    private List<ResponseState> states;

    public PostCheckStateResponse() {
        this.states = new ArrayList<>();
    }

    public void addResponseState(@NonNull ResponseState responseState) {
        this.states.add(responseState);
    }

    @Data
    @NoArgsConstructor
    public static class ResponseState {

        @JsonProperty("Y")
        private HashToCurveSecret hashToCurveSecret;

        @JsonProperty
        private String state;

        @JsonProperty
        private String witness;
    }
}
