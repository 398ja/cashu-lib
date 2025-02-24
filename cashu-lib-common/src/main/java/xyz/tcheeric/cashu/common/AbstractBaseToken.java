package xyz.tcheeric.cashu.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class AbstractBaseToken implements Token {

    private Version version;
    private String prefix;
    private boolean clickable;
}
