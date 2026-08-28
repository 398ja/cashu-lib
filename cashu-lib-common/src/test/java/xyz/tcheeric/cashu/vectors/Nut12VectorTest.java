package xyz.tcheeric.cashu.vectors;

import lombok.SneakyThrows;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.nut12.DLEQProof;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.crypto.DLEQUtils;
import xyz.tcheeric.cashu.crypto.DleqChallengeHash;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NUT-12 vectors: the DLEQ challenge hash and DLEQ verification on signatures and proofs.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/tests/12-tests.md">NUT-12 test vectors</a>
 */
class Nut12VectorTest {

    private static final VectorDocument VECTORS = VectorDocument.load("12-tests.md");

    private static final int HASH_INPUT_BLOCK = 0;
    private static final int HASH_OUTPUT_BLOCK = 1;
    private static final int DETERMINISTIC_NONCE_INPUT_BLOCK = 2;
    private static final int DETERMINISTIC_NONCE_OUTPUT_BLOCK = 3;
    private static final int BLIND_SIGNATURE_KEYS_BLOCK = 4;
    private static final int PROOF_KEYS_BLOCK = 5;

    private static final int BLIND_SIGNATURE_JSON_BLOCK = 0;
    private static final int PROOF_JSON_BLOCK = 1;

    private static final SecP256K1Curve CURVE = new SecP256K1Curve();

    /**
     * Ensures hash_e over the published points reproduces the published digest.
     */
    @Test
    void shouldReproducePublishedDigestWhenHashingDleqPoints() {
        // Arrange
        Map<String, String> inputs = VECTORS.block("shell", HASH_INPUT_BLOCK).labelledValues();
        String expectedDigest = VECTORS.block("shell", HASH_OUTPUT_BLOCK)
                .labelledValues().get("hash(R1, R2, K, C_)");

        // Act
        byte[] digest = DleqChallengeHash.of(
                point(inputs.get("R1")),
                point(inputs.get("R2")),
                point(inputs.get("K")),
                point(inputs.get("C_")));

        // Assert
        assertThat(Utils.bytesToHexString(digest)).isEqualTo(expectedDigest);
    }

    /**
     * Ensures the published (e, s) pair verifies against the published blinded message and key.
     */
    @Test
    void shouldVerifyWhenDeterministicNonceVectorIsChecked() {
        // Arrange
        Map<String, String> inputs = VECTORS.block("shell", DETERMINISTIC_NONCE_INPUT_BLOCK).labelledValues();
        Map<String, String> proof = VECTORS.block("shell", DETERMINISTIC_NONCE_OUTPUT_BLOCK).labelledValues();

        // Act
        boolean valid = DLEQUtils.verifyProof(
                proof.get("e"),
                proof.get("s"),
                point(inputs.get("B_")),
                point(inputs.get("C_")),
                point(inputs.get("A")));

        // Assert
        assertThat(valid).isTrue();
    }

    /**
     * Ensures the DLEQ proof carried by the published BlindSignature verifies.
     */
    @Test
    void shouldVerifyWhenBlindSignatureCarriesValidDleqProof() {
        // Arrange
        Map<String, String> keys = VECTORS.block("shell", BLIND_SIGNATURE_KEYS_BLOCK).labelledValues();
        BlindSignature blindSignature = readJson(BLIND_SIGNATURE_JSON_BLOCK, BlindSignature.class);
        DLEQProof dleq = blindSignature.getDleq();

        // Act
        boolean valid = DLEQUtils.verifyProof(
                dleq.getE(),
                dleq.getS(),
                point(keys.get("B_")),
                point(blindSignature.getBlindedSignature().toString()),
                point(keys.get("A")));

        // Assert
        assertThat(valid).isTrue();
    }

    /**
     * Ensures the DLEQ proof carried by the published Proof verifies with its blinding factor.
     */
    @Test
    void shouldVerifyWhenProofCarriesValidDleqProofWithBlindingFactor() {
        // Arrange
        Map<String, String> keys = VECTORS.block("shell", PROOF_KEYS_BLOCK).labelledValues();
        Proof<Secret> proof = readProof();
        DLEQProof dleq = proof.getDleq();

        // Act
        boolean valid = DLEQUtils.verifyProofWithBlindingFactor(
                dleq.getE(),
                dleq.getS(),
                dleq.getR(),
                proof.getSecret().toString(),
                point(proof.getUnblindedSignature().toString()),
                point(keys.get("A")));

        // Assert
        assertThat(valid).isTrue();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private static Proof<Secret> readProof() {
        return JsonUtils.JSON_MAPPER.readValue(VECTORS.block("json", PROOF_JSON_BLOCK).text(), Proof.class);
    }

    @SneakyThrows
    private static <T> T readJson(int jsonBlockIndex, Class<T> type) {
        return JsonUtils.JSON_MAPPER.readValue(VECTORS.block("json", jsonBlockIndex).text(), type);
    }

    private static ECPoint point(String compressedHex) {
        return CURVE.decodePoint(Utils.hexStringToBytes(compressedHex)).normalize();
    }
}
