package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"id", "unit", "active", "keys", "input_fee_ppk"})
public class KeySet {

    @JsonProperty
    private String id;

    @JsonProperty
    private String unit;

    /**
     * Whether the mint will sign new outputs with this keyset.
     *
     * <p>NUT-01 lists {@code active} among the fields of every keyset in the
     * {@code GET /v1/keys} response, and modern wallets model it as required.
     * Omitting it made the whole response fail deserialisation — a pydantic
     * {@code ValidationError} rather than a graceful degradation — so a wallet
     * could not obtain any keyset and reported "no active keysets found for
     * unit sat (or they are unsupported by this wallet)". The mint looked
     * offline to it.
     *
     * <p>Defaults to {@code true} because {@code GET /v1/keys} returns only
     * active keysets by definition (NUT-01: "The mint responds only with its
     * active keysets"). {@code GET /v1/keysets} carries the real flag per
     * keyset and is modelled separately by {@link ActiveKeySet}.
     */
    @JsonProperty
    @Builder.Default
    private boolean active = true;

    @JsonProperty
    private Keys keys;

    @JsonProperty("input_fee_ppk")
    private int partPerThousand;
}
