package xyz.tcheeric.cashu.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
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
    private List<Contact> contacts;

    @JsonProperty
    private String motd;

    @JsonProperty
    private Map<String, NutConfig> nuts = new HashMap<>();

    public void put(@NonNull String key, @NonNull NutConfig value) {
        nuts.put(key, value);
    }

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

        @JsonProperty
        private String info;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NutConfig {

        @JsonProperty
        private Set<Method> methods = new HashSet<>();

        @JsonProperty
        private Boolean disabled;

        @JsonProperty
        private Boolean supported;

        public void addMethod(Method... methods) {
            Collections.addAll(this.methods, methods);
        }

        @Data
        @NoArgsConstructor
        public static class Method {

            @JsonProperty
            private String method;

            @JsonProperty
            private String unit;

            @JsonProperty("min_amount")
            private int minAmount;

            @JsonProperty("max_amount")
            private int maxAmount;
        }
    }
}