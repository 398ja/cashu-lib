package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.util.ArrayList;
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

    public enum SignatureFlag { // IMPORTANT: Do not change the order!!!
        SIG_INPUTS,
        SIG_ALL
    }

    public P2PKSecret() {
        super(Kind.P2PK);
    }

    public P2PKSecret(@NonNull byte[] data) {
        super(Kind.P2PK, data);
        this.setNSigs(1);
        this.setSigFlag(SignatureFlag.SIG_INPUTS);
    }

    public P2PKSecret(@NonNull byte[] data, int nSigs, @NonNull SignatureFlag sigFlag) {
        super(Kind.P2PK, data);
        this.setNSigs(nSigs);
        this.setSigFlag(sigFlag);
    }

    public void addTag(@NonNull P2PKTag tag, @NonNull List<Object> values) {
        super.addTag(tag.name(), values);
    }

    public void setSigFlag(@NonNull SignatureFlag sigFlag) {
        super.setTag(P2PKTag.sigflag.name(), List.of(sigFlag));
    }

    public void setNSigs(@NonNull Integer nSigs) {
        super.setTag(P2PKTag.n_sigs.name(), List.of(nSigs));
    }

    public void setPubKeys(@NonNull List<String> pubKeys) {
        super.setTag(P2PKTag.pubkeys.name(), new ArrayList<>(pubKeys.stream().toList()));
    }

    public void addPubKey(@NonNull String pubKey) {
        List<String> pubKeys = getPubKeys();
        pubKeys.add(pubKey);
        this.setPubKeys(pubKeys);
    }

    public void setLockTime(@NonNull Integer lockTime) {
        super.setTag(P2PKTag.locktime.name(), List.of(lockTime));
    }

    public void setRefund(@NonNull List<String> refund) {
        super.setTag(P2PKTag.refund.name(), new ArrayList<>(refund.stream().toList()));
    }

    public void addRefund(@NonNull String refund) {
        List<String> refunds = getRefund();
        refunds.add(refund);
        this.setRefund(refunds);
    }

    public int getNSigs() {
        Tag tag = super.getTag(P2PKTag.n_sigs.name());
        if (tag == null) {
            return -1;
        }
        List<?> values = super.getTag(P2PKTag.n_sigs.name()).getValues();
        return values != null ? (int) values.get(0) : -1;
    }

    public String getSigFlag() {
        Tag tag = super.getTag(P2PKTag.sigflag.name());
        if (tag == null) {
            return null;
        }
        List<?> values = super.getTag(P2PKTag.sigflag.name()).getValues();
        return values != null ? ((SignatureFlag) values.get(0)).name() : null;
    }

    public List<String> getPubKeys() {
        Tag tag = super.getTag(P2PKTag.pubkeys.name());
        if (tag == null) {
            return new ArrayList<>();
        }
        List<?> values = tag.getValues();
        return values != null ? (List<String>) values : new ArrayList<>();
    }

    public int getLockTime() {
        Tag tag = super.getTag(P2PKTag.locktime.name());
        if (tag == null) {
            return 0;
        }
        List<?> values = super.getTag(P2PKTag.locktime.name()).getValues();
        return values != null ? (int) values.get(0) : 0;
    }

    public List<String> getRefund() {
        Tag tag = super.getTag(P2PKTag.refund.name());
        if (tag == null) {
            return new ArrayList<>();
        }
        List<?> values = super.getTag(P2PKTag.refund.name()).getValues();
        return values != null ? (List<String>) values : new ArrayList<>();
    }

    @SneakyThrows
    @Deprecated(forRemoval = true)
    public static P2PKSecret fromString(@NonNull String secret) {
        return new ObjectMapper().readValue(secret, P2PKSecret.class);
    }
}
