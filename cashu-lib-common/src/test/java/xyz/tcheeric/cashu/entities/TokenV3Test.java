package xyz.tcheeric.cashu.entities;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.RSSProof;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.TokenV3;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenV3Test {

    @Test
    /**
     * Ensures adding a proof for an existing mint merges proofs into a single entry.
     */
    void shouldMergeMintProofsWhenMintMatches() {
        // Arrange
        TokenV3<RandomStringSecret> token = new TokenV3<>();
        TokenV3.MintProof<RandomStringSecret> initialProof = new TokenV3.MintProof<>();
        initialProof.setMint("mint");
        initialProof.addProof(new RSSProof());
        initialProof.addProof(new RSSProof());
        token.addMintProof(initialProof);

        TokenV3.MintProof<RandomStringSecret> additionalProof = new TokenV3.MintProof<>();
        additionalProof.setMint("mint");
        additionalProof.addProof(new RSSProof());

        // Act
        token.addMintProof(additionalProof);

        // Assert
        assertEquals(1, token.getMintProofs().size());
    }

    @Test
    /**
     * Ensures proofs from different mints are tracked separately.
     */
    void shouldKeepMintProofsSeparateWhenMintDiffers() {
        // Arrange
        TokenV3<RandomStringSecret> token = new TokenV3<>();
        TokenV3.MintProof<RandomStringSecret> firstMintProof = new TokenV3.MintProof<>();
        firstMintProof.setMint("mint");
        firstMintProof.addProof(new RSSProof());
        firstMintProof.addProof(new RSSProof());
        token.addMintProof(firstMintProof);

        TokenV3.MintProof<RandomStringSecret> secondMintProof = new TokenV3.MintProof<>();
        secondMintProof.setMint("mint2");
        secondMintProof.addProof(new RSSProof());

        // Act
        token.addMintProof(secondMintProof);

        // Assert
        assertEquals(2, token.getMintProofs().size());
    }
}
