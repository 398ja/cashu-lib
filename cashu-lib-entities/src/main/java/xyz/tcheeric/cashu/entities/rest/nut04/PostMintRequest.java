package xyz.tcheeric.cashu.entities.rest.nut04;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
public class PostMintRequest<T extends Secret> {

    /**
     * Maximum number of outputs allowed per request to prevent DoS attacks.
     */
    public static final int MAX_OUTPUTS = 1000;

    @JsonProperty("quote")
    @NotBlank(message = "Quote ID is required")
    private String quoteId;

    @JsonProperty("outputs")
    @NotNull(message = "Outputs are required")
    @NotEmpty(message = "At least one output is required")
    @Size(max = MAX_OUTPUTS, message = "Maximum " + MAX_OUTPUTS + " outputs allowed")
    @Valid
    private List<BlindedMessage> blindedMessages;

    /**
     * NUT-20 — a BIP-340 signature over the quote id and the outputs, when the quote is locked.
     *
     * <p>Absent for an unlocked quote. The mint rejects a missing signature on a locked quote with
     * {@code 20009} and an invalid one with {@code 20008}.
     */
    @JsonProperty("signature")
    private String signature;

    @JsonIgnore
    private List<T> secrets;

    @JsonIgnore
    private List<byte[]> blindingFactors;

    /**
     * A mint request against an unlocked quote, carrying no NUT-20 signature.
     *
     * <p>The mint accepts this only when the quote itself is unlocked; a locked quote is refused
     * with {@code 20009}.
     */
    public PostMintRequest(String quoteId,
                           List<BlindedMessage> blindedMessages,
                           List<T> secrets,
                           List<byte[]> blindingFactors) {
        this(quoteId, blindedMessages, null, secrets, blindingFactors);
    }

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

    public void addSecret(@NonNull T secret, int index) {
        this.secrets.add(index, secret);
    }

    public void addBlindingFactor(byte[] blindingFactor, int index) {
        this.blindingFactors.add(index, blindingFactor);
    }

    public void addBlindMessage(@NonNull BlindedMessage blindedMessage, int index) {
        this.blindedMessages.add(index, blindedMessage);
    }

    public T getSecret(int index) {
        return this.secrets.get(index);
    }

    public byte[] getBlindingFactor(int index) {
        return this.blindingFactors.get(index);
    }
}
