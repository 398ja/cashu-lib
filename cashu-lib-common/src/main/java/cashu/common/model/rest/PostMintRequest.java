package cashu.common.model.rest;

import cashu.common.model.BlindedMessage;
import cashu.common.model.Secret;
import cashu.util.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
public class PostMintRequest {

    @JsonProperty("quote")
    private String quoteId;

    @JsonProperty("outputs")
    private List<BlindedMessage> blindedMessages;

    @JsonIgnore
    private List<Secret> secrets;

    @JsonIgnore
    private List<byte[]> blindingFactors;

    public PostMintRequest() {
        this.blindedMessages = new ArrayList<>();
        this.secrets = new ArrayList<>();
        this.blindingFactors = new ArrayList<>();
    }

    @Override
    public String toString() {
        List<String> bfactors = new ArrayList<>();
        blindingFactors.stream().map(Utils::bytesToHexString).forEach(bfactors::add);
        return "PostMintRequest{" +
                "quoteId='" + quoteId + '\'' +
                ", blindedMessages=" + blindedMessages +
                ", secrets=" + secrets +
                ", blindingFactors=" + bfactors +
                '}';
    }

    public void addSecret(@NonNull Secret secret, int index) {
        this.secrets.add(index, secret);
    }

    public void addBlindingFactor(byte[] blindingFactor, int index) {
        this.blindingFactors.add(index, blindingFactor);
    }

    public void addBlindMessage(@NonNull BlindedMessage blindedMessage, int index) {
        this.blindedMessages.add(index, blindedMessage);
    }

    public Secret getSecret(int index) {
        return this.secrets.get(index);
    }

    public byte[] getBlindingFactor(int index) {
        return this.blindingFactors.get(index);
    }
}
