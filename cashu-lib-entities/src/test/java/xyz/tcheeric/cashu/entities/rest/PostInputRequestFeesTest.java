package xyz.tcheeric.cashu.entities.rest;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.KeySet;
import xyz.tcheeric.cashu.common.LiteralSecret;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.nut02.KeySetResolver;
import xyz.tcheeric.cashu.common.nut02.UnknownKeySetException;
import xyz.tcheeric.cashu.entities.rest.nut03.PostSwapRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that a request prices its inputs per keyset rather than against one caller-chosen keyset.
 */
class PostInputRequestFeesTest {

    private static final String FIRST_KEYSET_ID = "009a1f293253e41e";
    private static final String SECOND_KEYSET_ID = "0042ade98b2a370a";

    /** Inputs from two keysets are charged each keyset's own fee. */
    @Test
    void shouldChargeEachInputAgainstItsOwnKeysetWhenInputsSpanKeysets() throws Exception {
        Map<String, KeySet> keySets = new LinkedHashMap<>();
        keySets.put(FIRST_KEYSET_ID, keySet(FIRST_KEYSET_ID, 100));
        keySets.put(SECOND_KEYSET_ID, keySet(SECOND_KEYSET_ID, 500));

        PostSwapRequest request = new PostSwapRequest();
        List<Proof<LiteralSecret>> inputs = new ArrayList<>();
        inputs.add(proof(FIRST_KEYSET_ID));
        inputs.add(proof(SECOND_KEYSET_ID));
        request.setInputs(new ArrayList<>(inputs));

        // 100 + 500 = 600 ppk, which rounds up to one unit.
        assertThat(request.getFees(KeySetResolver.of(keySets))).isEqualTo(1);
    }

    /** An input naming a keyset the resolver does not know is a typed protocol error. */
    @Test
    void shouldRaiseUnknownKeysetErrorWhenAnInputNamesAnUnknownKeyset() {
        PostSwapRequest request = new PostSwapRequest();
        request.setInputs(new ArrayList<>(List.of(proof(SECOND_KEYSET_ID))));

        assertThatThrownBy(() -> request.getFees(KeySetResolver.of(Map.of())))
                .isInstanceOf(UnknownKeySetException.class);
    }

    private static KeySet keySet(String id, int inputFeePpk) {
        KeySet keySet = new KeySet();
        keySet.setId(id);
        keySet.setUnit("sat");
        keySet.setPartPerThousand(inputFeePpk);
        return keySet;
    }

    private static Proof<LiteralSecret> proof(String keySetId) {
        Proof<LiteralSecret> proof = new Proof<>();
        proof.setAmount(1);
        proof.setKeySetId(keySetId);
        return proof;
    }
}
