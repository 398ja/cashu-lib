package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostMeltQuoteBolt11Request extends PostMeltQuoteRequest {

    @JsonProperty
    private String unit;

    public PostMeltQuoteBolt11Request(@NonNull String request, @NonNull String unit) {
        super(request);
        this.unit = unit;
    }
}
