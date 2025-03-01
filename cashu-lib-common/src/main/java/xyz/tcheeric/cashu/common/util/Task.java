package xyz.tcheeric.cashu.common.util;

public interface Task<T> {
    T execute() throws CashuErrorException;
}
