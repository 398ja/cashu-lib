package xyz.tcheeric.cashu.entities.rest.nut05;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostMeltBolt11Response extends PostMeltResponse {
    public PostMeltBolt11Response(boolean paid, String paymentPreimage) {
        super(paid, paymentPreimage);
    }
}
