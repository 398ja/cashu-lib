package cashu.common.model.rest;

import cashu.common.model.BlindedMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PostMintRequest {

    @JsonProperty("quote")
    private final String quoteId;

    @JsonProperty("outputs")
    private final List<BlindedMessage> blindedMessages;
}
