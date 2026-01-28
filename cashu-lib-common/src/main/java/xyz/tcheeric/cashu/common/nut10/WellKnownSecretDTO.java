package xyz.tcheeric.cashu.common.nut10;

import lombok.Data;

import java.util.List;

@Data
public class WellKnownSecretDTO {
    private WellKnownSecret.Kind kind;
    private String nonce;
    private String data;
    private List<WellKnownSecret.Tag> tags;
}
