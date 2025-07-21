package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.CryptoElement;

import java.util.ArrayList;
import java.util.List;

@Data
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
        private CryptoElement hashToCurveSecret;

        @JsonProperty
        private String state;

        @JsonProperty
        private String witness;
    }
}
