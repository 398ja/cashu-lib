package xyz.tcheeric.cashu.common.nut18;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.cashu.common.PrivateKey;

/**
 * @deprecated Use {@link VoucherSecret} instead.
 * This class is kept for backward compatibility with existing code.
 * VoucherSecret uses NUT-10 compliant tag-based storage for voucher metadata.
 */
@Slf4j
@Deprecated(since = "0.10.0", forRemoval = true)
public class VoucherWellKnownSecret extends VoucherSecret {

    public VoucherWellKnownSecret() {
        super();
    }

    /**
     * Creates a voucher secret with the given voucher data.
     * A unique nonce is automatically generated.
     *
     * @param voucherData serialized voucher bytes
     * @deprecated Use {@link VoucherSecret#VoucherSecret(java.util.UUID)} instead
     */
    @Deprecated
    public VoucherWellKnownSecret(@NonNull byte[] voucherData) {
        super();
        this.setData(voucherData);
        // Generate unique nonce for BDHKE
        //
        // asHex(), not toString(): PrivateKey redacts its toString() so a key
        // cannot reach a log by accident, and a nonce taken from it would be
        // the constant "PrivateKey(redacted)" for every proof ever minted.
        // Identical secrets hash to the same Y, and the mint keys spent proofs
        // on Y — so a 120 sat voucher split into 64+32+16+8 would have all four
        // outputs share one Y, and spending any one would mark the rest spent.
        try (PrivateKey randomKey = PrivateKey.generateRandom()) {
            this.setNonce(randomKey.asHex());
        }
    }

    /**
     * Creates a voucher secret with explicit nonce.
     *
     * @param voucherData serialized voucher bytes
     * @param nonce unique nonce for this proof
     * @deprecated Use {@link VoucherSecret} builder or setters instead
     */
    @Deprecated
    public VoucherWellKnownSecret(@NonNull byte[] voucherData, @NonNull String nonce) {
        super();
        this.setData(voucherData);
        this.setNonce(nonce);
    }

    /**
     * Gets the voucher data.
     *
     * @return voucher bytes
     * @deprecated Use {@link VoucherSecret#getData()} instead
     */
    @Deprecated
    public byte[] getVoucherData() {
        return getData();
    }
}
