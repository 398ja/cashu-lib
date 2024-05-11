package cashu.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Data
public class MintInformation {

    @JsonProperty
    private String name;

    @JsonProperty("pubkey")
    private PublicKey publicKey;

    @JsonProperty
    private String version;

    @JsonProperty
    private String description;

    @JsonProperty("description_long")
    private String descriptionLong;

    @JsonProperty
    private Map<String, String> contacts;

    @JsonProperty
    private String motd;

    @JsonProperty
    private Map<String, NutConfig> nuts;

    public void addNut(@NonNull Integer nut, @NonNull NutConfig config) {
        if (nuts.containsKey(nut.toString())) {
            return;
        }
        nuts.put(nut.toString(), config);
    }

    public interface NutConfig {
    }

    @Data
    @NoArgsConstructor
    public static class NutMethodsConfig implements NutConfig {

        @JsonProperty
        private final List<Method> methods = new ArrayList<>();

        public void addMethod(@NonNull Method method) {
            if (methods.contains(method)) {
                return;
            }
            methods.add(method);
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Method {

            @JsonProperty
            private Map<String, String> methodInfo;

            @JsonProperty
            private boolean disabled;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutSupportConfig implements NutConfig {
        @JsonProperty
        private boolean supported = false;
    }
}
