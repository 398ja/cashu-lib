package xyz.tcheeric.cashu.crypto;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.util.KeysUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DLEQUtils")
class DLEQUtilsTest {

    private static ECNamedCurveParameterSpec spec;
    private static ECPoint generator;

    @BeforeAll
    static void setup() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        generator = spec.getG();
    }

    @Nested
    @DisplayName("Proof generation")
    class ProofGenerationTests {

        /**
         * should generate challenge/response scalars for a valid blinded signature.
         */
        @Test
        void shouldGenerateValidProof() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint blindedMessage = decodePoint(blindResult[0]);

            ECPoint blindSignature = blindedMessage.multiply(privateKey).normalize();

            var proof = DLEQUtils.generateProof(privateKey, blindedMessage, blindSignature);

            assertThat(proof.e()).hasSize(64);
            assertThat(proof.s()).hasSize(64);
        }

        /**
         * should produce different proofs for the same inputs because of random nonce.
         */
        @Test
        void shouldGenerateDifferentProofs() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint blindedMessage = decodePoint(blindResult[0]);
            ECPoint blindSignature = blindedMessage.multiply(privateKey).normalize();

            var proof1 = DLEQUtils.generateProof(privateKey, blindedMessage, blindSignature);
            var proof2 = DLEQUtils.generateProof(privateKey, blindedMessage, blindSignature);

            assertThat(proof1.e()).isNotEqualTo(proof2.e());
            assertThat(proof1.s()).isNotEqualTo(proof2.s());
        }
    }

    @Nested
    @DisplayName("Alice verification (BlindSignature)")
    class AliceVerificationTests {

        /**
         * should accept valid proofs created by the mint.
         */
        @Test
        void shouldVerifyValidProof() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
            ECPoint mintPublicKey = generator.multiply(privateKey).normalize();

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint blindedMessage = decodePoint(blindResult[0]);
            ECPoint blindSignature = blindedMessage.multiply(privateKey).normalize();

            var proof = DLEQUtils.generateProof(privateKey, blindedMessage, blindSignature);

            boolean valid = DLEQUtils.verifyProof(proof.e(), proof.s(), blindedMessage, blindSignature, mintPublicKey);

            assertThat(valid).isTrue();
        }

        /**
         * should reject proofs where the challenge is tampered.
         */
        @Test
        void shouldRejectWrongChallenge() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
            ECPoint mintPublicKey = generator.multiply(privateKey).normalize();

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint blindedMessage = decodePoint(blindResult[0]);
            ECPoint blindSignature = blindedMessage.multiply(privateKey).normalize();

            var proof = DLEQUtils.generateProof(privateKey, blindedMessage, blindSignature);
            String wrongE = toggleFirstHexChar(proof.e());

            boolean valid = DLEQUtils.verifyProof(wrongE, proof.s(), blindedMessage, blindSignature, mintPublicKey);

            assertThat(valid).isFalse();
        }

        /**
         * should reject proofs verified against the wrong public key.
         */
        @Test
        void shouldRejectWrongPublicKey() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
            ECPoint mintPublicKey = generator.multiply(privateKey).normalize();

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint blindedMessage = decodePoint(blindResult[0]);
            ECPoint blindSignature = blindedMessage.multiply(privateKey).normalize();

            var proof = DLEQUtils.generateProof(privateKey, blindedMessage, blindSignature);
            byte[] wrongKeyBytes = KeysUtils.generatePrivateKey();
            ECPoint wrongPublicKey = generator.multiply(Utils.bigIntFromBytes(wrongKeyBytes)).normalize();

            boolean valid = DLEQUtils.verifyProof(proof.e(), proof.s(), blindedMessage, blindSignature, wrongPublicKey);

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("Carol verification (Proof with blinding factor)")
    class CarolVerificationTests {

        /**
         * should verify proofs when the correct blinding factor is supplied.
         */
        @Test
        void shouldVerifyWithBlindingFactor() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
            ECPoint mintPublicKey = generator.multiply(privateKey).normalize();

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint blindedMessage = decodePoint(blindResult[0]);
            BigInteger blindingFactor = Utils.bigIntFromBytes(blindResult[1]);

            ECPoint blindSignature = blindedMessage.multiply(privateKey).normalize();
            var proof = DLEQUtils.generateProof(privateKey, blindedMessage, blindSignature);

            ECPoint unblindedSignature = blindSignature.subtract(mintPublicKey.multiply(blindingFactor)).normalize();

            String blindingFactorHex = Utils.bytesToHexString(blindResult[1]);
            boolean valid = DLEQUtils.verifyProofWithBlindingFactor(
                    proof.e(),
                    proof.s(),
                    blindingFactorHex,
                    secret,
                    unblindedSignature,
                    mintPublicKey
            );

            assertThat(valid).isTrue();
        }

        /**
         * should reject proofs when using an incorrect blinding factor.
         */
        @Test
        void shouldRejectWrongBlindingFactor() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
            ECPoint mintPublicKey = generator.multiply(privateKey).normalize();

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint blindedMessage = decodePoint(blindResult[0]);
            BigInteger blindingFactor = Utils.bigIntFromBytes(blindResult[1]);

            ECPoint blindSignature = blindedMessage.multiply(privateKey).normalize();
            var proof = DLEQUtils.generateProof(privateKey, blindedMessage, blindSignature);

            ECPoint unblindedSignature = blindSignature.subtract(mintPublicKey.multiply(blindingFactor)).normalize();

            String wrongBlindingFactor = Utils.bytesToHexString(KeysUtils.generatePrivateKey());
            boolean valid = DLEQUtils.verifyProofWithBlindingFactor(
                    proof.e(),
                    proof.s(),
                    wrongBlindingFactor,
                    secret,
                    unblindedSignature,
                    mintPublicKey
            );

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("Hash function")
    class HashFunctionTests {

        /**
         * should return identical hash for identical inputs.
         */
        @Test
        void shouldProduceDeterministicHash() {
            ECPoint R1 = generator.multiply(BigInteger.valueOf(123)).normalize();
            ECPoint R2 = generator.multiply(BigInteger.valueOf(456)).normalize();
            ECPoint publicKey = generator.multiply(BigInteger.valueOf(789)).normalize();
            ECPoint blindedSignature = generator.multiply(BigInteger.valueOf(101112)).normalize();

            byte[] hash1 = DLEQUtils.dleqHash(R1, R2, publicKey, blindedSignature);
            byte[] hash2 = DLEQUtils.dleqHash(R1, R2, publicKey, blindedSignature);

            assertThat(hash1).isEqualTo(hash2);
        }

        /**
         * should produce 32-byte output for SHA-256.
         */
        @Test
        void shouldProduce32ByteHash() {
            ECPoint R1 = generator.multiply(BigInteger.ONE).normalize();
            ECPoint R2 = generator.multiply(BigInteger.valueOf(2)).normalize();
            ECPoint publicKey = generator.multiply(BigInteger.valueOf(3)).normalize();
            ECPoint blindedSignature = generator.multiply(BigInteger.valueOf(4)).normalize();

            byte[] hash = DLEQUtils.dleqHash(R1, R2, publicKey, blindedSignature);

            assertThat(hash).hasSize(32);
        }
    }

    private ECPoint decodePoint(byte[] raw64) {
        byte[] sec1 = raw64.length == 64 ? concat(new byte[]{0x04}, raw64) : raw64;
        return spec.getCurve().decodePoint(sec1);
    }

    private byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private String toggleFirstHexChar(String hex) {
        char first = hex.charAt(0);
        char toggled = first == '0' ? '1' : '0';
        return toggled + hex.substring(1);
    }
}
