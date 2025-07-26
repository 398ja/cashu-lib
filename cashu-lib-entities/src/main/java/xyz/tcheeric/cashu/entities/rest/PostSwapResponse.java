package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.BlindSignature;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostSwapResponse {

    @JsonProperty("signatures")
    private List<BlindSignature> blindSignatures;
 }
