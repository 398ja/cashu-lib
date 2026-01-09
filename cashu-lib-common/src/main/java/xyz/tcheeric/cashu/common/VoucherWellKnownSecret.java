package xyz.tcheeric.cashu.common;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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
        this.setNonce(PrivateKey.generateRandom().toString());
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
