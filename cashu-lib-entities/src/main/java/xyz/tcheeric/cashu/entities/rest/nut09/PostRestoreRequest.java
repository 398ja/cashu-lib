package xyz.tcheeric.cashu.entities.rest.nut09;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.BlindedMessage;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostRestoreRequest {

    /**
     * Maximum number of outputs allowed per request to prevent DoS attacks.
     */
    public static final int MAX_OUTPUTS = 1000;

    @JsonProperty("outputs")
    @NotNull(message = "Outputs are required")
    @NotEmpty(message = "At least one output is required")
    @Size(max = MAX_OUTPUTS, message = "Maximum " + MAX_OUTPUTS + " outputs allowed")
    @Valid
    private List<BlindedMessage> blindedMessages;
}
