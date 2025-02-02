package xyz.tcheeric.cashu.common.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TokenV3Test {

    @Test
    public void addOneMintProof() {
        TokenV3<RandomStringSecret> tokenV3 = new TokenV3();
        TokenV3.MintProof<RandomStringSecret> mintProof = new TokenV3.MintProof();
        mintProof.setMint("mint");
        mintProof.addProof(new RSSProof());
        mintProof.addProof(new RSSProof());
        tokenV3.addMintProof(mintProof);

        TokenV3.MintProof<RandomStringSecret> mintProof2 = new TokenV3.MintProof();
        mintProof2.setMint("mint");
        mintProof2.addProof(new RSSProof());
        tokenV3.addMintProof(mintProof2);

        assertEquals(1, tokenV3.getMintProofs().size());
    }

    @Test
    public void addTwoMintProof() {
        TokenV3<RandomStringSecret> tokenV3 = new TokenV3();
        TokenV3.MintProof<RandomStringSecret> mintProof = new TokenV3.MintProof();
        mintProof.setMint("mint");
        mintProof.addProof(new RSSProof());
        mintProof.addProof(new RSSProof());
        tokenV3.addMintProof(mintProof);

        TokenV3.MintProof<RandomStringSecret> mintProof2 = new TokenV3.MintProof();
        mintProof2.setMint("mint2");
        mintProof2.addProof(new RSSProof());
        tokenV3.addMintProof(mintProof2);

        assertEquals(2, tokenV3.getMintProofs().size());
    }
}
