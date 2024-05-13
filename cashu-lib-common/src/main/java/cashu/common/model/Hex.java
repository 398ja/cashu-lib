package cashu.common.model;

import cashu.common.json.deserializer.HexDeserializer;
import cashu.util.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@JsonDeserialize(using = HexDeserializer.class)
public class Hex {

    protected static final int PRIVATE_KEY_LENGTH = 64;
    protected static final int PUBLIC_KEY_LENGTH = 66;
    protected static final int SIGNATURE_LENGTH = 66;
    protected static final int SECRET_LENGTH = 64;

    @EqualsAndHashCode.Include
    private byte[] bytes;

    @JsonIgnore
    @Setter
    private int length;

    protected Hex(@NonNull String hexStr) {
        this(Utils.hexStringToBytes(hexStr), hexStr.length());
    }

    protected Hex(@NonNull String hexStr, int length) {
        this(Utils.hexStringToBytes(hexStr), length);
    }

    protected Hex(@NonNull byte[] bytes, int length) {
        this.bytes = bytes;
        this.length = length;
    }


    public static Hex fromBytes(@NonNull byte[] bytes) {
        return Hex.fromString(Utils.bytesToHexString(bytes));
    }

    public static Hex fromString(@NonNull String s) {
        if (s.matches("[0-9A-Fa-f]+") == false) {
            throw new IllegalArgumentException(String.format("Invalid hex string: %s", s));
        }

        return new Hex(s);
    }

    public static Hex fromBigInteger(@NonNull BigInteger bigInteger) {
        return Hex.fromBytes(bigInteger.toByteArray());
    }

    public BigInteger toBigInteger() {
        return new BigInteger(bytes);
    }

    public boolean equalsIgnoreCase(@NonNull Hex hex) {
        return this.toString().equalsIgnoreCase(hex.toString());
    }

    @JsonValue
    @Override
    public String toString() {
        return Utils.bytesToHexString(bytes);
    }

    public byte[] toBytes() {
        return bytes;
    }
}