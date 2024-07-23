package cashu.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
public class TokenV4 extends AbstractBaseToken {

    @JsonProperty("m")
    private String mintUrl;

    @JsonProperty("u")
    private String unit;

    @JsonProperty("d")
    private String memo;

    @JsonProperty("t")
    private Set<TokenData> tokenDataList = new HashSet<>();

    public TokenV4() {
        this(true);
    }

    public TokenV4(boolean clickable) {
        super(Version.V4, TOKEN_PREFIX, clickable);
    }


    @Data
    @NoArgsConstructor
    static class TokenData {
        @JsonProperty("i")
        private String keySetId;

        @JsonProperty("p")
        private Set<TokenProof> proofs = new HashSet<>();

        public void addProofs(@NonNull TokenProof... proofs) {
            Collections.addAll(this.proofs, proofs);
        }

        @Data
        @NoArgsConstructor
        static class TokenProof {
            @JsonProperty("a")
            private Integer amount;

            @JsonProperty("s")
            private String secret;

            @JsonProperty("c")
            private String signature;

            @JsonProperty("v")
            private DLEQProof dleqProof;

            @JsonProperty("w")
            private String witness;

            @Data
            @NoArgsConstructor
            private static class DLEQProof {
                @JsonProperty
                private String e;

                @JsonProperty
                private String s;

                @JsonProperty
                private String r;
            }
        }
    }
}
