package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Discrete Log Equality (DLEQ) proof for NUT-12 offline signature verification.
 *
 * <p>Contains the challenge {@code e} and response {@code s} scalars that prove
 * the mint used the same private key for its public key and for signing a blinded
 * message. Proofs attached to user-to-user transfers also include the blinding
 * factor {@code r} so the recipient can reconstruct the blinded points.
 */
@Data
@NoArgsConstructor
@JsonPropertyOrder({"e", "s", "r"})
public class DLEQProof {

    @JsonProperty("e")
    private String e;

    @JsonProperty("s")
    private String s;

    @JsonProperty("r")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String r;

    /**
     * Builder-based constructor that validates required scalars.
     */
    @Builder
    public DLEQProof(@NonNull String e, @NonNull String s, String r) {
        setE(e);
        setS(s);
        setR(r);
    }

    /**
     * Creates a DLEQ proof for BlindSignature payloads (no blinding factor).
     */
    public static DLEQProof forBlindSignature(@NonNull String e, @NonNull String s) {
        return DLEQProof.builder()
                .e(e)
                .s(s)
                .build();
    }

    /**
     * Creates a DLEQ proof for Proof payloads (includes blinding factor).
     */
    public static DLEQProof forProof(@NonNull String e, @NonNull String s, @NonNull String r) {
        return DLEQProof.builder()
                .e(e)
                .s(s)
                .r(r)
                .build();
    }

    /**
     * Checks if this proof includes the optional blinding factor.
     *
     * @return true when {@code r} is provided
     */
    public boolean hasBlindingFactor() {
        return r != null && !r.isEmpty();
    }

    public void setE(@NonNull String e) {
        this.e = validateScalar("e", e);
    }

    public void setS(@NonNull String s) {
        this.s = validateScalar("s", s);
    }

    public void setR(String r) {
        if (r == null) {
            this.r = null;
            return;
        }
        this.r = validateScalar("r", r);
    }

    private String validateScalar(String name, String scalar) {
        if (scalar.isBlank()) {
            throw new IllegalArgumentException("DLEQ " + name + " must not be blank. Got: '" + scalar + "'");
        }
        if (!scalar.matches("^[0-9a-fA-F]+$")) {
            throw new IllegalArgumentException("DLEQ " + name + " must be hexadecimal. Got: " + scalar);
        }
        if (scalar.length() != 64) {
            throw new IllegalArgumentException("DLEQ " + name + " must be 64 hex characters. Got: " + scalar);
        }
        return scalar.toLowerCase();
    }
}
