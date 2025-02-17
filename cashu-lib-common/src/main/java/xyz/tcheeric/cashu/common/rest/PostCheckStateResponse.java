package xyz.tcheeric.cashu.common.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.entities.CryptoElement;

import java.util.ArrayList;
import java.util.List;

@Data
public class PostCheckStateResponse {

    @JsonProperty
    private List<ResponseSatate> states;

    public PostCheckStateResponse() {
        this.states = new ArrayList<>();
    }

    public void addResponseState(@NonNull ResponseSatate responseSatate) {
        this.states.add(responseSatate);
    }

    @Data
    @NoArgsConstructor
    public static class ResponseSatate {

        @JsonProperty("Y")
        private CryptoElement hashToCurveSecret;

        @JsonProperty
        private String state;

        @JsonProperty
        private String witness;
    }
}
