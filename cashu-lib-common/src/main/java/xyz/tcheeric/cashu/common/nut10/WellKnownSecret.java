package xyz.tcheeric.cashu.common.nut10;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.cashu.common.PrivateKey;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
@NoArgsConstructor
@JsonDeserialize(using = WellKnownSecretDeserializer.class)
@JsonSerialize(using = WellKnownSecretSerializer.class)
public abstract class WellKnownSecret implements Secret {

    /**
     * NUT-10 spending-condition kinds.
     *
     * <p><strong>These names are the wire format.</strong> {@code WellKnownSecretSerializer}
     * writes {@code getKind().name()} and {@code WellKnownSecretDeserializer} reads
     * {@code Kind.valueOf(...)}, so renaming a member changes what every mint and wallet sees.
     *
     * <p>{@code P2PK} and {@code HTLC} are specified upstream (NUT-11, NUT-14). {@code VOUCHER}
     * and {@code P2PK_VOUCHER} are Imani extensions; NUT-10 sanctions this — it defines only the
     * envelope and leaves the condition types to other documents — but note its caution that a
     * mint which does not know a kind "may treat proofs as regular anyone-can-spend tokens".
     */
    public enum Kind {
        P2PK,
        HTLC,
        VOUCHER,
        /**
         * A voucher that is also P2PK-locked: issuer-signed voucher metadata in the tags, and a
         * spending key in {@code data} that the mint requires a witness signature from.
         *
         * <p>Named for the mechanism rather than the payload, as {@code P2PK} and {@code HTLC}
         * are, so a mint implementer can see that a witness is involved. A bare {@code VOUCHER}
         * secret carrying P2PK tags would not convey that, and the mint would dispatch it to a
         * voucher-only condition that never checks the witness.
         */
        P2PK_VOUCHER
    }

    private Kind kind;

    @Setter(lombok.AccessLevel.NONE)
    private String nonce;

    @Setter(lombok.AccessLevel.NONE)
    private byte[] data;

    @Setter(lombok.AccessLevel.NONE)
    private List<Tag> tags;

    /**
     * The exact secret string this object was parsed from, when it was parsed from one.
     *
     * <p>NUT-11 requires that "the message to sign MUST be constructed using the unescaped secret
     * string", and NUT-00 derives {@code Y = hash_to_curve(secret)} from that same string. Both
     * therefore commit to the bytes that actually arrived, not to whatever this library would emit
     * for an equivalent object. Re-serializing a received secret is how key order, whitespace,
     * hex case and escaping turn into an unspendable proof, so a received secret is never
     * re-encoded: {@link #toString()} replays the original.
     *
     * <p>It is cleared by every mutator, because a mutated secret is no longer the one that
     * arrived and replaying the original would misdescribe it.
     */
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private transient String wireString;

    public WellKnownSecret(@NonNull Kind kind) {
        this.kind = kind;
        this.tags = new ArrayList<>();
    }

    public WellKnownSecret(@NonNull Kind kind, byte[] data) {
        this.kind = kind;
        this.data = data;
        try (PrivateKey randomKey = PrivateKey.generateRandom()) {
            this.nonce = randomKey.toString();
        }
        this.tags = new ArrayList<>();
    }

    /**
     * Records the secret string this object was parsed from.
     *
     * <p>Called by the parsers, which are the only places the original string exists. Passing
     * {@code null} is a no-op, so a caller that does not have the original simply leaves the
     * secret to be encoded canonically.
     */
    public void rememberWireString(String wireString) {
        this.wireString = wireString;
    }

    /**
     * The exact secret string this object was parsed from, or {@code null} if it was constructed
     * rather than parsed, or has been mutated since.
     */
    @JsonIgnore
    public String getWireString() {
        return this.wireString;
    }

    /**
     * Discards the remembered wire string. Every mutator calls this.
     */
    protected void forgetWireString() {
        this.wireString = null;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
        forgetWireString();
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
        forgetWireString();
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
        if (tags != null) {
            tags.forEach(tag -> tag.attachTo(this));
        }
        forgetWireString();
    }

    @Override
    public byte[] getData() {
        return this.data;
    }

    @Override
    public void setData(@NonNull byte[] data) {
        this.data = data;
        forgetWireString();
    }

