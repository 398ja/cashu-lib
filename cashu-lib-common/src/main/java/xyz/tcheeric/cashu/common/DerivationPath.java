package xyz.tcheeric.cashu.common;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DerivationPath {

    private static final int CASHU_PURPOSE = 129372;
    private static final int CASHU_COIN_TYPE = 0;

    @EqualsAndHashCode.Include
    @Setter
    private KeysetId keysetId;

    @Setter
    private int counter;

    private final int suffix;

    @Override
    public String toString() {
        return String.format("m/%d'/%d'/%d'/%d'/%d", CASHU_PURPOSE, CASHU_COIN_TYPE, keysetId.toInt(), counter, suffix);
    }
}
