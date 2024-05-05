package cashu.common.protocol;

import cashu.common.error.CashuException;

public interface Ability<T> {

    T apply() throws CashuException;
}
