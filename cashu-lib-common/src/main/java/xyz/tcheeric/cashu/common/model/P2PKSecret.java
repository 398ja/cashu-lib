package xyz.tcheeric.cashu.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
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

    public P2PKSecret(@NonNull byte[] data) {
        super(Kind.P2PK, data);
    }

    public void addTag(@NonNull P2PKTag tag, @NonNull List<Object> values) {
        super.addTag(tag.name(), values);
    }

    public void setSigFlag(@NonNull SignatureFlag sigFlag) {
        super.addTag(P2PKTag.sigflag.name(), List.of(sigFlag));
    }

    public void setNSigs(@NonNull Integer nSigs) {
        super.addTag(P2PKTag.n_sigs.name(), List.of(nSigs));
    }

    public void setPubKeys(@NonNull List<String> pubKeys) {
        super.addTag(P2PKTag.pubkeys.name(), new ArrayList<>(pubKeys.stream().toList()));
    }

    public void addPubKey(@NonNull String pubKey) {
        List<String> pubKeys = getPubKeys();
        pubKeys.add(pubKey);
        this.setPubKeys(pubKeys);
    }

    public void setLockTime(@NonNull Integer lockTime) {
        super.addTag(P2PKTag.locktime.name(), List.of(lockTime));
    }

    public void setRefund(@NonNull List<String> refund) {
        super.addTag(P2PKTag.refund.name(), new ArrayList<>(refund.stream().toList()));
    }

    public void addRefund(@NonNull PublicKey refund) {
        List<String> refunds = getRefund();
        refunds.add(refund.toString());
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
        return values != null ? (String) values.get(0) : null;
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
            return Integer.MAX_VALUE;
        }
        List<?> values = super.getTag(P2PKTag.locktime.name()).getValues();
        return values != null ? (int) values.get(0) : Integer.MAX_VALUE;
    }

    public List<String> getRefund() {
        Tag tag = super.getTag(P2PKTag.refund.name());
        if (tag == null) {
            return new ArrayList<>();
        }
        List<?> values = super.getTag(P2PKTag.refund.name()).getValues();
        return values != null ? (List<String>) values : new ArrayList<>();
    }

    @Override
    public String toString() {
        return Hex.decode(getData()).toString();
    }

    @SneakyThrows
    @Deprecated(forRemoval = true)
    public static P2PKSecret fromString(@NonNull String secret) {
        return new ObjectMapper().readValue(secret, P2PKSecret.class);
    }
}
