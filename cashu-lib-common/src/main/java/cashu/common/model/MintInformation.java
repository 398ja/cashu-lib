package cashu.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MintInformation {
    private String name;
    @JsonProperty("pubkey")
    private PublicKey publicKey;
    private String version;
    private String description;
    @JsonProperty("description_long")
    private String descriptionLong;
    private List<Contact> contacts;
    private String motd;
    private Map<String, NutConfig> nuts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contact {

        public enum ContactMethod {
            EMAIL("email"),
            TELEGRAM("telegram"),
            TWITTER("twitter"),
            GITHUB("github"),
            DISCORD("discord"),
            FACEBOOK("facebook"),
            LINKEDIN("linkedin"),
            WEBSITE("website"),
            NOSTR("nostr"),
            OTHER("other");

            private final String value;

            ContactMethod(String value) {
                this.value = value;
            }

            @JsonValue
            public String getValue() {
                return value;
            }
        }

        @JsonProperty("method")
        private ContactMethod contactMethod;
        private String info;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NutConfig {
        private List<Method> methods;
        private Boolean disabled;
        private Boolean supported;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Method {
            private String method;
            private String unit;
            @JsonProperty("min_amount")
            private int minAmount;
            @JsonProperty("max_amount")
            private int maxAmount;
        }
    }
}