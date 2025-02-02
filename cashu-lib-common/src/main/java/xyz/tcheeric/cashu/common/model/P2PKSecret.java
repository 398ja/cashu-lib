package xyz.tcheeric.cashu.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.util.Collections;
import java.util.List;


@Log
public class P2PKSecret extends WellKnownSecret {

    public enum P2PKTag {
        sigflag,
        n_sigs,
        pubkeys,
        locktime,
        refund
    }

    public enum SignatureFlag {
        SIG_INPUTS,
        SIG_ALL
    }

    public P2PKSecret(@NonNull PublicKey data) {
        super(Kind.P2PK, data);
        this.addSigFlag(SignatureFlag.SIG_INPUTS);
    }

    public void addTag(@NonNull P2PKTag tag, @NonNull List<Object> values) {
        super.addTag(tag.name(), values);
    }

    public void addSigFlag(@NonNull SignatureFlag sigFlag) {
        super.addTag(P2PKTag.sigflag.name(), List.of(sigFlag));
    }

    public void addNSigs(@NonNull Integer nSigs) {
        super.addTag(P2PKTag.n_sigs.name(), List.of(nSigs));
    }

    public void addPubKeys(@NonNull List<PublicKey> pubKeys) {
        super.addTag(P2PKTag.pubkeys.name(), Collections.singletonList(pubKeys.stream().map(PublicKey::toString).toList()));
    }

    public void addLockTime(@NonNull Integer lockTime) {
        super.addTag(P2PKTag.locktime.name(), List.of(lockTime));
    }

    public void addRefund(@NonNull List<PublicKey> refund) {
        super.addTag(P2PKTag.refund.name(), Collections.singletonList(refund.stream().map(PublicKey::toString).toList()));
    }

    @SneakyThrows
    @Deprecated(forRemoval = true)
    public static P2PKSecret fromString(@NonNull String secret) {
        return new ObjectMapper().readValue(secret, P2PKSecret.class);
    }
}
