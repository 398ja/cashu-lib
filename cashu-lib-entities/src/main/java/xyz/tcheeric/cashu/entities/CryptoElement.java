package xyz.tcheeric.cashu.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class CryptoElement {

    protected static final int PRIVATE_KEY_LENGTH = 64;
    protected static final int PUBLIC_KEY_LENGTH = 66;
    protected static final int SIGNATURE_LENGTH = 66;
    protected static final int SECRET_LENGTH = 64;


    @EqualsAndHashCode.Include
    private byte[] bytes;

    @JsonIgnore
    @Setter
    private int length;

    protected CryptoElement(@NonNull String hexStr, int length) {
        this(Hex.decode(hexStr), length);
    }

    protected CryptoElement(byte[] bytes, int length) {
        this.bytes = bytes;
        this.length = length;
        validate();
    }

    public BigInteger toBigInteger() {
        return Utils.bigIntFromBytes(bytes);
    }

    public boolean equalsIgnoreCase(@NonNull CryptoElement cryptoElement) {
        return this.toString().equalsIgnoreCase(cryptoElement.toString());
    }

    @JsonValue
    @Override
    public String toString() {
        return Hex.toHexString(bytes);
    }

    public byte[] toBytes() {
        return bytes;
    }

    private void validate() {
        if (2 * bytes.length != length) {
            throw new IllegalArgumentException("Invalid length: " + this + "(" + bytes.length + ")");
        }
    }
}