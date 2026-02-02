package xyz.tcheeric.cashu.entities.rest.nut07;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.HashToCurveSecret;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostCheckStateRequest {

    /**
     * Maximum number of secrets to check per request to prevent DoS attacks.
     */
    public static final int MAX_SECRETS = 1000;

    @JsonProperty("Ys")
    @NotNull(message = "Secrets are required")
    @NotEmpty(message = "At least one secret is required")
    @Size(max = MAX_SECRETS, message = "Maximum " + MAX_SECRETS + " secrets allowed")
    @Valid
    private List<HashToCurveSecret> hashToCurveSecrets;
}
