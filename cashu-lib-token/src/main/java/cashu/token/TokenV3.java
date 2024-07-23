package cashu.token;

import cashu.common.model.Proof;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
public class TokenV3 extends AbstractBaseToken {

    @JsonProperty("token")
    private Set<MintProof> mintProofs = new HashSet<>();

    @JsonProperty
    private String unit;

    @JsonProperty
    private String memo;

    public TokenV3() {
        this(true);
    }

    public TokenV3(boolean clickable) {
        this(clickable, "", "");
    }

    public TokenV3(boolean clickable, @NonNull String unit, @NonNull String memo) {
        super(Version.V3, TOKEN_PREFIX, clickable);
        this.unit = unit;
        this.memo = memo;
    }

    public void addMintProof(@NonNull MintProof... mintProofs) {
        Collections.addAll(this.mintProofs, mintProofs);
    }

    // TODO: Implement this method
    public TokenV4 convert() {
        return null;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    static class MintProof {

        @JsonProperty
        private String mint;

        @JsonProperty
        @Builder.Default
        private final Set<Proof> proofs = new HashSet<>();

        public void addProofs(@NonNull Proof... proofs) {
            Collections.addAll(this.proofs, proofs);
        }

        // TODO - Implement this method
        public TokenV4.TokenData convert() {
            return null;
        }
    }
}
