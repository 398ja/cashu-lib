package xyz.tcheeric.cashu.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import xyz.tcheeric.cashu.common.json.deserializer.TagDeserializer;
import xyz.tcheeric.cashu.common.json.deserializer.WellKnownSecretDeserializer;

import java.util.ArrayList;
import java.util.List;

@Data
@Log
@NoArgsConstructor
@JsonDeserialize(using = WellKnownSecretDeserializer.class)
public abstract class WellKnownSecret implements Secret {

    public enum Kind {
        P2PK,
        HTLC
    }

    private Kind kind;
    private String nonce;
    private PublicKey data;
    private List<Tag> tags;

    public WellKnownSecret(@NonNull Kind kind, @NonNull PublicKey data) {
        this.kind = kind;
        this.data = data;
        this.nonce = PrivateKey.generateRandom().toString();
        this.tags = new ArrayList<>();
    }

    @Override
    public String getData() {
        return this.data.toString();
    }

    @Override
    public void setData(@NonNull String data) {
        this.data = PublicKey.fromString(data);
    }

    @Override
    public byte[] toBytes() {
        return this.data.toBytes();
    }

    public void addTag(@NonNull String key, @NonNull List<Object> values) {
        Tag tag = new Tag(key);
        tag.getValues().addAll(values);
        this.tags.add(tag);
    }

    public void addTag(@NonNull Tag tag) {
        this.tags.add(tag);
    }

    public void removeTag(@NonNull Tag tag) {
        this.tags.remove(tag);
    }

    @SneakyThrows
    @Override
    public String toString() {
        return new ObjectMapper().writeValueAsString(this).replace("\"", "\\\"");
    }


    @Data
    @JsonDeserialize(using = TagDeserializer.class)
    public static class Tag {
        private String key;
        private List<Object> values;

        public Tag() {
            this(null);
        }

        public Tag(@NonNull String key) {
            this.key = key;
            this.values = new ArrayList<>();
        }

        public void addValue(@NonNull Object value) {
            this.values.add(value);
        }

        public void removeValue(@NonNull Object value) {
            this.values.remove(value);
        }
    }
}
