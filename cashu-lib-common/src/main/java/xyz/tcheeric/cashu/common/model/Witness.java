package xyz.tcheeric.cashu.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.crypto.Schnorr;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Witness {

    @JsonProperty
    private List<String> signatures;

    public Witness() {
        this.signatures = new ArrayList<>();
    }

    public void addSignature(@NonNull String signature) {
        this.signatures.add(signature);
    }

    public void removeSignature(@NonNull String signature) {
        this.signatures.remove(signature);
    }
}
