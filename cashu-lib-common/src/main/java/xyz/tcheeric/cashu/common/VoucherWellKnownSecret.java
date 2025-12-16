package xyz.tcheeric.cashu.common;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * NUT-10 well-known secret for voucher tokens.
 *
 * <p>Each proof in a voucher token uses this secret type with:
 * <ul>
 *   <li>kind: VOUCHER</li>
 *   <li>nonce: unique per proof (ensures unique Y values)</li>
 *   <li>data: serialized SignedVoucher bytes</li>
 * </ul>
 *
 * <p>The unique nonce per proof ensures each proof has a unique Y value
 * (Y = hash_to_curve(secret)), preventing "already redeemed" errors
 * when swapping multiple proofs from the same voucher.
 */
@Slf4j
public class VoucherWellKnownSecret extends WellKnownSecret {

    public VoucherWellKnownSecret() {
        super(Kind.VOUCHER);
    }

    /**
     * Creates a voucher secret with the given voucher data.
     * A unique nonce is automatically generated.
     *
     * @param voucherData serialized SignedVoucher bytes
     */
    public VoucherWellKnownSecret(@NonNull byte[] voucherData) {
        super(Kind.VOUCHER, voucherData);
    }

    /**
     * Creates a voucher secret with explicit nonce.
     * Use this when you need deterministic secrets (e.g., for testing).
     *
     * @param voucherData serialized SignedVoucher bytes
     * @param nonce unique nonce for this proof
     */
    public VoucherWellKnownSecret(@NonNull byte[] voucherData, @NonNull String nonce) {
        super(Kind.VOUCHER);
        this.setData(voucherData);
        this.setNonce(nonce);
    }

    /**
     * Gets the voucher data (serialized SignedVoucher).
     *
     * @return voucher bytes
     */
    public byte[] getVoucherData() {
        return getData();
    }
}
