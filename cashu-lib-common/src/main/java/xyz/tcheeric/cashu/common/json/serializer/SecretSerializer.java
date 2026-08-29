package xyz.tcheeric.cashu.common.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import xyz.tcheeric.cashu.common.Secret;

import java.io.IOException;

/**
 * Writes {@code Proof.secret} as a JSON string, which is what NUT-00 defines it to be.
 *
 * <p>A NUT-10 well-known secret is itself JSON, and without this it was emitted as a nested JSON
 * <em>array</em> rather than as a string containing that array. NUT-11 shows the escaped string
 * form explicitly, and a mint reading the array form would compute a different
 * {@code hash_to_curve} preimage, or fail to parse the proof at all.
 *
 * <p>The string written is {@link Secret#toString()}, which replays the original wire string for a
 * secret that was parsed from one.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/00.md">NUT-00</a>
 */
public class SecretSerializer extends JsonSerializer<Secret> {

    @Override
    public void serialize(Secret value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }
}
