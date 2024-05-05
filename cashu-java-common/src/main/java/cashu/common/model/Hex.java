package cashu.common.model;

import cashu.common.json.deserializer.HexDeserializer;
import cashu.util.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
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

    @EqualsAndHashCode.Include
    private byte[] bytes;

    @JsonIgnore
    @Setter
    private int length;

    protected Hex(@NonNull String hexStr) {
        this(Utils.hexToBytes(hexStr), hexStr.length());
    }

    protected Hex(@NonNull String hexStr, int length) {
        this(Utils.hexToBytes(hexStr), length);
    }

    protected Hex(@NonNull byte[] bytes, int length) {
        this.bytes = bytes;
        this.length = length;
    }


    public static Hex fromBytes(@NonNull byte[] bytes) {
        return Hex.fromString(Utils.bytesToHex(bytes));
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
        return Utils.bytesToHex(bytes);
    }

    public byte[] toBytes() {
        return bytes;
    }
}