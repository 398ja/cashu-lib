package cashu.common.protocol;

public interface Ability<T> {

    T apply() throws CashuErrorException;
}