    @Override
    public byte[] toBytes() {
        return this.data;
    }

    public void addTag(@NonNull String key, @NonNull List<Object> values) {
        Tag tag = new Tag(key);
        tag.getValues().addAll(asTagStrings(values));
        this.tags.add(tag);
        tag.attachTo(this);
        forgetWireString();
    }

    /**
     * Renders tag values the way NUT-10 stores them, which is always as strings.
     *
     * <p>NUT-11 spells this out for the integer-valued tags: {@code n_sigs} and {@code locktime}
     * travel as {@code "2"} and {@code "42"}, and a wallet casts them on the way in. Converting
     * here rather than in the serializer keeps a constructed secret equal to the same secret
     * parsed back, which is what a round-trip test is really asserting, and stops the object model
     * holding a value the wire form cannot represent.
     */
    private static List<Object> asTagStrings(List<Object> values) {
        return values.stream().map(value -> (Object) String.valueOf(value)).toList();
    }

    public void addTag(@NonNull Tag tag) {
        this.tags.add(tag);
        tag.attachTo(this);
        forgetWireString();
    }

    public void removeTag(@NonNull Tag tag) {
        this.tags.remove(tag);
        tag.attachTo(null);
        forgetWireString();
    }

    public Tag getTag(@NonNull String key) {
        return this.tags.stream().filter(tag -> tag.getKey().equals(key)).findFirst().orElse(null);
    }

    public void setTag(@NonNull String key, @NonNull List<Object> values) {
        Tag tag = getTag(key);
        if (tag == null) {
            addTag(key, values);
        } else {
            tag.getValues().clear();
            tag.getValues().addAll(asTagStrings(values));
            forgetWireString();
        }
    }

    /**
     * The NUT-10 secret string: the one this object was parsed from if it was parsed from one, and
     * otherwise the spec's {@code [kind, {nonce, data, tags}]} encoding of its current state.
     */
    @SneakyThrows
    @Override
    public String toString() {
        return this.wireString != null
                ? this.wireString
                : JsonUtils.JSON_MAPPER.writeValueAsString(this);
    }


    /**
     * A NUT-10 tag.
     *
     * <h2>Why a tag knows its owner</h2>
     *
     * <p>{@link WellKnownSecret} caches the exact string it was parsed from, so that hashing and
     * signing use the bytes that actually arrived. Every mutator on the secret discards that
     * cache. A tag, however, is reachable through {@code getTags()} and was independently
     * mutable, so changing a tag's values left the secret returning a {@code toString()} that no
     * longer described it (audit L-7). Since {@code toString()} is what {@code hash_to_curve}
     * consumes, that is a secret whose identity and content disagree.
     *
     * <p>A tag therefore holds a back-reference to the secret it belongs to, set when it is
     * attached, and invalidates through it. Tags built during parsing have no owner yet, which is
     * correct: there is nothing cached to invalidate until the parse completes.
     */
    @Data
    @JsonDeserialize(using = TagDeserializer.class)
    @JsonSerialize(using = TagSerializer.class)
    public static class Tag {
        private String key;
        private List<Object> values;

        /** The secret this tag is attached to, if any. Not part of the tag's value. */
        @lombok.Getter(lombok.AccessLevel.NONE)
        @lombok.Setter(lombok.AccessLevel.NONE)
        @lombok.EqualsAndHashCode.Exclude
        @lombok.ToString.Exclude
        @JsonIgnore
        private transient WellKnownSecret owner;

        public Tag() {
            this.values = new ArrayList<>();
        }

        public Tag(@NonNull String key) {
            this.key = key;
            this.values = new ArrayList<>();
        }

        /** Attaches this tag to a secret, so that later mutation invalidates its wire string. */
        void attachTo(WellKnownSecret owner) {
            this.owner = owner;
        }

        private void ownerChanged() {
            if (owner != null) {
                owner.forgetWireString();
            }
        }

        public void setKey(String key) {
            this.key = key;
            ownerChanged();
        }

        public void setValues(List<Object> values) {
            this.values = values;
            ownerChanged();
        }

        public void addValue(@NonNull Object value) {
            this.values.add(value);
            ownerChanged();
        }

        public void removeValue(@NonNull Object value) {
            this.values.remove(value);
            ownerChanged();
        }
    }
}
