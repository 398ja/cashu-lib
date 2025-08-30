package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.Keys;
import xyz.tcheeric.cashu.common.PublicKey;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class KeysTest {

    @Test
    public void valuesAreUnmodifiable() {
        Keys keys = new Keys();
        keys.put(BigInteger.ONE, PublicKey.fromString("03a40f20667ed53513075dc51e715ff2046cad64eb68960632269ba7f0210e38bc"));
        assertThrows(UnsupportedOperationException.class, () ->
                keys.getValues().put(BigInteger.TWO, PublicKey.fromString("03fd4ce5a16b65576145949e6f99f445f8249fee17c606b688b504a849cdc452de"))
        );
    }
}
